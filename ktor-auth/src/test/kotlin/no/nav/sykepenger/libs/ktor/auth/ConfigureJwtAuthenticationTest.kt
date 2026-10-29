package no.nav.sykepenger.libs.ktor.auth

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigureJwtAuthenticationTest {
    private val oauth = MockOAuth2Server().also { it.start() }
    private val claims =
        mapOf(
            "NAVident" to "Z999999",
            "oid" to "6c0b2a0e-6a5b-4a4b-9a3c-3d2f1e0d9c8b",
            "name" to "Saksbehandler Testesen",
            "groups" to listOf("les"),
        )

    @AfterAll
    fun stoppUtsteder() = oauth.shutdown()

    @Test
    fun `gyldig token gir SaksbehandlerPrincipal`() {
        val token = token()
        val response = kall(token)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Z999999|6c0b2a0e-6a5b-4a4b-9a3c-3d2f1e0d9c8b|Saksbehandler Testesen|[Les]|$token", response.body)
    }

    @Test
    fun `skrivegruppe gir både Skriv og Les`() {
        assertEquals("[Skriv, Les]", kall(token(claims + ("groups" to listOf("skriv")))).body.split('|')[3])
    }

    @Test
    fun `token uten groups-claim gir ingen tilganger`() {
        assertEquals("[]", kall(token(claims - "groups")).body.split('|')[3])
    }

    @Test
    fun `navn faller tilbake til preferred_username og deretter NAVident`() {
        assertEquals("a@nav.no", kall(token(claims - "name" + ("preferred_username" to "a@nav.no"))).body.split('|')[2])
        assertEquals("Z999999", kall(token(claims - "name")).body.split('|')[2])
    }

    @Test
    fun `feil audience gir 401`() = assertUautentisert(kall(token(audience = "annen-tjeneste")))

    @Test
    fun `feil issuer gir 401`() = assertUautentisert(kall(token(issuer = "annen-utsteder")))

    @Test
    fun `mangler NAVident gir 401`() = assertUautentisert(kall(token(claims - "NAVident")))

    @Test
    fun `blank NAVident gir 401`() = assertUautentisert(kall(token(claims + ("NAVident" to " "))))

    @Test
    fun `mangler oid gir 401`() = assertUautentisert(kall(token(claims - "oid")))

    @Test
    fun `groups-claim som ikke er en liste gir 401`() = assertUautentisert(kall(token(claims + ("groups" to "les"))))

    @Test
    fun `mangler token gir 401`() = assertUautentisert(kall(null))

    @Test
    fun `ugyldig token gir 401`() = assertUautentisert(kall("ugyldig"))

    private data class Svar(
        val status: HttpStatusCode,
        val body: String,
    )

    private fun assertUautentisert(svar: Svar) {
        assertEquals(HttpStatusCode.Unauthorized, svar.status)
        assertEquals("", svar.body)
    }

    private fun token(
        tokenClaims: Map<String, Any> = claims,
        audience: String = CLIENT_ID,
        issuer: String = ISSUER_ID,
    ) = oauth.issueToken(issuerId = issuer, audience = audience, claims = tokenClaims).serialize()

    private fun kall(token: String?): Svar {
        lateinit var svar: Svar
        testApplication {
            application {
                configureJwtAuthentication(
                    AzureAdConfig(CLIENT_ID, oauth.issuerUrl(ISSUER_ID).toString(), oauth.jwksUrl(ISSUER_ID).toString()),
                    TilgangsgrupperTilTilganger(tilgangLesGruppeIder = setOf("les"), tilgangSkrivGruppeIder = setOf("skriv")),
                )
                routing {
                    authenticate(AZURE_AD_AUTHENTICATION_NAME) {
                        get("/beskyttet") {
                            val principal = call.principal<SaksbehandlerPrincipal>()!!
                            with(principal) {
                                call.respondText(
                                    listOf(
                                        saksbehandler.navIdent.value,
                                        saksbehandler.oid.value,
                                        saksbehandler.navn,
                                        tilganger.toString(),
                                        accessToken.value,
                                    ).joinToString("|"),
                                )
                            }
                        }
                    }
                }
            }
            val response: HttpResponse = client.get("/beskyttet") { if (token != null) bearerAuth(token) }
            svar = Svar(response.status, response.bodyAsText())
        }
        return svar
    }

    private companion object {
        const val CLIENT_ID = "test-client-id"
        const val ISSUER_ID = "azuread"
    }
}
