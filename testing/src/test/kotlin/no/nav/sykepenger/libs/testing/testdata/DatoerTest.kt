package no.nav.sykepenger.libs.testing.testdata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Period

class DatoerTest {
    @Test
    fun `fødselsdato gir en alder innenfor det angitte spennet`() {
        repeat(1000) {
            val fødselsdato = lagFødselsdato(minimumAlder = 18, maksimumAlder = 100)
            val alder = Period.between(fødselsdato, LocalDate.now()).years
            assertTrue(alder in 18..100) { "$fødselsdato gir alderen $alder, som er utenfor 18..100" }
        }
    }

    @Test
    fun `fødselsdato for en gitt alder gir nøyaktig den alderen`() {
        (0L..100L).forEach { forventetAlder ->
            repeat(100) {
                val fødselsdato = lagFødselsdato(alder = forventetAlder)
                val alder = Period.between(fødselsdato, LocalDate.now()).years
                assertEquals(forventetAlder.toInt(), alder) { "$fødselsdato gir feil alder" }
            }
        }
    }

    @Test
    fun `fødselsdato kan bli både yngste og eldste dag i spennet`() {
        val fødselsdatoer = (1..20_000).map { lagFødselsdato(alder = 30) }
        assertEquals(LocalDate.now().minusYears(30), fødselsdatoer.max()) { "yngste mulige fødselsdato ble aldri generert" }
        assertEquals(
            LocalDate.now().minusYears(31).plusDays(1),
            fødselsdatoer.min(),
        ) { "eldste mulige fødselsdato ble aldri generert" }
    }

    @Test
    fun `maksimumAlder kan ikke være lavere enn minimumAlder`() {
        assertThrows<IllegalArgumentException> { lagFødselsdato(minimumAlder = 40, maksimumAlder = 39) }
    }

    @Test
    fun `dødsdato er mellom fødselsdato og i dag`() {
        repeat(1000) {
            val fødselsdato = lagFødselsdato(18, 100)
            val dødsdato = lagDødsdato(fødselsdato)
            assertTrue(dødsdato >= fødselsdato) { "$dødsdato er før $fødselsdato" }
            assertTrue(dødsdato <= LocalDate.now()) { "$dødsdato er i framtida" }
        }
    }

    @Test
    fun `datohjelpere lager riktig dato`() {
        assertEquals(LocalDate.of(2026, 1, 5), 5 jan 2026)
        assertEquals(LocalDate.of(2026, 6, 30), 30 jun 2026)
        assertEquals(LocalDate.of(2026, 12, 24), 24 des 2026)
    }
}
