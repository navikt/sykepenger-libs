# sykepenger-libs

Fellesbiblioteker for sykepengetjenestene. Hver modul i dette repoet er et selvstendig bibliotek som publiseres som sitt
eget artefakt, slik at tjenestene kan ta i bruk akkurat de bibliotekene de trenger — uavhengig av hverandre.

## Moduler

| Modul                          | Beskrivelse                                                                      |
|--------------------------------|----------------------------------------------------------------------------------|
| [`logging`](logging/README.md) | Felles logback-oppsett (Team Logs / OpenSearch), loggefunksjoner, og MDC-oppsett |

Se den enkelte modulens README (linket til i tabellen over) for hvordan den brukes.

## Publisering

Bibliotekene publiseres til
[GitHub Package Registry](https://github.com/orgs/navikt/packages?repo_name=sykepenger-libs)
automatisk etter et vellykket bygg på `main`. Versjonsnummeret genereres fra byggetidspunktet på formen `ÅÅÅÅMMDD.TTMM`,
f. eks. `20262808.1542`. Dette sikrer at nyere bygg alltid har høyere versjonsnummer.

## For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen
[#team-sas-værsågod](https://nav-it.slack.com/archives/C019637N90X).
