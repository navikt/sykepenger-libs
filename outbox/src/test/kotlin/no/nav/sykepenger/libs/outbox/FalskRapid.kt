package no.nav.sykepenger.libs.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class FalskRapid(
    private val feilerPåForsøk: Set<Int> = emptySet(),
    private val kasterVedFeil: Boolean = false,
    private val stopperVedFeil: Boolean = false,
) : RapidsConnection() {
    private val objectMapper = jacksonObjectMapper()
    private var forsøk = 0
    val sendteNr: MutableList<Int> = CopyOnWriteArrayList()

    @Volatile
    var stoppetidVedFeil: Duration? = null

    val senderNå = CountDownLatch(1)
    private var fullførSending: CountDownLatch? = null

    fun holdIgjenSending() {
        fullførSending = CountDownLatch(1)
    }

    fun fullførSending() {
        fullførSending?.countDown()
    }

    override fun publish(message: String) {
        publish(listOf(OutgoingMessage(message)))
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        publish(listOf(OutgoingMessage(message, key)))
    }

    override fun publish(messages: List<OutgoingMessage>): Pair<List<SentMessage>, List<FailedMessage>> {
        forsøk++
        senderNå.countDown()
        fullførSending?.let { check(it.await(5, TimeUnit.SECONDS)) { "Testen fullførte ikke sendingen" } }
        if (forsøk in feilerPåForsøk) {
            val feil = RuntimeException("Kafka er nede")
            if (kasterVedFeil) throw feil
            // Som KafkaRapid: en feilet sending stopper rapiden på samme tråd før publish returnerer.
            if (stopperVedFeil) stoppetidVedFeil = tidBrukt { stop() }
            return emptyList<SentMessage>() to messages.mapIndexed { index, message -> FailedMessage(index, message, feil) }
        }
        messages.forEach { sendteNr.add(objectMapper.readTree(it.body).path("nr").asInt()) }
        return emptyList<SentMessage>() to emptyList()
    }

    private fun tidBrukt(block: () -> Unit): Duration {
        val start = System.nanoTime()
        block()
        return Duration.ofNanos(System.nanoTime() - start)
    }

    override fun rapidName() = "falsk-rapid"

    override fun start() = Unit

    override fun stop() = notifyShutdownSignal()
}
