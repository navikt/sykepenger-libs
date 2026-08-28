package no.nav.sykepenger.libs.logging

import org.slf4j.LoggerFactory
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
    val logger = LoggerFactory.getLogger(kClass.java)

    // Logg uten detaljer til STDOUT (fordi det ikke har marker)
    logger
        .atLevel(level)
        .setMessage(melding)
        .log()

    // Logg med detaljer til team logs (ved å sette marker)
    logger
        .atLevel(level)
        .addMarker(TEAM_LOGS_MARKER)
        .setMessage(melding.medTeamLogsDetaljer(teamLogsDetaljer))
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
