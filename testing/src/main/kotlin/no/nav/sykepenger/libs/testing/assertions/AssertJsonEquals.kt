package no.nav.sykepenger.libs.testing.assertions

import org.intellij.lang.annotations.Language
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

private val objectMapper = jacksonObjectMapper()

/**
 * Sjekker at to JSON-dokumenter er like, uavhengig av rekkefølgen på feltene.
 *
 * @param bortsettFraStier punktseparerte stier til felter som skal ignoreres, f.eks. `"vedtak.opprettet"`.
 * Dukker ned i arrays om den finner det.
 * F. eks. vil stien `a.b` fjerne `b` både i `{"a": {"b": 1}}` og i begge objektene i `{"a": [{"b": 1}, {"b": 2}]}`.
 */
fun assertJsonEquals(
    @Language("JSON") expectedJson: String,
    @Language("JSON") actualJson: String,
    bortsettFraStier: Set<String> = emptySet(),
) = assertJsonEquals(
    expectedJsonNode = objectMapper.readTree(expectedJson),
    actualJsonNode = objectMapper.readTree(actualJson),
    bortsettFraStier = bortsettFraStier,
)

/**
 * Sjekker at to JSON-dokumenter er like, uavhengig av rekkefølgen på feltene.
 *
 * @param bortsettFraStier punktseparerte stier til felter som skal ignoreres, f.eks. `"vedtak.opprettet"`.
 * Dukker ned i arrays om den finner det.
 * F. eks. vil stien `a.b` fjerne `b` både i `{"a": {"b": 1}}` og i begge objektene i `{"a": [{"b": 1}, {"b": 2}]}`.
 */
fun assertJsonEquals(
    @Language("JSON") expectedJson: String,
    actualJsonNode: JsonNode,
    bortsettFraStier: Set<String> = emptySet(),
) {
    assertJsonEquals(
        expectedJsonNode = objectMapper.readTree(expectedJson),
        actualJsonNode = actualJsonNode,
        bortsettFraStier = bortsettFraStier,
    )
}

/**
 * Sjekker at to JSON-dokumenter er like, uavhengig av rekkefølgen på feltene.
 *
 * @param bortsettFraStier punktseparerte stier til felter som skal ignoreres, f.eks. `"vedtak.opprettet"`.
 * Dukker ned i arrays om den finner det.
 * F. eks. vil stien `a.b` fjerne `b` både i `{"a": {"b": 1}}` og i begge objektene i `{"a": [{"b": 1}, {"b": 2}]}`.
 */
fun assertJsonEquals(
    expectedJsonNode: JsonNode,
    @Language("JSON") actualJson: String,
    bortsettFraStier: Set<String> = emptySet(),
) {
    assertJsonEquals(
        expectedJsonNode = expectedJsonNode,
        actualJsonNode = objectMapper.readTree(actualJson),
        bortsettFraStier = bortsettFraStier,
    )
}

/**
 * Sjekker at to JSON-dokumenter er like, uavhengig av rekkefølgen på feltene.
 *
 * @param bortsettFraStier punktseparerte stier til felter som skal ignoreres, f.eks. `"vedtak.opprettet"`.
 * Dukker ned i arrays om den finner det.
 * F. eks. vil stien `a.b` fjerne `b` både i `{"a": {"b": 1}}` og i begge objektene i `{"a": [{"b": 1}, {"b": 2}]}`.
 */
fun assertJsonEquals(
    expectedJsonNode: JsonNode,
    actualJsonNode: JsonNode,
    bortsettFraStier: Set<String> = emptySet(),
) {
    val stier = bortsettFraStier.map { it.split(".") }.toSet()
    val expected = expectedJsonNode.sortertKopiUten(stier)
    val actual = actualJsonNode.sortertKopiUten(stier)
    assertEquals(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected),
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual),
    )
}

/**
 * Lager en kopi der feltene i hvert objekt er sortert alfabetisk, og der feltene [stier] peker på er fjernet.
 * Hver sti er en liste med feltnavn som følges nedover. Arrays følges gjennom uten å konsumere et ledd, slik
 * at stien `a.b` fjerner `b` både i `{"a": {"b": 1}}` og i `{"a": [{"b": 1}, {"b": 2}]}`.
 */
private fun JsonNode.sortertKopiUten(stier: Set<List<String>>): JsonNode =
    when (this) {
        is ObjectNode -> {
            val fjernes = stier.mapNotNull { it.singleOrNull() }.toSet()
            val videre = stier.filter { it.size > 1 }.groupBy({ it.first() }, { it.drop(1) })
            objectMapper.createObjectNode().also { sortert ->
                properties()
                    .filterNot { (navn, _) -> navn in fjernes }
                    .sortedBy { (navn, _) -> navn }
                    .forEach { (navn, verdi) ->
                        sortert.set(navn, verdi.sortertKopiUten(videre[navn].orEmpty().toSet()))
                    }
            }
        }

        is ArrayNode ->
            objectMapper.createArrayNode().also { sortert ->
                forEach { sortert.add(it.sortertKopiUten(stier)) }
            }

        else -> deepCopy()
    }
