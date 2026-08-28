# sykepenger-libs

Fellesbiblioteker for sykepengetjenestene. Hver modul i dette repoet er et selvstendig bibliotek som publiseres som sitt
eget artefakt, slik at tjenestene kan ta i bruk akkurat de bibliotekene de trenger — uavhengig av hverandre.

## Moduler

| Modul                          | Beskrivelse                                                                      |
|--------------------------------|----------------------------------------------------------------------------------|
| [`logging`](logging/README.md) | Felles logback-oppsett (Team Logs / OpenSearch), loggefunksjoner, og MDC-oppsett |
| [`testing`](testing/README.md) | Hjelpemidler til testing - assertions og testdatagenerering                      |

Se den enkelte modulens README (linket til i tabellen over) for hvordan den brukes.

## Publisering

Bibliotekene publiseres til
[GitHub Package Registry](https://github.com/orgs/navikt/packages?repo_name=sykepenger-libs)
automatisk etter et vellykket bygg på `main`. Versjonsnummeret genereres fra byggetidspunktet på formen `ÅÅÅÅMMDD.TTMM`,
f. eks. `20262808.1542`. Dette sikrer at nyere bygg alltid har høyere versjonsnummer.

Hver modul publiseres uavhengig av de andre: modulen får bare ny versjon når den selv er endret, eller når
fellesoppsettet for byggingen er endret (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/`,
`gradlew`). Endringer i dokumentasjon fører ikke til publisering.

Hver modul har derfor sin egen workflow, `.github/workflows/main-<modul>.yml`, som avgjør *når* modulen skal
publiseres. Selve byggingen, testingen og publiseringen er lik for alle moduler, og ligger i den gjenbrukbare
workflowen `.github/workflows/bygg-test-og-publiser-modul.yml`.

## Legge til en ny modul

1. Lag en katalog med modulnavnet, med en `build.gradle.kts` som bruker `no.nav.helse.sas.sas-kotlin`-pluginen.
2. Legg modulen til i `include(...)` i `settings.gradle.kts`.
3. Kopier `.github/workflows/main-logging.yml` til `.github/workflows/main-<modul>.yml`, og bytt ut `logging` med
   navnet på den nye modulen (i `name`, i `paths` og i `with.modul`).
4. Skriv en `README.md` for modulen, og legg den til i tabellen over moduler lenger opp.

## For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen
[#team-sas-værsågod](https://nav-it.slack.com/archives/C019637N90X).
