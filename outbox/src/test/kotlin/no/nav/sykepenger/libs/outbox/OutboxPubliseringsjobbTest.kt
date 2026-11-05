package no.nav.sykepenger.libs.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.sykepenger.libs.leaderelection.LeaderElection
import no.nav.sykepenger.libs.postgres.TestDatabase
import no.nav.sykepenger.libs.testing.testdata.lagIdentitetsnummer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Timeout(30)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OutboxPubliseringsjobbTest {
    private val testDatabase = TestDatabase.start().also { it.opprettOutboxFraReadme() }
    private val rapid = TestRapid()
    private val fødselsnummer = lagIdentitetsnummer()

    @BeforeEach
    fun tømOutbox() {
        testDatabase.tøm("outbox")
        rapid.reset()
    }

    @AfterAll
    fun stoppDatabasen() {
        testDatabase.stop()
    }

    @Test
    fun `publiserer melding fra appens transaksjon med fødselsnummer som nøkkel`() {
        skrivITransaksjon(melding(nr = 1))

        jobb().publiserUpubliserte()

        assertEquals(1, rapid.inspektør.size)
        assertEquals(
            1,
            rapid.inspektør
                .message(0)
                .path("nr")
                .asInt(),
        )
        assertEquals(fødselsnummer, rapid.inspektør.key(0))
    }

    @Test
    fun `publiserer ikke melding når appens transaksjon rulles tilbake`() {
        testDatabase.dataSource.connection.use { connection ->
            connection.autoCommit = false
            Outbox.leggTil(connection, fødselsnummer, melding(nr = 1))
            connection.rollback()
        }

        jobb().publiserUpubliserte()

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `publiserer i rekkefølgen meldingene ble skrevet`() {
        skrivITransaksjon(melding(nr = 1), melding(nr = 2))
        skrivITransaksjon(melding(nr = 3))

        jobb().publiserUpubliserte()

        assertEquals(listOf(1, 2, 3), publiserteNr())
    }

    @Test
    fun `publiserer hver melding bare én gang`() {
        skrivITransaksjon(melding(nr = 1))
        jobb().publiserUpubliserte()
        skrivITransaksjon(melding(nr = 2))

        jobb().publiserUpubliserte()

        assertEquals(listOf(1, 2), publiserteNr())
    }

    @Test
    fun `merker publiserte meldinger med tidspunktet de ble publisert`() {
        skrivITransaksjon(melding(nr = 1))
        val før = Instant.now()

        jobb().publiserUpubliserte()

        val publisert = publiseringstidspunkter().single()
        assertNotNull(publisert)
        assertTrue(publisert >= før.truncatedTo(ChronoUnit.MILLIS), "$publisert skal være etter $før")
    }

    @Test
    fun `lar meldingen ligge og prøver igjen ved neste poll når sendingen feiler`() {
        skrivITransaksjon(melding(nr = 1), melding(nr = 2), melding(nr = 3))
        val rapid = SvikterPåPublisering(feilerPå = setOf(2))
        val jobb = OutboxPubliseringsjobb(testDatabase.dataSource, rapid, LeaderElection { true })

        jobb.publiserUpubliserte()

        assertEquals(listOf(1), rapid.sendteNr)
        assertEquals(1, publiseringstidspunkter().count { it != null })

        jobb.publiserUpubliserte()

        assertEquals(listOf(1, 2, 3), rapid.sendteNr)
        assertTrue(publiseringstidspunkter().all { it != null })
    }

    @Test
    fun `lar meldingen ligge når rapiden kaster under sending`() {
        skrivITransaksjon(melding(nr = 1))
        val rapid = SvikterPåPublisering(feilerPå = setOf(1), kaster = true)
        val jobb = OutboxPubliseringsjobb(testDatabase.dataSource, rapid, LeaderElection { true })

        jobb.publiserUpubliserte()
        assertEquals(listOf(null), publiseringstidspunkter())

        jobb.publiserUpubliserte()
        assertEquals(listOf(1), rapid.sendteNr)
    }

    @Test
    fun `publiserer ikke når poden ikke er leader`() {
        skrivITransaksjon(melding(nr = 1))
        var erLeader = false
        val jobb = jobb(LeaderElection { erLeader })

        jobb.publiserUpubliserte()
        assertEquals(0, rapid.inspektør.size)

        erLeader = true
        jobb.publiserUpubliserte()
        assertEquals(listOf(1), publiserteNr())
    }

    @Test
    fun `slutter å publisere mellom meldinger når poden mister ledelsen`() {
        skrivITransaksjon(melding(nr = 1), melding(nr = 2))
        var ledersjekker = 0

        jobb(LeaderElection { ledersjekker++ == 0 }).publiserUpubliserte()

        assertEquals(listOf(1), publiserteNr())
        assertEquals(1, publiseringstidspunkter().count { it != null })
    }

    @Test
    fun `starter polling ved oppstart og stopper ved nedstengning`() {
        val jobb = OutboxPubliseringsjobb(testDatabase.dataSource, rapid, LeaderElection { true }, 10.milliseconds)

        jobb.onStartup(rapid)
        try {
            skrivITransaksjon(melding(nr = 1))
            ventTil { rapid.inspektør.size == 1 }
        } finally {
            jobb.onShutdownSignal(rapid)
        }

        skrivITransaksjon(melding(nr = 2))
        Thread.sleep(100)
        assertEquals(listOf(1), publiserteNr())
    }

    @Test
    fun `avviser pollintervall som ikke er positivt og endelig`() {
        for (intervall in listOf(Duration.ZERO, (-1).milliseconds, Duration.INFINITE)) {
            assertFailsWith<IllegalArgumentException> {
                OutboxPubliseringsjobb(testDatabase.dataSource, rapid, LeaderElection { true }, intervall)
            }
        }
    }

    @Test
    fun `sender ikke en melding som en annen pod holder på å publisere`() {
        skrivITransaksjon(melding(nr = 1))
        testDatabase.dataSource.connection.use { annenPod ->
            annenPod.autoCommit = false
            annenPod.createStatement().use {
                it.executeQuery("SELECT id FROM outbox WHERE publisert_tidspunkt IS NULL ORDER BY sekvensnummer LIMIT 1 FOR UPDATE").close()
            }

            val jobb = thread { jobb().publiserUpubliserte() }
            Thread.sleep(200)
            annenPod.createStatement().use { it.executeUpdate("UPDATE outbox SET publisert_tidspunkt = now()") }
            annenPod.commit()
            jobb.join(5_000)
        }

        assertEquals(0, rapid.inspektør.size)
    }

    private fun ventTil(betingelse: () -> Boolean) {
        val frist = Instant.now().plusSeconds(5)
        while (!betingelse() && Instant.now() < frist) Thread.sleep(10)
        assertTrue(betingelse(), "Ventet for lenge")
    }

    private fun publiserteNr() =
        (0 until rapid.inspektør.size).map {
            rapid.inspektør
                .message(it)
                .path("nr")
                .asInt()
        }

    private fun publiseringstidspunkter(): List<Instant?> =
        testDatabase.dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT publisert_tidspunkt FROM outbox ORDER BY sekvensnummer").use { rs ->
                    buildList { while (rs.next()) add(rs.getTimestamp(1)?.toInstant()) }
                }
            }
        }

    private fun jobb(leaderElection: LeaderElection = LeaderElection { true }) = OutboxPubliseringsjobb(testDatabase.dataSource, rapid, leaderElection)

    private fun melding(nr: Int) = """{"@event_name":"test_hendelse","nr":$nr}"""

    private fun skrivITransaksjon(vararg meldinger: String) {
        testDatabase.dataSource.connection.use { connection ->
            connection.autoCommit = false
            meldinger.forEach { Outbox.leggTil(connection, fødselsnummer, it) }
            connection.commit()
        }
    }

    private fun TestDatabase.opprettOutboxFraReadme() {
        val readme = File("README.md").readText()
        val sql =
            checkNotNull(Regex("```sql\\n(.*?)```", RegexOption.DOT_MATCHES_ALL).find(readme)) {
                "README.md mangler SQL for outbox-tabellen"
            }.groupValues[1]
        dataSource.connection.use { connection ->
            connection.createStatement().use { it.execute(sql) }
        }
    }
}
