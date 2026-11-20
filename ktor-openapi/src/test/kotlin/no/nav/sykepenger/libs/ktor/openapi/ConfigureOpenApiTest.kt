package no.nav.sykepenger.libs.ktor.openapi

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ExperimentalKtorApi
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalKtorApi::class)
class ConfigureOpenApiTest {
    private data class Eksempel(
        val id: UUID,
        val beløp: BigDecimal,
        val valgfrittBeløp: BigDecimal?,
        val måned: YearMonth,
        val dato: LocalDate,
        val tidspunkt: Instant,
        val valgfrittTall: Int?,
        val valgfrittValg: Boolean?,
    )

    private fun hentSpec(): JsonNode {
        lateinit var spec: JsonNode
        testApplication {
            application {
                configureOpenApi(tittel = "testapp", versjon = "1")
                routing {
                    get("/api/eksempel") { call.respond(HttpStatusCode.NoContent) }.describe {
                        responses {
                            HttpStatusCode.OK {
                                schema = jsonSchema<Eksempel>()
                            }
                        }
                    }
                    get("/api/udokumentert") { call.respond(HttpStatusCode.NoContent) }
                    get("/isalive") { call.respond(HttpStatusCode.OK) }
                }
            }
            val response = client.get("/api/openapi.json")
            assertEquals(HttpStatusCode.OK, response.status)
            spec = jacksonObjectMapper().readTree(response.bodyAsText())
        }
        return spec
    }

    @Test
    fun `specen har tittel, versjon og bare rutene som er beskrevet`() {
        val spec = hentSpec()

        assertEquals("testapp", spec.at("/info/title").asString())
        assertEquals("1", spec.at("/info/version").asString())
        assertEquals(setOf("/api/eksempel"), spec["paths"].propertyNames().toSet())
    }

    @Test
    fun `typer som Jackson skriver som tall eller tekst, får skjema som tall eller tekst`() {
        val egenskaper = eksempelskjema(hentSpec())["properties"]

        assertEquals("""{"type":"string","format":"uuid"}""", egenskaper.get("id").toString())
        assertEquals("""{"type":"number"}""", egenskaper.get("beløp").toString())
        assertEquals("""{"type":["number","null"]}""", egenskaper.get("valgfrittBeløp").toString())
        assertEquals("""{"type":"string","pattern":"^\\d{4}-\\d{2}${'$'}"}""", egenskaper.get("måned").toString())
        assertEquals("""{"type":"string","format":"date"}""", egenskaper.get("dato").toString())
        assertEquals("""{"type":"string","format":"date-time"}""", egenskaper.get("tidspunkt").toString())
    }

    @Test
    fun `tall og sannhetsverdier som kan være null, kan være null i skjemaet`() {
        val egenskaper = eksempelskjema(hentSpec())["properties"]

        assertEquals("""{"type":["integer","null"]}""", egenskaper.get("valgfrittTall").toString())
        assertEquals("""{"type":["boolean","null"]}""", egenskaper.get("valgfrittValg").toString())
    }

    @Test
    fun `felter som ikke kan være null, er påkrevd`() {
        val påkrevd = eksempelskjema(hentSpec())["required"].values().map { it.asString() }

        assertTrue("beløp" in påkrevd)
        assertFalse("valgfrittBeløp" in påkrevd)
    }

    @Test
    fun `Swagger UI viser specen`() =
        testApplication {
            application { configureOpenApi(tittel = "testapp", versjon = "1") }

            val response = client.get("/api/swagger")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("swagger-ui"))
        }

    private fun eksempelskjema(spec: JsonNode): JsonNode {
        val svar = spec.at("/paths/~1api~1eksempel/get/responses/200/content/application~1json/schema")
        val referanse = svar["\$ref"]?.asString() ?: return svar
        return spec.at("/components/schemas").get(referanse.substringAfterLast("/"))
    }
}
