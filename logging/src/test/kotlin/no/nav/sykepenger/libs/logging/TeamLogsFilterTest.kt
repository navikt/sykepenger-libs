package no.nav.sykepenger.libs.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class TeamLogsFilterTest {
    private val context = LoggerContext()
    private val filter = TeamLogsFilter()

    @Test
    fun `hendelser markert med TEAM_LOGS slipper gjennom`() {
        assertEquals(FilterReply.ACCEPT, filter.decide(hendelse("no.nav.helse.EnTjeneste", TEAM_LOGS_MARKER)))
    }

    @Test
    fun `hendelser fra tjenestekall-loggeren slipper gjennom uten markør`() {
        assertEquals(FilterReply.ACCEPT, filter.decide(hendelse("tjenestekall")))
    }

    @Test
    fun `hendelser fra loggere som begynner på tjenestekall punktum slipper gjennom uten markør`() {
        assertEquals(FilterReply.ACCEPT, filter.decide(hendelse("tjenestekall.heihei")))
    }

    @Test
    fun `hendelser uten markør fra andre loggere stoppes`() {
        assertEquals(FilterReply.DENY, filter.decide(hendelse("no.nav.helse.EnTjeneste")))
    }

    @Test
    fun `loggere som bare slutter på tjenestekall stoppes`() {
        assertEquals(FilterReply.DENY, filter.decide(hendelse("no.nav.helse.tjenestekall")))
    }

    @Test
    fun `andre markører slipper ikke gjennom`() {
        val annenMarkør = MarkerFactory.getMarker("EN_ANNEN_MARKØR")
        assertEquals(FilterReply.DENY, filter.decide(hendelse("no.nav.helse.EnTjeneste", annenMarkør)))
    }

    @Test
    fun `en markør som refererer til TEAM_LOGS slipper gjennom`() {
        val sammensattMarkør = MarkerFactory.getDetachedMarker("SAMMENSATT").apply { add(TEAM_LOGS_MARKER) }
        assertEquals(FilterReply.ACCEPT, filter.decide(hendelse("no.nav.helse.EnTjeneste", sammensattMarkør)))
    }

    @Test
    fun `hendelser helt uten markørliste stoppes`() {
        assertEquals(FilterReply.DENY, filter.decide(hendelse("no.nav.helse.EnTjeneste")))
    }

    private fun hendelse(
        loggerNavn: String,
        vararg markører: Marker,
    ): ILoggingEvent =
        LoggingEvent(
            TeamLogsFilterTest::class.java.name,
            context.getLogger(loggerNavn),
            Level.INFO,
            "en melding",
            null,
            null,
        ).also { hendelse -> markører.forEach(hendelse::addMarker) }
}
