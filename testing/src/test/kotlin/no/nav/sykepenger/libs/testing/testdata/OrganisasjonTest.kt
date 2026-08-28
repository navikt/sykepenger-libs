package no.nav.sykepenger.libs.testing.testdata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrganisasjonTest {
    @Test
    fun `organisasjonsnummer har ni sifre`() {
        repeat(100) {
            val organisasjonsnummer = lagOrganisasjonsnummer()
            assertEquals(9, organisasjonsnummer.length)
            assertTrue(organisasjonsnummer.all(Char::isDigit))
        }
    }

    @Test
    fun `organisasjonsnavn er sammensatt og varierer`() {
        val navn = (1..100).map { lagOrganisasjonsnavn() }
        assertTrue(navn.none(String::isBlank))
        assertTrue(navn.toSet().size > 1) { "det ble bare generert ett organisasjonsnavn" }
    }

    @Test
    fun `organisasjon får generert navn og organisasjonsnummer`() {
        val organisasjon = TestOrganisasjon()
        assertTrue(organisasjon.navn.isNotBlank())
        assertEquals(9, organisasjon.organisasjonsnummer.length)
        assertTrue(organisasjon.organisasjonsnummer.all(Char::isDigit))
    }

    @Test
    fun `angitte verdier brukes som de er`() {
        val organisasjon = TestOrganisasjon(organisasjonsnummer = "987654321", navn = "NEPEFORUM")
        assertEquals("987654321", organisasjon.organisasjonsnummer)
        assertEquals("NEPEFORUM", organisasjon.navn)
    }
}
