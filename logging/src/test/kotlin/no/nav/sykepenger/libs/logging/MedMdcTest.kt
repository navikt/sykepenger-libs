package no.nav.sykepenger.libs.logging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.slf4j.MDC
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MedMdcTest {
    @BeforeTest
    @AfterTest
    fun ryddOppMdc() {
        MDC.clear()
    }

    @Test
    fun `medMdc setter verdiene mens blokka kjører`() {
        medMdc(
            MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId",
            MdcKey.MELDING_ID to "en meldingId",
        ) {
            assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
            assertEquals("en meldingId", MDC.get(MdcKey.MELDING_ID.value))
        }
    }

    @Test
    fun `medMdc returnerer verdien fra blokka`() {
        val resultat = medMdc(MdcKey.MELDING_ID to "en meldingId") { 42 }
        assertEquals(42, resultat)
    }

    @Test
    fun `medMdc fjerner verdiene etter at blokka er ferdig`() {
        medMdc(MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId") {}
        assertNull(MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
    }

    @Test
    fun `medMdc beholder verdier som allerede lå i MDC`() {
        MDC.put(MdcKey.IDENTITETSNUMMER.value, "et identitetsnummer")

        medMdc(MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId") {
            assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
            assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
        }

        assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
        assertNull(MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
    }

    @Test
    fun `medMdc overskriver en eksisterende verdi og gjenoppretter den etterpå`() {
        MDC.put(MdcKey.MELDING_ID.value, "opprinnelig meldingId")

        medMdc(MdcKey.MELDING_ID to "ny meldingId") {
            assertEquals("ny meldingId", MDC.get(MdcKey.MELDING_ID.value))
        }

        assertEquals("opprinnelig meldingId", MDC.get(MdcKey.MELDING_ID.value))
    }

    @Test
    fun `medMdc med null-verdi fjerner en eksisterende verdi og gjenoppretter den etterpå`() {
        MDC.put(MdcKey.MELDING_ID.value, "en meldingId")

        medMdc(MdcKey.MELDING_ID to null) {
            assertNull(MDC.get(MdcKey.MELDING_ID.value))
        }

        assertEquals("en meldingId", MDC.get(MdcKey.MELDING_ID.value))
    }

    @Test
    fun `medMdc med null-verdi for en nøkkel som ikke finnes er ufarlig`() {
        medMdc(MdcKey.MELDING_ID to null) {
            assertNull(MDC.get(MdcKey.MELDING_ID.value))
        }
        assertNull(MDC.get(MdcKey.MELDING_ID.value))
    }

    @Test
    fun `medMdc uten nøkler lar MDC være uendret`() {
        MDC.put(MdcKey.MELDING_ID.value, "en meldingId")

        medMdc {
            assertEquals("en meldingId", MDC.get(MdcKey.MELDING_ID.value))
        }

        assertEquals("en meldingId", MDC.get(MdcKey.MELDING_ID.value))
    }

    @Test
    fun `medMdc gjenoppretter MDC selv om blokka kaster`() {
        MDC.put(MdcKey.MELDING_ID.value, "opprinnelig meldingId")

        assertFailsWith<IllegalStateException> {
            medMdc(
                MdcKey.MELDING_ID to "ny meldingId",
                MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId",
            ) {
                error("noe gikk galt")
            }
        }

        assertEquals("opprinnelig meldingId", MDC.get(MdcKey.MELDING_ID.value))
        assertNull(MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
    }

    @Test
    fun `medMdc kan nøstes`() {
        medMdc(MdcKey.IDENTITETSNUMMER to "et identitetsnummer") {
            medMdc(MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId") {
                assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
                assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
            }
            assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
            assertNull(MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
        }
        assertNull(MDC.get(MdcKey.IDENTITETSNUMMER.value))
    }

    @Test
    fun `coMedMdc setter verdiene mens blokka kjører`() =
        runBlocking {
            coMedMdc(
                MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId",
                MdcKey.MELDING_ID to "en meldingId",
            ) {
                assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
                assertEquals("en meldingId", MDC.get(MdcKey.MELDING_ID.value))
            }
        }

    @Test
    fun `coMedMdc returnerer verdien fra blokka`() =
        runBlocking {
            val resultat = coMedMdc(MdcKey.MELDING_ID to "en meldingId") { 42 }
            assertEquals(42, resultat)
        }

    @Test
    fun `coMedMdc fjerner verdiene etter at blokka er ferdig`() =
        runBlocking {
            coMedMdc(MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId") {}
            assertNull(MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
        }

    @Test
    fun `coMedMdc arver verdier som allerede lå i MDC`() =
        runBlocking {
            MDC.put(MdcKey.IDENTITETSNUMMER.value, "et identitetsnummer")

            coMedMdc(MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId") {
                assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
                assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
            }

            assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
            assertNull(MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
        }

    @Test
    fun `coMedMdc overskriver en eksisterende verdi og gjenoppretter den etterpå`() =
        runBlocking {
            MDC.put(MdcKey.MELDING_ID.value, "opprinnelig meldingId")

            coMedMdc(MdcKey.MELDING_ID to "ny meldingId") {
                assertEquals("ny meldingId", MDC.get(MdcKey.MELDING_ID.value))
            }

            assertEquals("opprinnelig meldingId", MDC.get(MdcKey.MELDING_ID.value))
        }

    @Test
    fun `coMedMdc med null-verdi fjerner en eksisterende verdi og gjenoppretter den etterpå`() =
        runBlocking {
            MDC.put(MdcKey.MELDING_ID.value, "en meldingId")

            coMedMdc(MdcKey.MELDING_ID to null) {
                assertNull(MDC.get(MdcKey.MELDING_ID.value))
            }

            assertEquals("en meldingId", MDC.get(MdcKey.MELDING_ID.value))
        }

    @Test
    fun `coMedMdc gjenoppretter MDC selv om blokka kaster`() =
        runBlocking {
            MDC.put(MdcKey.MELDING_ID.value, "opprinnelig meldingId")

            assertFailsWith<IllegalStateException> {
                coMedMdc(
                    MdcKey.MELDING_ID to "ny meldingId",
                    MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId",
                ) {
                    error("noe gikk galt")
                }
            }

            assertEquals("opprinnelig meldingId", MDC.get(MdcKey.MELDING_ID.value))
            assertNull(MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
        }

    @Test
    fun `coMedMdc beholder verdiene på tvers av suspensjon og trådbytte`() =
        runBlocking {
            coMedMdc(MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId") {
                yield()
                assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))

                withContext(Dispatchers.Default) {
                    assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
                }

                withContext(Dispatchers.IO) {
                    assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
                }
            }
        }

    @Test
    fun `coMedMdc kan nøstes`() =
        runBlocking {
            coMedMdc(MdcKey.IDENTITETSNUMMER to "et identitetsnummer") {
                coMedMdc(MdcKey.VEDTAKSPERIODE_ID to "en vedtaksperiodeId") {
                    assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
                    assertEquals("en vedtaksperiodeId", MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
                }
                assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
                assertNull(MDC.get(MdcKey.VEDTAKSPERIODE_ID.value))
            }
            assertNull(MDC.get(MdcKey.IDENTITETSNUMMER.value))
        }

    @Test
    fun `medMdc inni coMedMdc utvider den arvede konteksten`() =
        runBlocking {
            coMedMdc(MdcKey.IDENTITETSNUMMER to "et identitetsnummer") {
                medMdc(MdcKey.OPPGAVE_ID to "en oppgaveId") {
                    assertEquals("et identitetsnummer", MDC.get(MdcKey.IDENTITETSNUMMER.value))
                    assertEquals("en oppgaveId", MDC.get(MdcKey.OPPGAVE_ID.value))
                }
                assertNull(MDC.get(MdcKey.OPPGAVE_ID.value))
            }
        }

    @Test
    fun `samtidige coMedMdc-blokker påvirker ikke hverandre`() =
        runBlocking {
            val resultater =
                coroutineScope {
                    (1..20)
                        .map { nummer ->
                            async(Dispatchers.Default) {
                                coMedMdc(MdcKey.MELDING_ID to "meldingId-$nummer") {
                                    yield()
                                    MDC.get(MdcKey.MELDING_ID.value)
                                }
                            }
                        }.awaitAll()
                }

            assertEquals((1..20).map { "meldingId-$it" }, resultater)
        }
}
