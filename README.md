# sykepenger-libs

Fellesbiblioteker for sykepengetjenestene. Hver modul i dette repoet publiseres som sitt eget artefakt, slik at
tjenestene kan ta i bruk akkurat de bibliotekene de trenger.

## Moduler

| Modul                              | Beskrivelse                                                                      |
|------------------------------------|----------------------------------------------------------------------------------|
| [`ktor-auth`](ktor-auth/README.md) | Ktor-autentisering av saksbehandlere med Azure AD-token og Entra-tilgangsgrupper |
| [`leader-election`](leader-election/README.md) | Leader election med SSE fra Nais-elector og styring av oppstart og stopp |
| [`logging`](logging/README.md)     | Felles logback-oppsett (Team Logs / OpenSearch), loggefunksjoner, og MDC-oppsett |
| [`postgres`](postgres/README.md)   | Datasource, Flyway-migrering og testdatabase for Postgres i Nais-apper           |
| [`testing`](testing/README.md)     | Hjelpemidler til testing - assertions og testdatagenerering                      |

Se den enkelte modulens README (linket til i tabellen over) for hvordan den brukes.

## Publisering

Bibliotekene publiseres til
[GitHub Package Registry](https://github.com/orgs/navikt/packages?repo_name=sykepenger-libs)
automatisk etter et vellykket bygg på `main`. Versjonsnummeret genereres fra byggetidspunktet på formen `ÅÅÅÅMMDD.TTMM`,
f. eks. `20262808.1542`. Dette sikrer at nyere bygg alltid har høyere versjonsnummer.

Alle modulene bygges, testes og publiseres samlet, med samme versjonsnummer, av workflowen
`.github/workflows/main.yml`. Moduler som avhenger av hverandre, bruker prosjektavhengigheter i Gradle. En publisert
modul peker derfor alltid på en versjon av de andre modulene som er bygget og publisert sammen med den. Endringer bare
i dokumentasjon (`*.md`) fører ikke til publisering.

Tjenestene kan fortsatt ta i bruk bare de modulene de trenger. Bruk samme versjon for alle modulene fra
sykepenger-libs i en tjeneste.

## Legge til en ny modul

1. Lag en katalog med modulnavnet, med en `build.gradle.kts` som bruker `no.nav.sykepenger.kotlin`-pluginen.
2. Legg modulen til i `include(...)` i `settings.gradle.kts`.
3. Bruk `project(":<modul>")` når modulen avhenger av en annen modul i repoet.
4. Skriv en `README.md` for modulen, og legg den til i tabellen over moduler lenger opp.

## For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen
[#team-sas-værsågod](https://nav-it.slack.com/archives/C019637N90X).
