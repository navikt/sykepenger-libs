package no.nav.sykepenger.libs.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.spi.FilterReply
import ch.qos.logback.core.status.Status
import net.logstash.logback.appender.LogstashTcpSocketAppender
import net.logstash.logback.encoder.LogstashEncoder
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LogbackBaseTest {
    private val teamLogsMarkør: Marker = MarkerFactory.getMarker("TEAM_LOGS")

    @Test
    fun `standardnivåer når tjenesten ikke overstyrer noe`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            assertEquals(Level.INFO, context.getLogger(ROOT_LOGGER_NAME).level)
            assertEquals(Level.INFO, context.getLogger("tjenestekall").level)
        }
    }

    @Test
    fun `tjenesten kan overstyre nivåene med properties satt før include`() {
        medKonfigurasjon("logback-base-med-overstyringer.xml") { context ->
            assertEquals(Level.ERROR, context.getLogger(ROOT_LOGGER_NAME).level)
            assertEquals(Level.TRACE, context.getLogger("tjenestekall").level)
        }
    }

    @Test
    fun `en vanlig logger uten markør går bare til applikasjonsloggen`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            assertContentEquals(listOf("STDOUT_JSON"), context.mottakereFor("no.nav.helse.EnTjeneste"))
        }
    }

    @Test
    fun `en vanlig logger med TEAM_LOGS-markør går bare til team-logs`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            assertContentEquals(
                listOf("team-logs"),
                context.mottakereFor("no.nav.helse.EnTjeneste", teamLogsMarkør),
            )
        }
    }

    @Test
    fun `tjenestekall-loggeren går til team-logs uten markør, av hensyn til tjenester som allerede bruker den`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            assertContentEquals(listOf("team-logs"), context.mottakereFor("tjenestekall"))
        }
    }

    @Test
    fun `tjenestekall-loggeren sender bare én kopi selv om markøren er satt`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            assertContentEquals(listOf("team-logs"), context.mottakereFor("tjenestekall", teamLogsMarkør))
        }
    }

    @Test
    fun `en logger som bare slutter på tjenestekall går ikke til team-logs uten markør`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            assertContentEquals(listOf("STDOUT_JSON"), context.mottakereFor("no.nav.helse.tjenestekall"))
        }
    }

    @Test
    fun `tjenestekall-loggeren arver ikke, så innholdet havner aldri i applikasjonsloggen`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            assertFalse(context.getLogger("tjenestekall").isAdditive)
        }
    }

    @Test
    fun `det er bare én forbindelse til team-logs`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            val teamLogsAppendere =
                context.loggerList
                    .flatMap { it.iteratorForAppenders().asSequence() }
                    .distinct()
                    .filterIsInstance<LogstashTcpSocketAppender>()
            assertContentEquals(listOf("team-logs"), teamLogsAppendere.map { it.name })
        }
    }

    @Test
    fun `konsollappenderen tar bare med MDC-felter uten personinformasjon`() {
        medKonfigurasjon("logback-base-uten-overstyringer.xml") { context ->
            val appender = context.getLogger(ROOT_LOGGER_NAME).getAppender("STDOUT_JSON")
            assertIs<ConsoleAppender<ILoggingEvent>>(appender)
            val encoder = assertIs<LogstashEncoder>(appender.encoder)
            assertContentEquals(
                listOf("callId", "trace_flags", "span_id", "trace_id"),
                encoder.includeMdcKeyNames,
            )
        }
    }

    private fun LoggerContext.teamLogsAppender(navn: String): LogstashTcpSocketAppender {
        val appender =
            loggerList
                .flatMap { it.iteratorForAppenders().asSequence() }
                .distinct()
                .singleOrNull { it.name == navn }
        return assertIs<LogstashTcpSocketAppender>(appender, "Fant ikke appenderen $navn")
    }

    /**
     * Navnene på appenderne som faktisk mottar en hendelse logget på INFO av [loggerNavn], etter at
     * arv (additivity) og appendernes filterkjeder er tatt hensyn til.
     */
    private fun LoggerContext.mottakereFor(
        loggerNavn: String,
        vararg markører: Marker,
    ): List<String> {
        val hendelse =
            LoggingEvent(
                LogbackBaseTest::class.java.name,
                getLogger(loggerNavn),
                Level.INFO,
                "en melding",
                null,
                null,
            ).also { hendelse -> markører.forEach(hendelse::addMarker) }

        return aktuelleAppendere(loggerNavn)
            .filter { it.slipperGjennom(hendelse) }
            .map { it.name }
    }

    private fun LoggerContext.aktuelleAppendere(loggerNavn: String): List<Appender<ILoggingEvent>> {
        val appendere = mutableListOf<Appender<ILoggingEvent>>()
        var navn: String? = loggerNavn
        while (navn != null) {
            val logger = getLogger(navn)
            appendere.addAll(logger.iteratorForAppenders().asSequence())
            if (!logger.isAdditive) break
            navn = if (navn == ROOT_LOGGER_NAME) null else navn.substringBeforeLast('.', ROOT_LOGGER_NAME)
        }
        return appendere
    }

    private fun Appender<ILoggingEvent>.slipperGjennom(hendelse: ILoggingEvent): Boolean {
        copyOfAttachedFiltersList.forEach { filter ->
            when (filter.decide(hendelse)) {
                FilterReply.DENY -> return false
                FilterReply.ACCEPT -> return true
                else -> Unit
            }
        }
        return true
    }

    private fun medKonfigurasjon(
        ressurs: String,
        assertions: (LoggerContext) -> Unit,
    ) {
        val context = LoggerContext()
        try {
            JoranConfigurator()
                .apply { this.context = context }
                .doConfigure(checkNotNull(javaClass.classLoader.getResource(ressurs)) { "Fant ikke $ressurs" })

            val feil =
                context.statusManager.copyOfStatusList
                    .filter { it.level == Status.ERROR }
                    .map { it.message }
            assertTrue(feil.isEmpty(), "Konfigurasjonen ga feil: $feil")

            assertions(context)
        } finally {
            context.stop()
        }
    }
}
