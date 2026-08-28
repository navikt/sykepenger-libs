package no.nav.sykepenger.libs.testing.testdata

import kotlin.random.Random

/** Lager en tilfeldig NAV-ident, med forbokstaven fra [etternavn]. */
fun lagNavIdent(etternavn: String = lagEtternavn()): String = etternavn.first().uppercase() + Random.nextInt(from = 100_000, until = 1_000_000)

/** Lager en NAV-epostadresse utledet av [fornavn] og [etternavn]. */
fun lagNavEpost(
    fornavn: String = lagFornavn(),
    etternavn: String = lagEtternavn(),
): String = "$fornavn.$etternavn@nav.no".lowercase()

/** En saksbehandler med tilfeldig og sammenhengende data per default. Alle feltene kan overstyres enkeltvis. */
data class TestSaksbehandler(
    val fornavn: String = lagFornavn(),
    val mellomnavn: String? = lagMellomnavnOrNull(),
    val etternavn: String = lagEtternavn(),
    val navIdent: String = lagNavIdent(etternavn = etternavn),
    val epost: String = lagNavEpost(fornavn = fornavn, etternavn = etternavn),
)
