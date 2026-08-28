package no.nav.sykepenger.libs.testing.testdata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdentitetsnummerTest {
    @Test
    fun `fødselsnummer har elleve sifre og gyldige kontrollsifre`() {
        repeat(1000) {
            assertGyldigIdentitetsnummer(lagFødselsnummer())
        }
    }

    @Test
    fun `d-nummer har elleve sifre og gyldige kontrollsifre`() {
        repeat(1000) {
            assertGyldigIdentitetsnummer(lagDNummer())
        }
    }

    @Test
    fun `identitetsnummer er gyldig uansett om det er fødselsnummer eller d-nummer`() {
        repeat(1000) {
            assertGyldigIdentitetsnummer(lagIdentitetsnummer())
        }
    }

    @Test
    fun `identitetsnummer blir både fødselsnummer og d-nummer`() {
        val identitetsnumre = (1..1000).map { lagIdentitetsnummer() }
        assertTrue(identitetsnumre.any { it.take(2).toInt() <= 31 }) { "det ble aldri generert et fødselsnummer" }
        assertTrue(identitetsnumre.any { it.take(2).toInt() > 40 }) { "det ble aldri generert et d-nummer" }
    }

    @Test
    fun `fødselsnummer er syntetisk og inneholder fødselsdatoen`() {
        val fødselsnummer = lagFødselsnummer(fødselsdato = 7 mar 1985, mann = true)
        assertEquals("07", fødselsnummer.substring(0, 2))
        assertEquals("83", fødselsnummer.substring(2, 4))
        assertEquals("85", fødselsnummer.substring(4, 6))
    }

    @Test
    fun `d-nummer har 40 lagt til på dagen og er syntetisk`() {
        val dNummer = lagDNummer(fødselsdato = 7 mar 1985, mann = false)
        assertEquals("47", dNummer.substring(0, 2))
        assertEquals("83", dNummer.substring(2, 4))
        assertEquals("85", dNummer.substring(4, 6))
    }

    @Test
    fun `kjønnssiffer er oddetall for menn og partall for kvinner`() {
        repeat(100) {
            assertTrue(lagFødselsnummer(mann = true)[8].digitToInt() % 2 == 1)
            assertTrue(lagFødselsnummer(mann = false)[8].digitToInt() % 2 == 0)
            assertTrue(lagDNummer(mann = true)[8].digitToInt() % 2 == 1)
            assertTrue(lagDNummer(mann = false)[8].digitToInt() % 2 == 0)
            assertTrue(lagIdentitetsnummer(mann = true)[8].digitToInt() % 2 == 1)
            assertTrue(lagIdentitetsnummer(mann = false)[8].digitToInt() % 2 == 0)
        }
    }

    @Test
    fun `nummer for personer født på 1900-tallet kan ha individnummer i alle tildelte serier`() {
        val individnumre =
            (1..1000).map {
                lagFødselsnummer(fødselsdato = 15 jun 1970, mann = true).substring(6, 9).toInt()
            }
        assertTrue(individnumre.any { it < 500 }) { "individnummerserien 000-499 ble aldri brukt" }
        assertTrue(individnumre.any { it >= 900 }) { "individnummerserien 900-999 ble aldri brukt" }
    }

    @Test
    fun `personer født på 2000-tallet får individnummer i riktig serie`() {
        repeat(1000) {
            val individnummer = lagFødselsnummer(fødselsdato = 15 jun 2005, mann = true).substring(6, 9).toInt()
            assertTrue(individnummer >= 500) { "$individnummer er utenfor serien 500-999" }
        }
    }

    @Test
    fun `fødselsnummer kan ikke lages for fødselsår utenfor de tildelte seriene`() {
        val fødselsdato = 1 jan 2040
        val feil = assertThrows<IllegalStateException> { lagFødselsnummer(fødselsdato = fødselsdato, mann = true) }
        assertTrue(feil.message!!.contains("2040"))
    }

    private fun assertGyldigIdentitetsnummer(identitetsnummer: String) {
        assertEquals(11, identitetsnummer.length) { "$identitetsnummer har feil lengde" }
        assertTrue(identitetsnummer.all(Char::isDigit)) { "$identitetsnummer inneholder noe annet enn sifre" }
        assertEquals(
            identitetsnummer[9].digitToInt(),
            beregnKontrollsiffer1(identitetsnummer.take(9)),
        ) { "$identitetsnummer har ugyldig kontrollsiffer 1" }
        assertEquals(
            identitetsnummer[10].digitToInt(),
            beregnKontrollsiffer2(identitetsnummer.take(10)),
        ) { "$identitetsnummer har ugyldig kontrollsiffer 2" }
        val måned = identitetsnummer.substring(2, 4).toInt()
        assertTrue(måned in 81..92) { "$identitetsnummer er ikke syntetisk" }
    }
}
