# sykepenger-libs - ktor-auth

Felles Ktor-oppsett for å autentisere saksbehandlere og apper med Azure AD-token (Entra ID):

* **`configureJwtAuthentication`** — legger til en JWT-provider kalt `AZURE_AD_AUTHENTICATION_NAME` i
  `Authentication`. Tokenet valideres mot issuer, audience og JWKS, og kallet får en `SaksbehandlerPrincipal`.
* **`configureAppAuthentication`** — legger til en JWT-provider kalt `AZURE_AD_APP_AUTHENTICATION_NAME` som bare
  godtar maskin-til-maskin-token (client credentials). Tokenet valideres mot issuer, audience og JWKS, og må ha
  `idtyp=app` og `azp`. Kallet får en `AppPrincipal` med `KlientId` (`azp`). Hvilke apper som får token, styres av
  `accessPolicy.inbound` i Nais.
* **`SaksbehandlerPrincipal`** — saksbehandleren (`NavIdent`, `SaksbehandlerOid`, navn), tilgangene
  (`Tilgang.Les`/`Tilgang.Skriv`) og det rå `AccessToken` som kan brukes til OBO-veksling, for eksempel mot
  populasjonstilgangskontrollen.
* **`TilgangsgrupperTilTilganger`** — gjør om Entra-grupper i `groups`-claimet til tilganger. Skrivetilgang gir
  også lesetilgang.
* **`AzureAdConfig`** — klient-ID, issuer og JWKS-URI.

Token uten `NAVident` eller `oid`, eller med et `groups`-claim som ikke er en liste, gir 401. Token uten
`groups`-claim godtas, men gir ingen tilganger. 401-svar har tom body.

## Ta i bruk

```kotlin
dependencies {
    implementation("no.nav.sykepenger.libs:ktor-auth:<versjon>")
}
```

Appen må ha disse miljøvariablene. `AZURE_*` settes av Nais når `azure.application.enabled` er `true`.
`TILGANG_LES` og `TILGANG_SKRIV` er én eller flere kommaseparerte Entra-gruppe-ID-er.

| Variabel                       | Brukes av                     |
|--------------------------------|-------------------------------|
| `AZURE_APP_CLIENT_ID`          | `AzureAdConfig`               |
| `AZURE_OPENID_CONFIG_ISSUER`   | `AzureAdConfig`               |
| `AZURE_OPENID_CONFIG_JWKS_URI` | `AzureAdConfig`               |
| `TILGANG_LES`                  | `TilgangsgrupperTilTilganger` |
| `TILGANG_SKRIV`                | `TilgangsgrupperTilTilganger` |

```kotlin
fun Application.module(env: Map<String, String>) {
    configureJwtAuthentication(AzureAdConfig.fraEnv(env), TilgangsgrupperTilTilganger.fraEnv(env))
    routing {
        authenticate(AZURE_AD_AUTHENTICATION_NAME) {
            get("/api/noe") {
                val principal = call.principal<SaksbehandlerPrincipal>()!!
                if (!principal.harTilgang(Tilgang.Les)) return@get call.respond(HttpStatusCode.Forbidden)
                // ...
            }
        }
    }
}
```

I tester kan du utstede token med `MockOAuth2Server` og claimene `NAVident`, `oid` og `groups`.

### Apper

```kotlin
fun Application.module(env: Map<String, String>) {
    configureAppAuthentication(AzureAdConfig.fraEnv(env))
    routing {
        authenticate(AZURE_AD_APP_AUTHENTICATION_NAME) {
            post("/api/noe") {
                val principal = call.principal<AppPrincipal>()!!
                // ...
            }
        }
    }
}
```

`configureAppAuthentication` og `configureJwtAuthentication` kan brukes i samme app. Endepunktene velger provider med
`authenticate(...)`. `MockOAuth2Server.issueToken` overstyrer `azp`, så bruk `anyToken` med claimene `aud`, `idtyp=app`
og `azp` i tester.
