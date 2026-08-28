package no.nav.sykepenger.libs.testing.testdata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SaksbehandlerTest {
    @Test
    fun `NAV-ident starter med første bokstav i etternavnet og har seks sifre`() {
        repeat(100) {
            val navIdent = lagNavIdent(etternavn = "Undulat")
            assertEquals("U", navIdent.take(1))
            assertEquals(7, navIdent.length)
            assertTrue(navIdent.drop(1).all(Char::isDigit)) { "$navIdent har noe annet enn sifre etter bokstaven" }
        }
    }

    @Test
    fun `epost er fornavn punktum etternavn i små bokstaver`() {
        assertEquals("måteholden.undulat@nav.no", lagNavEpost(fornavn = "Måteholden", etternavn = "Undulat"))
    }

    @Test
    fun `NAV-ident og epost henger sammen med navnet`() {
        repeat(100) {
            val saksbehandler = TestSaksbehandler()
            assertEquals(
                "${saksbehandler.fornavn}.${saksbehandler.etternavn}@nav.no".lowercase(),
                saksbehandler.epost,
            )
            assertEquals(saksbehandler.etternavn.first().uppercase(), saksbehandler.navIdent.take(1))
        }
    }

    @Test
    fun `angitte verdier brukes som de er`() {
        val saksbehandler =
            TestSaksbehandler(
                fornavn = "Upresis",
                mellomnavn = "Robust",
                etternavn = "Genser",
                navIdent = "G123456",
                epost = "upresis.genser@nav.no",
            )
        assertEquals("Upresis", saksbehandler.fornavn)
        assertEquals("Robust", saksbehandler.mellomnavn)
        assertEquals("Genser", saksbehandler.etternavn)
        assertEquals("G123456", saksbehandler.navIdent)
        assertEquals("upresis.genser@nav.no", saksbehandler.epost)
    }

    @Test
    fun `NAV-ident og epost utledes av et angitt navn`() {
        val saksbehandler = TestSaksbehandler(fornavn = "Upresis", etternavn = "Genser")
        assertEquals("upresis.genser@nav.no", saksbehandler.epost)
        assertEquals("G", saksbehandler.navIdent.take(1))
    }
}
