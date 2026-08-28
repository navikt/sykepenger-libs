package no.nav.sykepenger.libs.testing.testdata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period

class PersonTest {
    @Test
    fun `aktørId har tretten sifre`() {
        repeat(100) {
            val aktørId = lagAktørId()
            assertEquals(13, aktørId.length)
            assertTrue(aktørId.all(Char::isDigit))
        }
    }

    @Test
    fun `identitetsnummeret stemmer med fødselsdatoen og kjønnet`() {
        repeat(1000) {
            val person = TestPerson()
            val identitetsnummer = person.identitetsnummer
            val dag = identitetsnummer.substring(0, 2).toInt().let { if (it > 40) it - 40 else it }
            val måned = identitetsnummer.substring(2, 4).toInt() - 80
            val år = identitetsnummer.substring(4, 6).toInt()
            assertEquals(person.fødselsdato.dayOfMonth, dag) { "$identitetsnummer stemmer ikke med ${person.fødselsdato}" }
            assertEquals(person.fødselsdato.monthValue, måned) { "$identitetsnummer stemmer ikke med ${person.fødselsdato}" }
            assertEquals(person.fødselsdato.year % 100, år) { "$identitetsnummer stemmer ikke med ${person.fødselsdato}" }
            assertEquals(person.mann, identitetsnummer[8].digitToInt() % 2 == 1) { "$identitetsnummer har feil kjønnssiffer" }
        }
    }

    @Test
    fun `fødselsdatoen gir en yrkesaktiv alder som standard`() {
        repeat(1000) {
            val person = TestPerson()
            val alder = Period.between(person.fødselsdato, LocalDate.now()).years
            assertTrue(alder in 18..66) { "${person.fødselsdato} gir alderen $alder, som er utenfor 18..66" }
        }
    }

    @Test
    fun `angitte verdier brukes som de er`() {
        val person =
            TestPerson(
                fødselsdato = 7 mar 1985,
                mann = true,
                identitetsnummer = "07838512345",
                aktørId = "1234567890123",
                fornavn = "Måteholden",
                mellomnavn = "Tidløs",
                etternavn = "Undulat",
            )
        assertEquals(7 mar 1985, person.fødselsdato)
        assertTrue(person.mann)
        assertEquals("07838512345", person.identitetsnummer)
        assertEquals("1234567890123", person.aktørId)
        assertEquals("Måteholden", person.fornavn)
        assertEquals("Tidløs", person.mellomnavn)
        assertEquals("Undulat", person.etternavn)
    }

    @Test
    fun `mellomnavn kan settes til null`() {
        assertNull(TestPerson(mellomnavn = null).mellomnavn)
    }

    @Test
    fun `to personer får ulike verdier`() {
        val identitetsnumre = (1..100).map { TestPerson().identitetsnummer }.toSet()
        assertTrue(identitetsnumre.size > 90) { "fikk bare ${identitetsnumre.size} unike identitetsnummer av 100" }
    }
}
