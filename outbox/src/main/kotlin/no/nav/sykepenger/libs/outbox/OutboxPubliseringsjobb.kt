package no.nav.sykepenger.libs.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.sykepenger.libs.leaderelection.LeaderElection
import no.nav.sykepenger.libs.logging.loggError
import no.nav.sykepenger.libs.logging.loggInfo
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OutboxPubliseringsjobb(
    private val dataSource: DataSource,
    private val rapidsConnection: RapidsConnection,
    private val leaderElection: LeaderElection,
    private val pollIntervall: Duration = 0.5.seconds,
) : RapidsConnection.StatusListener {
    init {
        require(pollIntervall.isPositive() && pollIntervall.isFinite()) { "Pollintervallet må være positivt og endelig" }
    }

    private var executor: ScheduledExecutorService? = null

    @Synchronized
    override fun onStartup(rapidsConnection: RapidsConnection) {
        if (executor != null) return
        loggInfo("Starter publisering fra outboxen")
        executor =
            Executors
                .newSingleThreadScheduledExecutor(
                    Thread
                        .ofPlatform()
                        .name("outbox-publisering")
                        .daemon()
                        .factory(),
                ).apply {
                    scheduleWithFixedDelay(
                        ::publiserUpubliserte,
                        0,
                        pollIntervall.inWholeNanoseconds,
                        TimeUnit.NANOSECONDS,
                    )
                }
    }

    @Synchronized
    override fun onShutdownSignal(rapidsConnection: RapidsConnection) {
        val executor = executor ?: return
        loggInfo("Stopper publisering fra outboxen")
        executor.shutdown()
        // Lar en påbegynt publisering bli ferdig, slik at meldingen ikke sendes uten å bli merket som publisert.
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow()
    }

    fun publiserUpubliserte() {
        try {
            while (leaderElection.isLeader()) {
                if (!publiserNeste()) return
            }
        } catch (e: Exception) {
            loggError("Feil under publisering fra outboxen. Prøver igjen ved neste poll", e)
        }
    }

    private fun publiserNeste(): Boolean =
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val melding = connection.hentNesteUpubliserte()
                if (melding != null) {
                    publiser(melding)
                    connection.markerSomPublisert(melding.id)
                }
                connection.commit()
                melding != null
            } catch (e: Exception) {
                connection.rollback()
                throw e
            }
        }

    private fun publiser(melding: UpublisertMelding) {
        val (_, feilede) =
            rapidsConnection.publish(listOf(OutgoingMessage(body = melding.innhold, key = melding.fødselsnummer)))
        feilede.firstOrNull()?.let { throw it.error }
    }

    private fun Connection.hentNesteUpubliserte(): UpublisertMelding? =
        prepareStatement(
            """
            SELECT id, fodselsnummer, melding
            FROM outbox
            WHERE publisert_tidspunkt IS NULL
            ORDER BY sekvensnummer
            LIMIT 1
            FOR UPDATE
            """.trimIndent(),
        ).use { statement ->
            statement.executeQuery().use { rs ->
                if (!rs.next()) return null
                UpublisertMelding(rs.getObject("id", UUID::class.java), rs.getString("fodselsnummer"), rs.getString("melding"))
            }
        }

    private fun Connection.markerSomPublisert(id: UUID) {
        prepareStatement("UPDATE outbox SET publisert_tidspunkt = now() WHERE id = ?").use { statement ->
            statement.setObject(1, id)
            statement.executeUpdate()
        }
    }

    private class UpublisertMelding(
        val id: UUID,
        val fødselsnummer: String,
        val innhold: String,
    )
}
