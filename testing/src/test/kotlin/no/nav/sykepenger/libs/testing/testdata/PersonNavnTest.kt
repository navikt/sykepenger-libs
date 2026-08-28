package no.nav.sykepenger.libs.testing.testdata

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonNavnTest {
    @Test
    fun `navnegenerering gir varierte, ikke-tomme navn`() {
        assertTrue((1..100).map { lagFornavn() }.toSet().size > 1)
        assertTrue((1..100).map { lagMellomnavn() }.toSet().size > 1)
        assertTrue((1..100).map { lagEtternavn() }.toSet().size > 1)
        assertTrue(listOf(lagFornavn(), lagMellomnavn(), lagEtternavn()).none(String::isBlank))
    }

    @Test
    fun `mellomnavn genereres av og til`() {
        val mellomnavn = (1..100).map { lagMellomnavnOrNull() }
        assertTrue(mellomnavn.any { it == null }) { "det ble aldri generert null" }
        assertTrue(mellomnavn.any { it != null }) { "det ble aldri generert et mellomnavn" }
    }
}
