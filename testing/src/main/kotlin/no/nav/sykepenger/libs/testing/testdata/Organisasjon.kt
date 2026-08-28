package no.nav.sykepenger.libs.testing.testdata

import kotlin.random.Random

private val organisasjonsnavnDel1 =
    listOf(
        "NEPE",
        "KLOVNE",
        "BOBLEBAD-",
        "DUSTE",
        "SKIHOPP",
        "SMÅBARN",
        "SPANIA",
    )

private val organisasjonsnavnDel2 =
    listOf(
        "AVDELINGEN",
        "SENTERET",
        "FORUM",
        "KLUBBEN",
        "SNEKKERIET",
    )

/** Lager et tilfeldig organisasjonsnummer. */
fun lagOrganisasjonsnummer(): String = Random.nextLong(from = 800_000_000, until = 999_999_999).toString()

/** Lager et tilfeldig organisasjonsnavn. */
fun lagOrganisasjonsnavn(): String = organisasjonsnavnDel1.random() + organisasjonsnavnDel2.random()

/** En organisasjon med tilfeldig og sammenhengende data per default. Alle feltene kan overstyres enkeltvis. */
data class TestOrganisasjon(
    val organisasjonsnummer: String = lagOrganisasjonsnummer(),
    val navn: String = lagOrganisasjonsnavn(),
)
