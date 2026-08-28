package no.nav.sykepenger.libs.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker
import org.slf4j.MarkerFactory

private const val TEAM_LOGS_MARKER_NAME = "TEAM_LOGS"
val TEAM_LOGS_MARKER: Marker = MarkerFactory.getMarker(TEAM_LOGS_MARKER_NAME)

/**
 * Slipper gjennom hendelser som skal til team-logs: enten fordi de er markert med [TEAM_LOGS_MARKER],
 * eller fordi de er logget av tjenestekall-loggeren.
 *
 * Tjenestekall-loggeren er med av hensyn til tjenester som allerede bruker den, og som derfor ikke
 * setter markøren. Nye tjenester bør bruke markøren i stedet.
 */
class TeamLogsFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent): FilterReply =
        if (event.harTeamLogsMarker() || event.erTjenestekallLogger()) {
            FilterReply.ACCEPT
        } else {
            FilterReply.DENY
        }

    private fun ILoggingEvent.erTjenestekallLogger(): Boolean = loggerName == "tjenestekall" || loggerName.startsWith("tjenestekall.")

    private fun ILoggingEvent.harTeamLogsMarker() = markerList?.any { it.contains(TEAM_LOGS_MARKER_NAME) } == true
}
