package no.nav.sykepenger.libs.ktor.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class TilgangsgrupperTilTilgangerTest {
    private val sut = TilgangsgrupperTilTilganger(tilgangLesGruppeIder = setOf("les"), tilgangSkrivGruppeIder = setOf("skriv"))

    @Test
    fun `skrivegruppe gir både Skriv og Les`() {
        assertEquals(setOf(Tilgang.Skriv, Tilgang.Les), sut.tilganger(setOf("skriv")))
    }

    @Test
    fun `lesegruppe gir bare Les`() {
        assertEquals(setOf(Tilgang.Les), sut.tilganger(setOf("les")))
    }

    @Test
    fun `ukjent gruppe gir ingen tilganger`() {
        assertEquals(emptySet(), sut.tilganger(setOf("ukjent")))
    }

    @Test
    fun `ingen grupper gir ingen tilganger`() {
        assertEquals(emptySet(), sut.tilganger(emptySet()))
    }

    @Test
    fun `fraEnv støtter flere kommaseparerte grupper`() {
        val fraEnv =
            TilgangsgrupperTilTilganger.fraEnv(
                mapOf(
                    "TILGANG_LES" to "les-1, les-2 ,les-3",
                    "TILGANG_SKRIV" to "skriv-1,",
                ),
            )
        assertEquals(setOf(Tilgang.Les), fraEnv.tilganger(setOf("les-2")))
        assertEquals(setOf(Tilgang.Les, Tilgang.Skriv), fraEnv.tilganger(setOf("skriv-1")))
        assertEquals(emptySet(), fraEnv.tilganger(setOf("")))
    }
}
