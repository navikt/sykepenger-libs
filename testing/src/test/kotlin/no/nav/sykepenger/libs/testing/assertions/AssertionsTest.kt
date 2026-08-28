package no.nav.sykepenger.libs.testing.assertions

import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class AssertionsTest {
    private val objectMapper = jacksonMapperBuilder().build()

    @Test
    fun `i fortiden`() {
        assertIFortiden(LocalDateTime.now().minusNanos(1))
        assertFailsWith<AssertionError> { assertIFortiden(LocalDateTime.now().plusMinutes(1)) }
    }

    @Test
    fun `mindre enn n sekunder siden`() {
        assertMindreEnnNSekunderSiden(sekunder = 10, actual = LocalDateTime.now().minusSeconds(5))
        assertFailsWith<AssertionError> {
            assertMindreEnnNSekunderSiden(sekunder = 10, actual = LocalDateTime.now().minusSeconds(20))
        }
    }

    @Test
    fun `avrunder til mikrosekunder`() {
        val basis = LocalDateTime.of(2026, 1, 1, 12, 0, 0)
        assertEqualsByMicrosecond(basis.withNano(1_000_500), basis.withNano(1_001_000))
        assertNotEqualsByMicrosecond(basis.withNano(1_000_499), basis.withNano(1_001_000))
    }

    @Test
    fun `avrunder instant til mikrosekunder`() {
        val basis = Instant.parse("2026-01-01T12:00:00Z")
        assertEqualsByMicrosecond(basis.plusNanos(1_000_500), basis.plusNanos(1_001_000))
        assertNotEqualsByMicrosecond(basis.plusNanos(1_000_499), basis.plusNanos(1_001_000))
    }

    @Test
    fun `null er likt null`() {
        assertEqualsByMicrosecond(null as LocalDateTime?, null)
        assertEqualsByMicrosecond(null as Instant?, null)
    }

    @Test
    fun `er tall`() {
        val node = objectMapper.readTree("""{ "tall": 1, "tekst": "1", "null": null }""")
        assertIsNumber(node["tall"])
        assertFailsWith<AssertionError> { assertIsNumber(node["tekst"]) }
        assertFailsWith<AssertionError> { assertIsNumber(node["null"]) }
        assertFailsWith<AssertionError> { assertIsNumber(node["finnesikke"]) }
    }

    @Test
    fun `etter tidspunkt`() {
        val basis = LocalDateTime.of(2026, 1, 1, 12, 0, 0)
        assertAfter(expectedAfter = basis, actual = basis.plusNanos(1))
        assertFailsWith<AssertionError> { assertAfter(expectedAfter = basis, actual = basis) }

        val instant = Instant.parse("2026-01-01T12:00:00Z")
        assertAfter(expectedAfter = instant, actual = instant.plusNanos(1))
        assertFailsWith<AssertionError> { assertAfter(expectedAfter = instant, actual = instant) }
    }

    @Test
    fun minst() {
        assertAtLeast(expectedMinimum = 2, actual = 2)
        assertAtLeast(expectedMinimum = 2L, actual = 3L)
        assertFailsWith<AssertionError> { assertAtLeast(expectedMinimum = 2, actual = 1) }
        assertFailsWith<AssertionError> { assertAtLeast(expectedMinimum = 2L, actual = 1L) }
    }
}
