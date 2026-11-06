# sykepenger-libs

Fellesbiblioteker for sykepengetjenestene. Hver modul i dette repoet publiseres som sitt eget artefakt, slik at
tjenestene kan ta i bruk akkurat de bibliotekene de trenger.

## Moduler

| Modul                              | Beskrivelse                                                                      |
|------------------------------------|----------------------------------------------------------------------------------|
| [`ktor-auth`](ktor-auth/README.md) | Ktor-autentisering av saksbehandlere med Azure AD-token og Entra-tilgangsgrupper |
| [`leader-election`](leader-election/README.md) | Leader election med SSE fra Nais-elector og styring av oppstart og stopp |
| [`logging`](logging/README.md)     | Felles logback-oppsett (Team Logs / OpenSearch), loggefunksjoner, og MDC-oppsett |
| [`outbox`](outbox/README.md)       | Outbox i Postgres som publiserer meldinger på rapiden fra poden som er leder     |
| [`postgres`](postgres/README.md)   | Datasource, Flyway-migrering og testdatabase for Postgres i Nais-apper           |
| [`testing`](testing/README.md)     | Hjelpemidler til testing - assertions og testdatagenerering                      |

Se den enkelte modulens README (linket til i tabellen over) for hvordan den brukes.

## Publisering

Bibliotekene publiseres til
[GitHub Package Registry](https://github.com/orgs/navikt/packages?repo_name=sykepenger-libs)
automatisk etter et vellykket bygg på `main`. Versjonsnummeret genereres fra byggetidspunktet på formen `ÅÅÅÅMMDD.TTMM`,
f. eks. `20262808.1542`. Dette sikrer at nyere bygg alltid har høyere versjonsnummer.

Hver modul bygges, testes og publiseres uavhengig av de andre. Modulen får bare ny versjon når den selv er endret,
eller når fellesoppsettet for byggingen er endret (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`,
`gradle/`, `gradlew`, `gradlew.bat`). Endringer i modulens workflow eller den gjenbrukbare workflowen fører også til
publisering. Endringer bare i dokumentasjon (`*.md`) fører ikke til publisering.

Hver modul har sin egen workflow, `.github/workflows/main-<modul>.yml`, som avgjør når modulen skal publiseres.
Selve byggingen, testingen og publiseringen ligger i den gjenbrukbare workflowen
`.github/workflows/bygg-test-og-publiser-modul.yml`. Du kan også starte en modulworkflow manuelt.

Moduler som avhenger av andre moduler i repoet, bruker publiserte versjoner fra `gradle/libs.versions.toml`,
også i tester. Oppdater avhengigheten etter at en ny versjon av modulen er publisert. Modulene trenger ikke å ha
samme versjonsnummer i en tjeneste.

## Legge til en ny modul

1. Lag en katalog med modulnavnet, med en `build.gradle.kts` som bruker `no.nav.sykepenger.kotlin`-pluginen.
2. Legg modulen til i `include(...)` i `settings.gradle.kts`.
3. Kopier `.github/workflows/main-logging.yml` til `.github/workflows/main-<modul>.yml`, og bytt ut `logging` med
   navnet på den nye modulen (i `name`, i `paths` og i `with.modul`).
4. Bruk en publisert versjon fra `gradle/libs.versions.toml` når modulen avhenger av en annen modul i repoet.
5. Skriv en `README.md` for modulen, og legg den til i tabellen over moduler lenger opp.

## For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen
[#team-sas-værsågod](https://nav-it.slack.com/archives/C019637N90X).
