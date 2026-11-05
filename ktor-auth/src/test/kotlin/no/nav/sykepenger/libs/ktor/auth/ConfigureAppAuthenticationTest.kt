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
class ConfigureAppAuthenticationTest {
    private val oauth = MockOAuth2Server().also { it.start() }
    private val appClaims = mapOf("idtyp" to "app", "azp" to "sp-forsikring-klient-id", "azp_name" to "dev-gcp:tbd:sp-forsikring")
    private val saksbehandlerClaims =
        mapOf("NAVident" to "Z999999", "oid" to "6c0b2a0e-6a5b-4a4b-9a3c-3d2f1e0d9c8b", "azp" to "speil-klient-id", "groups" to listOf("les"))

    @AfterAll
    fun stoppUtsteder() = oauth.shutdown()

    @Test
    fun `app-token gir AppPrincipal med klient-ID`() {
        val response = kall("/app", token(appClaims))
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("sp-forsikring-klient-id", response.body)
    }

    @Test
    fun `saksbehandlertoken gir 401 på app-endepunkt`() = assertUautentisert(kall("/app", token(saksbehandlerClaims)))

    @Test
    fun `app-token gir 401 på saksbehandlerendepunkt`() = assertUautentisert(kall("/saksbehandler", token(appClaims)))

    @Test
    fun `saksbehandlertoken virker fortsatt på saksbehandlerendepunkt`() {
        assertEquals(HttpStatusCode.OK, kall("/saksbehandler", token(saksbehandlerClaims)).status)
    }

    @Test
    fun `mangler idtyp gir 401`() = assertUautentisert(kall("/app", token(appClaims - "idtyp")))

    @Test
    fun `idtyp som ikke er app gir 401`() = assertUautentisert(kall("/app", token(appClaims + ("idtyp" to "user"))))

    @Test
    fun `mangler azp gir 401`() = assertUautentisert(kall("/app", token(appClaims - "azp")))

    @Test
    fun `blank azp gir 401`() = assertUautentisert(kall("/app", token(appClaims + ("azp" to " "))))

    @Test
    fun `feil audience gir 401`() = assertUautentisert(kall("/app", token(appClaims, audience = "annen-tjeneste")))

    @Test
    fun `feil issuer gir 401`() = assertUautentisert(kall("/app", token(appClaims, issuer = "annen-utsteder")))

    @Test
    fun `mangler token gir 401`() = assertUautentisert(kall("/app", null))

    @Test
    fun `ugyldig token gir 401`() = assertUautentisert(kall("/app", "ugyldig"))

    private data class Svar(
        val status: HttpStatusCode,
        val body: String,
    )

    private fun assertUautentisert(svar: Svar) {
        assertEquals(HttpStatusCode.Unauthorized, svar.status)
        assertEquals("", svar.body)
    }

    private fun token(
        tokenClaims: Map<String, Any>,
        audience: String = CLIENT_ID,
        issuer: String = ISSUER_ID,
    ) = oauth.anyToken(oauth.issuerUrl(issuer), tokenClaims + ("aud" to audience)).serialize()

    private fun kall(
        path: String,
        token: String?,
    ): Svar {
        lateinit var svar: Svar
        testApplication {
            application {
                val azureAdConfig = AzureAdConfig(CLIENT_ID, oauth.issuerUrl(ISSUER_ID).toString(), oauth.jwksUrl(ISSUER_ID).toString())
                configureAppAuthentication(azureAdConfig)
                configureJwtAuthentication(
                    azureAdConfig,
                    TilgangsgrupperTilTilganger(tilgangLesGruppeIder = setOf("les"), tilgangSkrivGruppeIder = emptySet()),
                )
                routing {
                    authenticate(AZURE_AD_APP_AUTHENTICATION_NAME) {
                        get("/app") {
                            val principal = call.principal<AppPrincipal>()!!
                            call.respondText(principal.klientId.value)
                        }
                    }
                    authenticate(AZURE_AD_AUTHENTICATION_NAME) {
                        get("/saksbehandler") { call.respondText("ok") }
                    }
                }
            }
            val response: HttpResponse = client.get(path) { if (token != null) bearerAuth(token) }
            svar = Svar(response.status, response.bodyAsText())
        }
        return svar
    }

    private companion object {
        const val CLIENT_ID = "test-client-id"
        const val ISSUER_ID = "azuread"
    }
}
