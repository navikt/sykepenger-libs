package no.nav.sykepenger.libs.testing.assertions

import tools.jackson.databind.JsonNode
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Sjekker at tidspunktet er før nå. */
fun assertIFortiden(actual: LocalDateTime) {
    val now = LocalDateTime.now()
    assertTrue(
        actual = actual.isBefore(now),
        message = "Forventet at tidspunktet var i fortiden (i forhold til nå: $now), men det var $actual",
    )
}

/** Sjekker at tidspunktet er høyst [sekunder] sekunder tilbake i tid. */
fun assertMindreEnnNSekunderSiden(
    sekunder: Int,
    actual: LocalDateTime,
) {
    val now = LocalDateTime.now()
    assertTrue(
        actual = actual.isAfter(now.minusSeconds(sekunder.toLong())),
        message = "Forventet at tidspunktet var innenfor $sekunder sekunder tilbake i tid (i forhold til nå: $now), men det var $actual",
    )
}

/** Sjekker at tidspunktene er like, avrundet til nærmeste mikrosekund. */
fun assertEqualsByMicrosecond(
    expected: LocalDateTime?,
    actual: LocalDateTime?,
) = assertEquals(expected?.roundToMicros(), actual?.roundToMicros())

/** Sjekker at tidspunktene er like, avrundet til nærmeste mikrosekund. */
fun assertEqualsByMicrosecond(
    expected: Instant?,
    actual: Instant?,
) = assertEquals(expected?.roundToMicros(), actual?.roundToMicros())

/** Sjekker at tidspunktene er ulike, avrundet til nærmeste mikrosekund. */
fun assertNotEqualsByMicrosecond(
    expected: LocalDateTime?,
    actual: LocalDateTime?,
) = assertNotEquals(expected?.roundToMicros(), actual?.roundToMicros())

/** Sjekker at tidspunktene er ulike, avrundet til nærmeste mikrosekund. */
fun assertNotEqualsByMicrosecond(
    expected: Instant?,
    actual: Instant?,
) = assertNotEquals(expected?.roundToMicros(), actual?.roundToMicros())

private fun LocalDateTime.roundToMicros(): LocalDateTime {
    val roundUp = (this.nano % 1000) >= 500
    return truncatedTo(ChronoUnit.MICROS).plus(if (roundUp) 1 else 0, ChronoUnit.MICROS)
}

private fun Instant.roundToMicros(): Instant {
    val roundUp = (this.nano % 1000) >= 500
    return truncatedTo(ChronoUnit.MICROS).plus(if (roundUp) 1 else 0, ChronoUnit.MICROS)
}

/** Sjekker at JSON-noden finnes og er et tall. */
fun assertIsNumber(actual: JsonNode?) {
    assertEquals(true, actual?.takeUnless { it.isNull }?.isNumber)
}

/** Sjekker at tidspunktet er etter [expectedAfter]. */
fun assertAfter(
    expectedAfter: Instant,
    actual: Instant,
) = assertTrue(actual.isAfter(expectedAfter), "Forventet tidspunkt etter $expectedAfter, men var $actual")

/** Sjekker at tidspunktet er etter [expectedAfter]. */
fun assertAfter(
    expectedAfter: LocalDateTime,
    actual: LocalDateTime,
) = assertTrue(actual.isAfter(expectedAfter), "Forventet tidspunkt etter $expectedAfter, men var $actual")

/** Sjekker at tallet er minst [expectedMinimum]. */
fun assertAtLeast(
    expectedMinimum: Long,
    actual: Long,
) = assertTrue(actual >= expectedMinimum, "Forventet minst $expectedMinimum, men var $actual")

/** Sjekker at tallet er minst [expectedMinimum]. */
fun assertAtLeast(
    expectedMinimum: Int,
    actual: Int,
) = assertTrue(actual >= expectedMinimum, "Forventet minst $expectedMinimum, men var $actual")
