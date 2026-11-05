# sykepenger-libs - leader-election

Følger leader-endringer fra Nais-elector over SSE med JDK-ens `HttpClient`.

## Installasjon

```kotlin
dependencies {
    implementation("no.nav.sykepenger.libs:leader-election:<version>")
}
```

Modulen krever Java 25 og bruker `logging`-modulen med samme versjon. Se [logging-oppsettet](../logging/README.md)
for `logback.xml`.

## Bruk i Nais

Slå på elector-sidecaren i Nais-manifestet:

```yaml
spec:
  leaderElection: true
```

Opprett abonnementet når appen starter:

```kotlin
import no.nav.sykepenger.libs.leaderelection.NaisLeaderElection

val leaderElection = NaisLeaderElection()
leaderElection.start()

if (leaderElection.isLeader()) {
    publiserFraOutbox()
}
```

Kall `leaderElection.close()` når appen stopper. I en Rapids-app kalles `start()` fra `onStartup`
og `close()` fra `onShutdownSignal` på appens `RapidsConnection.StatusListener`.
Modulen har ingen avhengighet til Rapids.

Konstruktøren leser `ELECTOR_SSE_URL` og `HOSTNAME` fra miljøet. Begge må finnes, og `HOSTNAME` kan ikke være tomt.
Du kan også oppgi URL og hostname direkte, for eksempel i tester:

```kotlin
import kotlin.time.Duration.Companion.milliseconds

val leaderElection = NaisLeaderElection(
    electorSseUrl = "http://localhost:8080/sse",
    hostname = "min-pod",
    ventetidFørNyTilkobling = 100.milliseconds,
)
```

## Oppførsel

- `isLeader()` gir `false` før den første leader-hendelsen og etter `close()`.
- Elector sender JSON med feltet `name`. Bare poden med samme hostname er leader.
  Et manglende, tomt eller null `name` betyr at ingen er leader.
- Siste kjente leader gjelder også ved brudd i strømmen. Elector beholder leader til poden slettes.
- Abonnementet kobler til på nytt etter brudd, HTTP-feil eller ugyldig JSON. Standard ventetid er fem sekunder.
  Ventetiden må være endelig og minst ett millisekund.
- Tilkoblingsfeil logges som WARN, og som ERROR etter tolv feil på rad.
- `start()` starter én bakgrunnstråd. Flere kall mens abonnementet kjører gjør ingenting.
- `close()` lukker strømmen, avbryter tilkobling eller venting og venter på at bakgrunnstråden stopper.
  Flere kall er tillatt. Et stoppet abonnement kan ikke startes igjen.

## Bruk uten Nais

Jobber som bare skal kjøre på leader, tar grensesnittet `LeaderElection` som parameter:

```kotlin
import no.nav.sykepenger.libs.leaderelection.LeaderElection

val alltidLeader = LeaderElection { true }
val aldriLeader = LeaderElection { false }
```

Da kan appen teste jobben uten elector. Modulens egne tester bruker en falsk SSE-server og det offentlige
grensesnittet for å sjekke leader-endringer, ny tilkobling og stopp.
