package no.nav.sykepenger.libs.testing.testdata

import java.time.LocalDate
import kotlin.random.Random

/** Lager en tilfeldig aktørId. */
fun lagAktørId() = Random.nextLong(from = 1_000_000_000_000, until = 1_000_099_999_999).toString()

/** En person med tilfeldig og sammenhengende data per default. Alle feltene kan overstyres enkeltvis. */
data class TestPerson(
    val fødselsdato: LocalDate = lagFødselsdato(),
    val mann: Boolean = Random.nextBoolean(),
    val identitetsnummer: String =
        lagIdentitetsnummer(
            fødselsdato = fødselsdato,
            mann = mann,
        ),
    val aktørId: String = lagAktørId(),
    val fornavn: String = lagFornavn(),
    val mellomnavn: String? = lagMellomnavnOrNull(),
    val etternavn: String = lagEtternavn(),
)
