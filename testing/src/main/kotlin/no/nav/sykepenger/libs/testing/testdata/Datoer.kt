package no.nav.sykepenger.libs.testing.testdata

import java.time.LocalDate
import kotlin.random.Random

/** Datoliteral: `1 jan 2026` er 1. januar 2026. */
infix fun Int.jan(år: Int): LocalDate = LocalDate.of(år, 1, this)

/** Datoliteral: `1 feb 2026` er 1. februar 2026. */
infix fun Int.feb(år: Int): LocalDate = LocalDate.of(år, 2, this)

/** Datoliteral: `1 mar 2026` er 1. mars 2026. */
infix fun Int.mar(år: Int): LocalDate = LocalDate.of(år, 3, this)

/** Datoliteral: `1 apr 2026` er 1. april 2026. */
infix fun Int.apr(år: Int): LocalDate = LocalDate.of(år, 4, this)

/** Datoliteral: `1 mai 2026` er 1. mai 2026. */
infix fun Int.mai(år: Int): LocalDate = LocalDate.of(år, 5, this)

/** Datoliteral: `1 jun 2026` er 1. juni 2026. */
infix fun Int.jun(år: Int): LocalDate = LocalDate.of(år, 6, this)

/** Datoliteral: `1 jul 2026` er 1. juli 2026. */
infix fun Int.jul(år: Int): LocalDate = LocalDate.of(år, 7, this)

/** Datoliteral: `1 aug 2026` er 1. august 2026. */
infix fun Int.aug(år: Int): LocalDate = LocalDate.of(år, 8, this)

/** Datoliteral: `1 sep 2026` er 1. september 2026. */
infix fun Int.sep(år: Int): LocalDate = LocalDate.of(år, 9, this)

/** Datoliteral: `1 okt 2026` er 1. oktober 2026. */
infix fun Int.okt(år: Int): LocalDate = LocalDate.of(år, 10, this)

/** Datoliteral: `1 nov 2026` er 1. november 2026. */
infix fun Int.nov(år: Int): LocalDate = LocalDate.of(år, 11, this)

/** Datoliteral: `1 des 2026` er 1. desember 2026. */
infix fun Int.des(år: Int): LocalDate = LocalDate.of(år, 12, this)

/** Lager en tilfeldig fødselsdato for en person som er nøyaktig [alder] år gammel i dag. */
fun lagFødselsdato(alder: Long): LocalDate =
    lagFødselsdato(
        minimumAlder = alder,
        maksimumAlder = alder,
    )

/** Lager en tilfeldig fødselsdato for en person mellom [minimumAlder] og [maksimumAlder] år i dag. */
fun lagFødselsdato(
    minimumAlder: Long = 18,
    maksimumAlder: Long = 66,
): LocalDate {
    require(maksimumAlder >= minimumAlder) { "maksimumAlder ($maksimumAlder) kan ikke være lavere enn minimumAlder ($minimumAlder)" }
    val iDag = LocalDate.now()
    val senesteFødselsdato = iDag.minusYears(minimumAlder)
    val tidligsteFødselsdato = iDag.minusYears(maksimumAlder + 1).plusDays(1)
    val randomDayInEpoch = Random.nextLong(tidligsteFødselsdato.toEpochDay(), senesteFødselsdato.toEpochDay() + 1)
    return LocalDate.ofEpochDay(randomDayInEpoch)
}

/** Lager en tilfeldig dødsdato mellom [fødselsdato] og i dag. */
fun lagDødsdato(fødselsdato: LocalDate): LocalDate = LocalDate.ofEpochDay(Random.nextLong(fødselsdato.toEpochDay(), LocalDate.now().toEpochDay()))
