package no.nav.sykepenger.libs.logging

import org.junit.jupiter.api.parallel.Isolated
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.MarkerFactory
import tools.jackson.databind.JsonNode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ende-til-ende-tester for rutingen: hva havner faktisk i nav-logs og i team-logs, med det ekte logback-oppsettet?
 *
 * Testene installerer oppsettet i den globale [org.slf4j.ILoggerFactory], og må derfor kjøre for seg selv.
 */
@Isolated
class LoggrutingTest {
    @Test
    fun `loggefunksjonene sender meldingen til begge mål, men detaljene bare til team-logs`() {
        val captured = captureLogEvents { loggWarn("en melding", "detalj" to "verdi", "tom" to null) }

        assertContentEquals(listOf("en melding"), captured.navLogs.messages())
        assertContentEquals(listOf("en melding - detalj: \"verdi\" tom: null"), captured.teamLogs.messages())
    }

    @Test
    fun `loggefunksjonene sender stacktracen bare til team-logs`() {
        val captured = captureLogEvents { loggError("en melding", RuntimeException("noe gikk galt")) }

        assertEquals(1, captured.navLogs.size)
        assertFalse(captured.navLogs.single().has("stack_trace"))
        assertTrue(
            captured.teamLogs
                .single()["stack_trace"]
                .asString()
                .contains("noe gikk galt"),
        )
    }

    @Test
    fun `en NavngittLogger sender meldingen til begge mål, men detaljene bare til team-logs`() {
        val captured =
            captureLogEvents {
                navngittLogger("no.nav.helse.EnNavngittLogger").warn("en melding", "detalj" to "verdi", "tom" to null)
            }

        assertContentEquals(listOf("en melding"), captured.navLogs.messages())
        assertContentEquals(listOf("en melding - detalj: \"verdi\" tom: null"), captured.teamLogs.messages())
    }

    @Test
    fun `en NavngittLogger sender stacktracen bare til team-logs`() {
        val captured =
            captureLogEvents {
                navngittLogger("no.nav.helse.EnNavngittLogger").error("en melding", RuntimeException("noe gikk galt"))
            }

        assertEquals(1, captured.navLogs.size)
        assertFalse(captured.navLogs.single().has("stack_trace"))
        assertTrue(
            captured.teamLogs
                .single()["stack_trace"]
                .asString()
                .contains("noe gikk galt"),
        )
    }

    @Test
    fun `en NavngittLogger logger under navnet den ble opprettet med`() {
        val captured = captureLogEvents { navngittLogger("no.nav.helse.EnNavngittLogger").info("en melding") }

        assertEquals("no.nav.helse.EnNavngittLogger", captured.navLogs.single()["logger_name"].asString())
        assertEquals("no.nav.helse.EnNavngittLogger", captured.teamLogs.single()["logger_name"].asString())
    }

    @Test
    fun `en NavngittLogger som heter tjenestekall havner bare i team-logs`() {
        val captured = captureLogEvents { navngittLogger("tjenestekall").info("et tjenestekall", "detalj" to "verdi") }

        assertContentEquals(emptyList(), captured.navLogs.messages())
        assertContentEquals(listOf("et tjenestekall - detalj: \"verdi\""), captured.teamLogs.messages())
    }

    @Test
    fun `undernivåer av tjenestekall havner bare i team-logs også for en NavngittLogger`() {
        val captured = captureLogEvents { navngittLogger("tjenestekall.heihei").info("et tjenestekall") }

        assertContentEquals(emptyList(), captured.navLogs.messages())
        assertContentEquals(listOf("et tjenestekall"), captured.teamLogs.messages())
    }

    @Test
    fun `MDC-felter som kan inneholde persondata havner bare i team-logs også for en NavngittLogger`() {
        val captured =
            captureLogEvents {
                MDC.putCloseable("callId", "en-call-id").use {
                    medMdc(MdcKey.VEDTAKSPERIODE_ID to "en-vedtaksperiode-id") {
                        navngittLogger("no.nav.helse.EnNavngittLogger").info("en melding")
                    }
                }
            }

        val navLogs = captured.navLogs.single()
        assertEquals("en-call-id", navLogs["callId"].asString())
        assertFalse(navLogs.has("vedtaksperiodeId"))

        val teamLogs = captured.teamLogs.single()
        assertEquals("en-call-id", teamLogs["callId"].asString())
        assertEquals("en-vedtaksperiode-id", teamLogs["vedtaksperiodeId"].asString())
    }

    @Test
    fun `standardnivåene gjelder også for en NavngittLogger`() {
        val captured =
            captureLogEvents {
                navngittLogger("no.nav.helse.EnNavngittLogger").debug("under standardterskelen")
                navngittLogger("no.nav.helse.EnNavngittLogger").info("over standardterskelen")
                navngittLogger("tjenestekall").debug("under standardterskelen for tjenestekall")
                navngittLogger("tjenestekall").info("over standardterskelen for tjenestekall")
            }

        assertContentEquals(listOf("over standardterskelen"), captured.navLogs.messages())
        assertContentEquals(
            listOf("over standardterskelen", "over standardterskelen for tjenestekall"),
            captured.teamLogs.messages(),
        )
    }

    @Test
    fun `nivåene fra tjenestens egne properties gjelder også for en NavngittLogger`() {
        val captured =
            captureLogEvents("logback-base-med-overstyringer.xml") {
                navngittLogger("no.nav.helse.EnNavngittLogger").info("under terskelen for root")
                navngittLogger("no.nav.helse.EnNavngittLogger").error("over terskelen for root")
                navngittLogger("tjenestekall").trace("under terskelen for root, men ikke for tjenestekall")
            }

        assertContentEquals(listOf("over terskelen for root"), captured.navLogs.messages())
        assertContentEquals(
            listOf("over terskelen for root", "under terskelen for root, men ikke for tjenestekall"),
            captured.teamLogs.messages(),
        )
    }

    @Test
    fun `en vanlig slf4j-logger havner i begge mål`() {
        val captured =
            captureLogEvents { LoggerFactory.getLogger("no.nav.helse.EnTredjepart").info("fra et rammeverk") }

        assertContentEquals(listOf("fra et rammeverk"), captured.navLogs.messages())
        assertContentEquals(listOf("fra et rammeverk"), captured.teamLogs.messages())
    }

    @Test
    fun `en vanlig slf4j-logger får stacktracen bare med til team-logs`() {
        val captured =
            captureLogEvents {
                LoggerFactory
                    .getLogger("no.nav.helse.EnTredjepart")
                    .error("fra et rammeverk", RuntimeException("noe gikk galt"))
            }

        assertContentEquals(listOf("fra et rammeverk"), captured.navLogs.messages())
        assertFalse(captured.navLogs.single().has("stack_trace"))

        assertContentEquals(listOf("fra et rammeverk"), captured.teamLogs.messages())
        assertTrue(
            captured.teamLogs
                .single()["stack_trace"]
                .asString()
                .contains("noe gikk galt"),
        )
    }

    @Test
    fun `tjenestekall-loggeren havner bare i team-logs`() {
        val captured = captureLogEvents { LoggerFactory.getLogger("tjenestekall").info("et tjenestekall") }

        assertContentEquals(emptyList(), captured.navLogs.messages())
        assertContentEquals(listOf("et tjenestekall"), captured.teamLogs.messages())
    }

    @Test
    fun `undernivåer av tjenestekall-loggeren havner bare i team-logs`() {
        val captured = captureLogEvents { LoggerFactory.getLogger("tjenestekall.heihei").info("et tjenestekall") }

        assertContentEquals(emptyList(), captured.navLogs.messages())
        assertContentEquals(listOf("et tjenestekall"), captured.teamLogs.messages())
    }

    @Test
    fun `en logger som bare slutter på tjenestekall havner i begge mål`() {
        val captured = captureLogEvents { LoggerFactory.getLogger("no.nav.helse.tjenestekall").info("en melding") }

        assertContentEquals(listOf("en melding"), captured.navLogs.messages())
        assertContentEquals(listOf("en melding"), captured.teamLogs.messages())
    }

    @Test
    fun `en marker som ikke betyr noe for oss påvirker ikke rutingen`() {
        val marker = MarkerFactory.getMarker("EN_ANNEN_MARKØR")

        val captured =
            captureLogEvents { LoggerFactory.getLogger("no.nav.helse.EnTredjepart").info(marker, "en melding") }

        assertContentEquals(listOf("en melding"), captured.navLogs.messages())
        assertContentEquals(listOf("en melding"), captured.teamLogs.messages())
    }

    @Test
    fun `en sammensatt marker som refererer til NOT_NAV_LOGS holdes unna nav-logs`() {
        val marker = MarkerFactory.getDetachedMarker("SAMMENSATT").apply { add(NOT_NAV_LOGS_MARKER) }

        val captured =
            captureLogEvents { LoggerFactory.getLogger("no.nav.helse.EnTredjepart").info(marker, "en melding") }

        assertContentEquals(emptyList(), captured.navLogs.messages())
        assertContentEquals(listOf("en melding"), captured.teamLogs.messages())
    }

    @Test
    fun `en sammensatt marker som refererer til NOT_TEAM_LOGS holdes unna team-logs`() {
        val marker = MarkerFactory.getDetachedMarker("SAMMENSATT").apply { add(NOT_TEAM_LOGS_MARKER) }

        val captured =
            captureLogEvents { LoggerFactory.getLogger("no.nav.helse.EnTredjepart").info(marker, "en melding") }

        assertContentEquals(listOf("en melding"), captured.navLogs.messages())
        assertContentEquals(emptyList(), captured.teamLogs.messages())
    }

    @Test
    fun `MDC-felter som kan inneholde persondata havner bare i team-logs`() {
        val captured =
            captureLogEvents {
                MDC.putCloseable("callId", "en-call-id").use {
                    medMdc(MdcKey.VEDTAKSPERIODE_ID to "en-vedtaksperiode-id") { loggInfo("en melding") }
                }
            }

        val navLogs = captured.navLogs.single()
        assertEquals("en-call-id", navLogs["callId"].asString())
        assertFalse(navLogs.has("vedtaksperiodeId"))

        val teamLogs = captured.teamLogs.single()
        assertEquals("en-call-id", teamLogs["callId"].asString())
        assertEquals("en-vedtaksperiode-id", teamLogs["vedtaksperiodeId"].asString())
    }

    @Test
    fun `standardnivåene slipper INFO gjennom, men ikke DEBUG`() {
        val captured =
            captureLogEvents {
                loggDebug("under standardterskelen")
                loggInfo("over standardterskelen")
                LoggerFactory.getLogger("tjenestekall").debug("under standardterskelen for tjenestekall")
                LoggerFactory.getLogger("tjenestekall").info("over standardterskelen for tjenestekall")
            }

        assertContentEquals(listOf("over standardterskelen"), captured.navLogs.messages())
        assertContentEquals(
            listOf("over standardterskelen", "over standardterskelen for tjenestekall"),
            captured.teamLogs.messages(),
        )
    }

    @Test
    fun `nivåene fra tjenestens egne properties styrer hva som slipper gjennom`() {
        val captured =
            captureLogEvents("logback-base-med-overstyringer.xml") {
                loggInfo("under terskelen for root")
                loggError("over terskelen for root")
                LoggerFactory.getLogger("tjenestekall").trace("under terskelen for root, men ikke for tjenestekall")
            }

        assertContentEquals(listOf("over terskelen for root"), captured.navLogs.messages())
        assertContentEquals(
            listOf("over terskelen for root", "under terskelen for root, men ikke for tjenestekall"),
            captured.teamLogs.messages(),
        )
    }

    private fun List<JsonNode>.messages() = map { it["message"].asString() }
}
