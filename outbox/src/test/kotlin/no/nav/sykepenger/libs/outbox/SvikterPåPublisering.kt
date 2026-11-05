package no.nav.sykepenger.libs.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import tools.jackson.module.kotlin.jacksonObjectMapper

internal class SvikterPåPublisering(
    private val feilerPå: Set<Int>,
    private val kaster: Boolean = false,
) : RapidsConnection() {
    private val objectMapper = jacksonObjectMapper()
    private var forsøk = 0
    val sendteNr = mutableListOf<Int>()

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
        if (forsøk in feilerPå) {
            val feil = RuntimeException("Kafka er nede")
            if (kaster) throw feil
            return emptyList<SentMessage>() to messages.mapIndexed { index, message -> FailedMessage(index, message, feil) }
        }
        messages.forEach { sendteNr.add(objectMapper.readTree(it.body).path("nr").asInt()) }
        return emptyList<SentMessage>() to emptyList()
    }

    override fun rapidName() = "svikter-rapid"

    override fun start() = Unit

    override fun stop() = Unit
}
