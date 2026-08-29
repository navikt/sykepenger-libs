package no.nav.sykepenger.libs.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun navngittLogger(loggerNavn: String) = NavngittLogger(loggerNavn)

class NavngittLogger(
    loggerNavn: String,
) {
    private val logger: Logger = LoggerFactory.getLogger(loggerNavn)

    fun error(
        melding: String,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.ERROR, melding, teamLogsDetaljer.toList())
    }

    fun error(
        melding: String,
        throwable: Throwable,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.ERROR, melding, teamLogsDetaljer.toList(), throwable)
    }

    fun warn(
        melding: String,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.WARN, melding, teamLogsDetaljer.toList())
    }

    fun warn(
        melding: String,
        throwable: Throwable,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.WARN, melding, teamLogsDetaljer.toList(), throwable)
    }

    fun info(
        melding: String,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.INFO, melding, teamLogsDetaljer.toList())
    }

    fun info(
        melding: String,
        throwable: Throwable,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.INFO, melding, teamLogsDetaljer.toList(), throwable)
    }

    fun debug(
        melding: String,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.DEBUG, melding, teamLogsDetaljer.toList())
    }

    fun debug(
        melding: String,
        throwable: Throwable,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.DEBUG, melding, teamLogsDetaljer.toList(), throwable)
    }

    fun trace(
        melding: String,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.TRACE, melding, teamLogsDetaljer.toList())
    }

    fun trace(
        melding: String,
        throwable: Throwable,
        vararg teamLogsDetaljer: Pair<String, String?>,
    ) {
        loggMedDetaljer(logger, Level.TRACE, melding, teamLogsDetaljer.toList(), throwable)
    }
}
