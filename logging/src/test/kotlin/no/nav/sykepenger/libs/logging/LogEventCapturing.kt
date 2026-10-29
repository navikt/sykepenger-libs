package no.nav.sykepenger.libs.logging

import ch.qos.logback.classic.BasicConfigurator
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.status.Status.ERROR
import org.slf4j.LoggerFactory.getILoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.InetAddress
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CapturedLogEvents(
    val navLogs: List<JsonNode>,
    val teamLogs: List<JsonNode>,
)

internal fun captureLogEvents(
    logbackXmlResourceName: String = "logback-base-uten-overstyringer.xml",
    block: () -> Unit,
): CapturedLogEvents {
    val (capturedTeamLogs, capturedStdout) =
        captureWithMockTeamLogs { mockTeamLogsHostnameAndPort, awaitTeamLogsConnection ->
            withLogbackConfiguration(logbackXmlResourceName, mockTeamLogsHostnameAndPort) {
                // Team-logs-appenderen kobler til asynkront. Uten å vente kan konteksten stoppes før den har koblet til.
                awaitTeamLogsConnection()
                captureStdout(block)
            }
        }
    return CapturedLogEvents(
        navLogs = capturedStdout.linesToJsonNodes(),
        teamLogs = capturedTeamLogs.linesToJsonNodes(),
    )
}

private fun <T> captureWithMockTeamLogs(
    block: (mockTeamLogsHostnameAndPort: String, awaitTeamLogsConnection: () -> Unit) -> T,
): Pair<String, T> =
    ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { socket ->
        val capturedTeamLogsBytes = ByteArrayOutputStream()
        val connectionAccepted = CountDownLatch(1)
        val teamLogsMockThread =
            thread(name = "team-logs-mock") {
                socket.accept().use { forbindelse ->
                    connectionAccepted.countDown()
                    forbindelse.inputStream.copyTo(capturedTeamLogsBytes)
                }
            }
        val mockTeamLogsHostnameAndPort = "localhost:${socket.localPort}"
        val awaitTeamLogsConnection = {
            assertTrue(connectionAccepted.await(10, TimeUnit.SECONDS), "Team-logs-appenderen koblet aldri til")
        }

        val blockOutput = block(mockTeamLogsHostnameAndPort, awaitTeamLogsConnection)

        teamLogsMockThread.join(Duration.ofSeconds(10).toMillis())
        assertFalse(teamLogsMockThread.isAlive, "Forbindelsen mot team-logs ble aldri lukket")

        capturedTeamLogsBytes.toByteArray().toString(Charsets.UTF_8) to blockOutput
    }

private fun <T> withLogbackConfiguration(
    logbackXmlResourceName: String,
    teamLogsDestination: String,
    block: () -> T,
): T {
    val context = getILoggerFactory() as LoggerContext
    return try {
        context.reset()
        context.putProperty("TEAM_LOGS_DESTINATION", teamLogsDestination)

        JoranConfigurator()
            .apply { this.context = context }
            .doConfigure(checkNotNull(CapturedLogEvents::class.java.classLoader.getResource(logbackXmlResourceName)) { "Fant ikke $logbackXmlResourceName" })

        val konfigurasjonsfeil =
            context.statusManager.copyOfStatusList
                .filter { it.level == ERROR }
                .map { it.message }
        assertTrue(konfigurasjonsfeil.isEmpty(), "Konfigurasjonen ga feil: $konfigurasjonsfeil")

        block()
    } finally {
        // Stopper appenderne, slik at team-logs-appenderen tømmer køen sin og lukker forbindelsen
        context.stop()
        context.reset()
        BasicConfigurator().apply { this.context = context }.configure(context)
    }
}

private fun captureStdout(block: () -> Unit): String =
    ByteArrayOutputStream().use { byteArrayOutputStream ->
        PrintStream(byteArrayOutputStream, true, Charsets.UTF_8).use { printstream ->
            val realStdout = System.out
            System.setOut(printstream)
            try {
                block()
            } finally {
                printstream.flush()
                System.setOut(realStdout)
            }
        }
        byteArrayOutputStream.toByteArray().toString(Charsets.UTF_8)
    }

private val objectMapper = jacksonObjectMapper()

private fun String.linesToJsonNodes(): List<JsonNode> =
    lines()
        .filter { it.isNotBlank() }
        .map { objectMapper.readTree(it) }
