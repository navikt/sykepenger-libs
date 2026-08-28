package no.nav.sykepenger.libs.testing.testdata

private val fornavnListe =
    listOf(
        "Måteholden",
        "Dypsindig",
        "Ultrafiolett",
        "Urettferdig",
        "Berikende",
        "Upresis",
        "Stridlynt",
        "Rund",
        "Internasjonal",
    )

private val mellomnavnListe =
    listOf(
        "Lysende",
        "Spennende",
        "Tidløs",
        "Hjertelig",
        "Storslått",
        "Sjarmerende",
        "Uforutsigbar",
        "Behagelig",
        "Robust",
        "Sofistikert",
    )

private val etternavnListe =
    listOf(
        "Diode",
        "Flom",
        "Damesykkel",
        "Undulat",
        "Bakgrunn",
        "Genser",
        "Fornøyelse",
        "Campingvogn",
        "Bakkeklaring",
    )

/** Lager et tilfeldig fornavn. */
fun lagFornavn(): String = fornavnListe.random()

/** Lager et tilfeldig mellomnavn. */
fun lagMellomnavn(): String = mellomnavnListe.random()

/** Lager et tilfeldig mellomnavn, eller `null` — omtrent annenhver gang. */
fun lagMellomnavnOrNull(): String? = if (Math.random() < 0.5) lagMellomnavn() else null

/** Lager et tilfeldig etternavn. */
fun lagEtternavn(): String = etternavnListe.random()
