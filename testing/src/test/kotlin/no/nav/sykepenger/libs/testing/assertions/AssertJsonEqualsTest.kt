package no.nav.sykepenger.libs.testing.assertions

import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonMapperBuilder
import kotlin.test.assertFailsWith

class AssertJsonEqualsTest {
    @Test
    fun `lik json med ulik rekkefølge på feltene`() {
        assertJsonEquals(
            expectedJson = """{ "a": 1, "b": { "c": 2, "d": [1, 2] } }""",
            actualJson = """{ "b": { "d": [1, 2], "c": 2 }, "a": 1 }""",
        )
    }

    @Test
    fun `ulik rekkefølge i array er ikke likt`() {
        assertFailsWith<AssertionError> {
            assertJsonEquals(
                expectedJson = """{ "a": [1, 2] }""",
                actualJson = """{ "a": [2, 1] }""",
            )
        }
    }

    @Test
    fun `ulik json`() {
        assertFailsWith<AssertionError> {
            assertJsonEquals(
                expectedJson = """{ "a": 1 }""",
                actualJson = """{ "a": 2 }""",
            )
        }
    }

    @Test
    fun `sammenlikner mot jsonnode`() {
        val node = jacksonMapperBuilder().build().readTree("""{ "b": 2, "a": 1 }""")
        assertJsonEquals(
            expectedJson = """{ "a": 1, "b": 2 }""",
            actualJsonNode = node,
        )
    }

    @Test
    fun `sammenlikner jsonnode mot json`() {
        val node = jacksonMapperBuilder().build().readTree("""{ "b": 2, "a": 1 }""")
        assertJsonEquals(
            expectedJsonNode = node,
            actualJson = """{ "a": 1, "b": 2 }""",
        )
    }

    @Test
    fun `sammenlikner to jsonnoder`() {
        val objectMapper = jacksonMapperBuilder().build()
        assertJsonEquals(
            expectedJsonNode = objectMapper.readTree("""{ "a": 1, "b": { "c": 2 } }"""),
            actualJsonNode = objectMapper.readTree("""{ "b": { "c": 2 }, "a": 1 }"""),
        )
        assertFailsWith<AssertionError> {
            assertJsonEquals(
                expectedJsonNode = objectMapper.readTree("""{ "a": 1 }"""),
                actualJsonNode = objectMapper.readTree("""{ "a": 2 }"""),
            )
        }
    }

    @Test
    fun `sammenlikner json på rotnivå som ikke er objekter`() {
        assertJsonEquals(expectedJson = """[{ "a": 1 }]""", actualJson = """[{ "a": 1 }]""")
        assertJsonEquals(expectedJson = """"tekst"""", actualJson = """"tekst"""")
    }

    @Test
    fun `ser bort fra felter på rotnivå`() {
        assertJsonEquals(
            expectedJson = """{ "a": 1, "@opprettet": "2026-01-01T00:00:00", "system_read_count": 0 }""",
            actualJson = """{ "a": 1, "@opprettet": "2020-01-01T00:00:00", "system_participating_services": [] }""",
            bortsettFraStier = setOf("@opprettet", "system_read_count", "system_participating_services"),
        )
    }

    @Test
    fun `ser bort fra nøstede felter`() {
        assertJsonEquals(
            expectedJson = """{ "a": { "b": { "c": 1, "d": 2 } } }""",
            actualJson = """{ "a": { "b": { "c": 9, "d": 2 } } }""",
            bortsettFraStier = setOf("a.b.c"),
        )
    }

    @Test
    fun `sti fjerner bare på riktig nivå`() {
        assertFailsWith<AssertionError> {
            assertJsonEquals(
                expectedJson = """{ "c": 1, "a": { "b": { "c": 1 } } }""",
                actualJson = """{ "c": 2, "a": { "b": { "c": 9 } } }""",
                bortsettFraStier = setOf("a.b.c"),
            )
        }
    }

    @Test
    fun `sti følger gjennom arrays uten å telle indeks`() {
        assertJsonEquals(
            expectedJson = """{ "a": [{ "b": 1, "c": 1 }, { "b": 2, "c": 2 }] }""",
            actualJson = """{ "a": [{ "b": 9, "c": 1 }, { "b": 8, "c": 2 }] }""",
            bortsettFraStier = setOf("a.b"),
        )
    }

    @Test
    fun `sti følger gjennom nøstede arrays`() {
        assertJsonEquals(
            expectedJson = """{ "a": [[{ "b": 1 }], [{ "b": 2 }]] }""",
            actualJson = """{ "a": [[{ "b": 9 }], [{ "b": 8 }]] }""",
            bortsettFraStier = setOf("a.b"),
        )
    }

    @Test
    fun `flere stier med samme første ledd`() {
        assertJsonEquals(
            expectedJson = """{ "a": { "b": 1, "c": 1, "d": 1 } }""",
            actualJson = """{ "a": { "b": 9, "c": 8, "d": 1 } }""",
            bortsettFraStier = setOf("a.b", "a.c"),
        )
    }

    @Test
    fun `sti som ikke finnes er harmløs`() {
        assertJsonEquals(
            expectedJson = """{ "a": 1 }""",
            actualJson = """{ "a": 1 }""",
            bortsettFraStier = setOf("finnes.ikke", "heller.ikke.her"),
        )
    }

    @Test
    fun `sti gjennom en verdi som ikke er objekt er harmløs`() {
        assertJsonEquals(
            expectedJson = """{ "a": 1, "b": null }""",
            actualJson = """{ "a": 1, "b": null }""",
            bortsettFraStier = setOf("a.b", "b.c"),
        )
    }

    @Test
    fun `fjerner hele undertreet når stien peker på et objekt`() {
        assertJsonEquals(
            expectedJson = """{ "a": { "b": { "c": 1 } }, "d": 2 }""",
            actualJson = """{ "a": { "b": { "c": 9, "e": 9 } }, "d": 2 }""",
            bortsettFraStier = setOf("a.b"),
        )
    }
}
