package no.nav.sykepenger.libs.testing.testdata

import java.time.LocalDate
import kotlin.random.Random

/** Lager et tilfeldig syntetisk identitetsnummer — som regel et fødselsnummer, av og til et D-nummer. */
fun lagIdentitetsnummer(
    fødselsdato: LocalDate = lagFødselsdato(18, 100),
    mann: Boolean = Math.random() <= 0.5,
): String =
    if (Math.random() < 0.9) {
        lagFødselsnummer(fødselsdato, mann)
    } else {
        lagDNummer(fødselsdato, mann)
    }

/**
 * Lager et tilfeldig syntetisk fødselsnummer med gyldige kontrollsifre.
 *
 * Syntetisk på samme måte som Skatteetaten gjør det, ved at det legges 80 til månedssifrene.
 * Ref. https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/test-for-konsumenter/
 */
fun lagFødselsnummer(
    fødselsdato: LocalDate = lagFødselsdato(18, 100),
    mann: Boolean = Math.random() <= 0.5,
): String {
    val fødselsnummerFødselsdato =
        buildString {
            append(fødselsdato.dayOfMonth.toString().padStart(2, '0'))
            append(fødselsdato.month.value + 80)
            append((fødselsdato.year % 100).toString().padStart(2, '0'))
        }
    val personnummer =
        lagPersonnummerForFødselsnummer(
            fødselsnummerFødselsdato = fødselsnummerFødselsdato,
            fødselsår = fødselsdato.year,
            mann = mann,
        )
    return fødselsnummerFødselsdato + personnummer
}

/**
 * Lager et tilfeldig syntetisk D-nummer med gyldige kontrollsifre.
 *
 * Syntetisk på samme måte som Skatteetaten gjør det, ved at det legges 80 til månedssifrene.
 * Ref. https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/test-for-konsumenter/
 */
fun lagDNummer(
    fødselsdato: LocalDate = lagFødselsdato(18, 100),
    mann: Boolean = Math.random() <= 0.5,
): String {
    val dNummerFødselsdato =
        buildString {
            append(fødselsdato.dayOfMonth + 40)
            append(fødselsdato.month.value + 80)
            append((fødselsdato.year % 100).toString().padStart(2, '0'))
        }
    val personnummer =
        lagPersonnummerForDNummer(
            dNummerFødselsdato = dNummerFødselsdato,
            mann = mann,
        )
    return dNummerFødselsdato + personnummer
}

private fun lagPersonnummerForFødselsnummer(
    fødselsnummerFødselsdato: String,
    fødselsår: Int,
    mann: Boolean,
): String {
    var individnummer: String
    var kontrollsiffer1: Int
    var kontrollsiffer2: Int
    do {
        do {
            // Tildel individnummer jf. https://lovdata.no/forskrift/2017-07-14-1201/§2-2-1
            individnummer = when (fødselsår) {
                in 1854..1899 -> Random.nextInt(from = 50, until = 75)
                in 1940..1999 if Math.random() < 0.5 -> Random.nextInt(from = 90, until = 100)
                in 1900..1999 -> Random.nextInt(from = 0, until = 50)
                in 2000..2039 -> Random.nextInt(from = 50, until = 100)
                else -> error("Det er ikke mulig å tildele personnummer til fødselsåret $fødselsår")
            }.toString().padStart(2, '0') + ((Random.nextInt(until = 5) * 2) + (if (mann) 1 else 0)).toString()
            kontrollsiffer1 = beregnKontrollsiffer1(fødselsnummerFødselsdato + individnummer)
        } while (kontrollsiffer1 > 9)
        kontrollsiffer2 = beregnKontrollsiffer2(fødselsnummerFødselsdato + individnummer + kontrollsiffer1)
    } while (kontrollsiffer2 > 9)

    return individnummer + kontrollsiffer1 + kontrollsiffer2
}

private fun lagPersonnummerForDNummer(
    dNummerFødselsdato: String,
    mann: Boolean,
): String {
    var individnummer: String
    var kontrollsiffer1: Int
    var kontrollsiffer2: Int
    do {
        do {
            // Individnummer i D-nummer tildeles fortløpende, ikke i serier
            individnummer = Random.nextInt(from = 0, until = 10).toString().padStart(2, '0') +
                ((Random.nextInt(until = 5) * 2) + (if (mann) 1 else 0)).toString()
            kontrollsiffer1 = beregnKontrollsiffer1(dNummerFødselsdato + individnummer)
        } while (kontrollsiffer1 > 9)
        kontrollsiffer2 = beregnKontrollsiffer2(dNummerFødselsdato + individnummer + kontrollsiffer1)
    } while (kontrollsiffer2 > 9)

    return individnummer + kontrollsiffer1 + kontrollsiffer2
}

internal fun beregnKontrollsiffer1(sifre: String): Int = beregnKontrollsiffer(sifre, listOf(3, 7, 6, 1, 8, 9, 4, 5, 2))

internal fun beregnKontrollsiffer2(sifre: String): Int = beregnKontrollsiffer(sifre, listOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2))

private fun beregnKontrollsiffer(
    sifre: String,
    vekting: List<Int>,
): Int = 11 - ((sifre).foldIndexed(0) { index, acc, char -> acc + vekting[index] * char.digitToInt() } % 11)
