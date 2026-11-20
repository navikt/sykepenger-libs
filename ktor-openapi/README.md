# sykepenger-libs - ktor-openapi

Felles oppsett for å lage OpenAPI-specen fra Ktor-rutene, med Ktors egen OpenAPI-støtte
(`ktor-server-routing-openapi`). Specen lages fra koden, slik [ADR 0017 i sas-meta](https://github.com/navikt/helse-sas-meta/blob/main/docs/adr/0017-spokelse-genererer-openapi-fra-koden.md)
beskriver.

* **`configureOpenApi(tittel, versjon)`** — serverer specen på `/api/openapi.json` og Swagger UI på `/api/swagger`.
  Bare ruter som er beskrevet med `.describe {}` kommer med, så helsesjekker og metrikker holdes ute.
* **Skjema uten `@Serializable`** — skjemaet lages med refleksjon over Kotlin-klassene, slik at DTO-er som Jackson
  serialiserer, ikke trenger kotlinx.serialization. `BigDecimal` blir `number`, `YearMonth` blir tekst på formen
  `2026-01`, og `UUID`, `LocalDate` og `Instant` blir tekst med format.
* **`assertOpenApiSnapshot(spec, snapshot)`** (test fixtures) — sammenligner specen med en snapshot i repoet, slik at
  hver kontraktendring blir en synlig diff i PR-en. Kjør testen med miljøvariabelen
  `OPPDATER_OPENAPI_SNAPSHOT=true` for å oppdatere snapshoten.

## Ta i bruk

```kotlin
dependencies {
    implementation("no.nav.sykepenger.libs:ktor-openapi:<versjon>")
    testImplementation(testFixtures("no.nav.sykepenger.libs:ktor-openapi:<versjon>"))
}
```

```kotlin
@OptIn(ExperimentalKtorApi::class)
fun Application.module() {
    configureOpenApi(tittel = "sp-noe", versjon = "1")
    routing {
        get("/api/noe") {
            call.respond(ApiNoe(...))
        }.describe {
            summary = "Henter noe"
            responses {
                HttpStatusCode.OK { schema = jsonSchema<ApiNoe>() }
            }
        }
    }
}
```

`.describe {}` er eksperimentelt i Ktor og krever `@OptIn(ExperimentalKtorApi::class)`.

### Snapshot-test

```kotlin
@Test
fun `OpenAPI-specen er lik snapshoten`() =
    testApplication {
        application { module() }
        assertOpenApiSnapshot(client.get("/api/openapi.json").bodyAsText(), Path.of("src/test/resources/openapi/sp-noe.json"))
    }
```
