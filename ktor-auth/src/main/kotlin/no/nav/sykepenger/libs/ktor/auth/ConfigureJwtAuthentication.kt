package no.nav.sykepenger.libs.ktor.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import java.net.URI
import java.util.concurrent.TimeUnit

const val AZURE_AD_AUTHENTICATION_NAME = "azure-ad-jwt"

/**
 * Installerer [Authentication] med en JWT-provider kalt [AZURE_AD_AUTHENTICATION_NAME] som godtar
 * Azure AD-token utstedt på vegne av en saksbehandler, og gir en [SaksbehandlerPrincipal].
 *
 * Token uten `NAVident` eller `oid`, eller med et `groups`-claim som ikke er en liste, avvises med 401.
 * Token uten `groups`-claim godtas, men gir ingen tilganger.
 */
fun Application.configureJwtAuthentication(
    azureAdConfig: AzureAdConfig,
    tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
) {
    val jwkProvider =
        JwkProviderBuilder(URI(azureAdConfig.jwkProviderUri).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    install(Authentication) {
        jwt(AZURE_AD_AUTHENTICATION_NAME) {
            verifier(jwkProvider, azureAdConfig.issuerUrl) {
                withAudience(azureAdConfig.clientId)
            }
            validate { credential ->
                val payload = credential.payload
                val navIdent = payload.ikkeBlankStreng("NAVident") ?: return@validate null
                val oid = payload.ikkeBlankStreng("oid") ?: return@validate null
                val navn = payload.ikkeBlankStreng("name") ?: payload.ikkeBlankStreng("preferred_username") ?: navIdent
                val entraGrupper = payload.entraGrupper() ?: return@validate null
                val accessToken =
                    request.headers[HttpHeaders.Authorization]
                        ?.substringAfter(' ')
                        ?.let(::AccessToken)
                        ?: return@validate null

                SaksbehandlerPrincipal(
                    saksbehandler = Saksbehandler(NavIdent(navIdent), SaksbehandlerOid(oid), navn),
                    tilganger = tilgangsgrupperTilTilganger.tilganger(entraGrupper),
                    accessToken = accessToken,
                )
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

private fun Payload.ikkeBlankStreng(claim: String) = getClaim(claim).asString()?.takeIf(String::isNotBlank)

private fun Payload.entraGrupper(): Set<String>? {
    val claim = getClaim("groups")
    if (claim.isMissing || claim.isNull) return emptySet()
    return try {
        claim.asList(String::class.java)?.toSet()
    } catch (_: JWTDecodeException) {
        null
    }
}
