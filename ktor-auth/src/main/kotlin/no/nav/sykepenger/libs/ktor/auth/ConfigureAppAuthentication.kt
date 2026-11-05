package no.nav.sykepenger.libs.ktor.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

const val AZURE_AD_APP_AUTHENTICATION_NAME = "azure-ad-app-jwt"

/**
 * Legger til en JWT-provider kalt [AZURE_AD_APP_AUTHENTICATION_NAME] i [Authentication] som bare godtar
 * maskin-til-maskin-token (client credentials) fra Azure AD, og gir en [AppPrincipal].
 *
 * Entra ID setter `idtyp=app` bare på maskin-til-maskin-token. Token uten `idtyp=app`, for eksempel token utstedt på vegne
 * av en saksbehandler, eller uten `azp`, avvises med 401. Hvilke apper som får token, styres av `inbound` i Nais.
 *
 * Kan kombineres med [configureJwtAuthentication] i samme app.
 */
fun Application.configureAppAuthentication(azureAdConfig: AzureAdConfig) {
    val jwkProvider = jwkProvider(azureAdConfig)

    authentication {
        jwt(AZURE_AD_APP_AUTHENTICATION_NAME) {
            verifier(jwkProvider, azureAdConfig.issuerUrl) {
                withAudience(azureAdConfig.clientId)
            }
            validate { credential ->
                val payload = credential.payload
                if (payload.getClaim("idtyp").asString() != "app") return@validate null
                val klientId = payload.ikkeBlankStreng("azp") ?: return@validate null
                AppPrincipal(KlientId(klientId))
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
