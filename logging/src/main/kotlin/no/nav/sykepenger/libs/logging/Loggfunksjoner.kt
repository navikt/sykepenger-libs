package no.nav.sykepenger.libs.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import org.slf4j.event.Level
import kotlin.reflect.KClass

inline fun <reified T : Any> T.loggError(
    melding: String,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.ERROR, melding, teamLogsDetaljer.toList())
}

inline fun <reified T : Any> T.loggError(
    melding: String,
    throwable: Throwable,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.ERROR, melding, teamLogsDetaljer.toList(), throwable)
}

inline fun <reified T : Any> T.loggWarn(
    melding: String,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.WARN, melding, teamLogsDetaljer.toList())
}

inline fun <reified T : Any> T.loggWarn(
    melding: String,
    throwable: Throwable,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.WARN, melding, teamLogsDetaljer.toList(), throwable)
}

inline fun <reified T : Any> T.loggInfo(
    melding: String,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.INFO, melding, teamLogsDetaljer.toList())
}

inline fun <reified T : Any> T.loggInfo(
    melding: String,
    throwable: Throwable,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.INFO, melding, teamLogsDetaljer.toList(), throwable)
}

inline fun <reified T : Any> T.loggDebug(
    melding: String,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.DEBUG, melding, teamLogsDetaljer.toList())
}

inline fun <reified T : Any> T.loggDebug(
    melding: String,
    throwable: Throwable,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.DEBUG, melding, teamLogsDetaljer.toList(), throwable)
}

inline fun <reified T : Any> T.loggTrace(
    melding: String,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.TRACE, melding, teamLogsDetaljer.toList())
}

inline fun <reified T : Any> T.loggTrace(
    melding: String,
    throwable: Throwable,
    vararg teamLogsDetaljer: Pair<String, String?>,
) {
    loggMedDetaljer(T::class, Level.TRACE, melding, teamLogsDetaljer.toList(), throwable)
}

fun <T : Any> loggMedDetaljer(
    kClass: KClass<T>,
    level: Level,
    melding: String,
    teamLogsDetaljer: List<Pair<String, String?>>,
    throwable: Throwable? = null,
) {
    loggMedDetaljer(
        logger = LoggerFactory.getLogger(kClass.java),
        level = level,
        melding = melding,
        teamLogsDetaljer = teamLogsDetaljer,
        throwable = throwable
    )
}

internal val NOT_NAV_LOGS_MARKER: Marker = MarkerFactory.getMarker("NOT_NAV_LOGS")
internal val NOT_TEAM_LOGS_MARKER: Marker = MarkerFactory.getMarker("NOT_TEAM_LOGS")

internal fun loggMedDetaljer(
    logger: Logger,
    level: Level,
    melding: String,
    teamLogsDetaljer: List<Pair<String, String?>>,
    throwable: Throwable? = null
) {
    // Logg uten detaljer og stacktrace til nav-logs
    logger
        .atLevel(level)
        .setMessage(melding)
        .addMarker(NOT_TEAM_LOGS_MARKER)
        .log()

    // Logg med detaljer og stacktrace til Team Logs
    logger
        .atLevel(level)
        .setMessage(melding.medTeamLogsDetaljer(teamLogsDetaljer = teamLogsDetaljer))
        .addMarker(NOT_NAV_LOGS_MARKER)
        .also { if (throwable != null) it.setCause(throwable) }
        .log()
}

private fun String.medTeamLogsDetaljer(teamLogsDetaljer: List<Pair<String, String?>>): String =
    buildString {
        append(this@medTeamLogsDetaljer)
        if (teamLogsDetaljer.isNotEmpty()) {
            append(" -")
            teamLogsDetaljer.forEach { (name, value) ->
                append(" ")
                append(name)
                append(": ")
                append(value?.let { "\"$it\"" } ?: "null")
            }
        }
    }
