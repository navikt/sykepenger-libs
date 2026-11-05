# sykepenger-libs - outbox

Lagrer meldinger til rapiden i samme transaksjon som resten av endringen, og publiserer dem fra poden som er leder.

## Installasjon

```kotlin
dependencies {
    implementation("no.nav.sykepenger.libs:outbox:<version>")
}
```

Modulen tar med seg `leader-election` og API-et til rapids-and-rivers. Bruk samme versjon for alle modulene fra
sykepenger-libs.

## Skrive til outboxen

Skriv meldingen i samme transaksjon som resten av endringen. `Outbox.leggTil` bruker transaksjonen på
`connection` og verken committer eller ruller tilbake:

```kotlin
import no.nav.sykepenger.libs.outbox.Outbox

sessionOf(dataSource).use { session ->
    session.transaction { tx ->
        lagreAvgjørelse(tx, avgjørelse)
        Outbox.leggTil(
            connection = tx.connection.underlying,
            fødselsnummer = avgjørelse.fødselsnummer,
            melding = JsonMessage.newMessage("rettighetskategori_endret", mapOf(...)).toJson(),
        )
    }
}
```

Eksempelet bruker kotliquery, men alle biblioteker som gir tilgang til en `java.sql.Connection`, kan brukes.
`melding` er hele meldingen som JSON, og jobben publiserer den uendret. Ruller appen transaksjonen tilbake,
blir meldingen aldri publisert.

## Publisere fra outboxen

```kotlin
import no.nav.sykepenger.libs.leaderelection.NaisLeaderElection
import no.nav.sykepenger.libs.outbox.OutboxPubliseringsjobb

val leaderElection = NaisLeaderElection()
rapidsConnection.register(OutboxPubliseringsjobb(dataSource, rapidsConnection, leaderElection))
```

Appen må også starte og stoppe `leaderElection`, se [leader-election](../leader-election/README.md).

- Jobben starter ved `onStartup` og poller hvert halve sekund. Sett `pollIntervall` for å endre det.
- Den publiserer bare når `leaderElection.isLeader()` er `true`, og sjekker det før hver melding.
- Hver melding publiseres i en egen transaksjon: jobben låser den eldste upubliserte meldingen (`FOR UPDATE`),
  publiserer den med fødselsnummeret som nøkkel og setter `publisert_tidspunkt`.
- Feiler sendingen, ruller jobben tilbake, logger feilen og stopper runden. Meldingen og de etter den blir liggende,
  og jobben prøver igjen ved neste poll. Rekkefølgen holdes.
- Med `KafkaRapid` fra rapids-and-rivers stopper rapiden seg selv når en sending feiler. Da stopper også jobben,
  og meldingen blir publisert når poden har startet på nytt.
- Rekkefølgen følger `sekvensnummer`, altså rekkefølgen meldingene ble skrevet inn i tabellen, ikke rekkefølgen
  transaksjonene ble committet i. Skriver to transaksjoner samtidig, kan en melding med høyere sekvensnummer bli
  publisert før en med lavere.
- Ved `onShutdownSignal` stopper jobben pollingen og venter til en påbegynt publisering er ferdig.

## Testing

`publiserUpubliserte()` kjører én runde. Bruk den i tester for å publisere det som ligger i outboxen, uten å vente på
pollingen. I tester uten Nais kan `leaderElection` være `LeaderElection { true }`.

## Tabellen

Appen eier selv Flyway-migreringen. Lag tabellen og indeksen slik:

```sql
CREATE TABLE outbox
(
    id                  uuid        PRIMARY KEY,
    sekvensnummer       bigint      GENERATED ALWAYS AS IDENTITY UNIQUE,
    fodselsnummer       text        NOT NULL,
    melding             jsonb       NOT NULL,
    opprettet           timestamptz NOT NULL DEFAULT now(),
    publisert_tidspunkt timestamptz NULL
);

CREATE INDEX outbox_upublisert_idx ON outbox (sekvensnummer) WHERE publisert_tidspunkt IS NULL;
```

| Kolonne               | Innhold                                                         |
|-----------------------|-----------------------------------------------------------------|
| `id`                  | ID-en til meldingen i outboxen                                  |
| `sekvensnummer`       | Rekkefølgen meldingene ble skrevet i. Jobben publiserer i denne rekkefølgen |
| `fodselsnummer`       | Nøkkelen meldingen publiseres med                               |
| `melding`             | Hele meldingen som JSON, slik den skal publiseres               |
| `opprettet`           | Når meldingen ble skrevet                                       |
| `publisert_tidspunkt` | Når meldingen ble publisert. `null` til den er publisert        |

Den delvise indeksen gjør at jobben finner upubliserte meldinger raskt, også når tabellen har mange publiserte rader.
Modulens tester lager tabellen med SQL-en over.
