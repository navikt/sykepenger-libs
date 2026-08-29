# sykepenger-libs - logging

Felles loggoppsett og logghjelpere for sykepengetjenestene: et delt logback-oppsett som inkluderes fra classpath,
Kotlin-funksjoner for å logge til både nav-logs og Team Logs, og MDC-hjelpere.

## Hvordan bruker jeg dette?

### Installasjon i repoet

```kotlin
dependencies {
    implementation("no.nav.sykepenger.libs:logging:<version>")
}
```

#### Viktig!

Dette loggebiblioteket sørger for riktig logback-oppsett for Team Logs og nav-logs, men annet oppsett (f. eks i
Nais-manifestet) må gjøres selv i hvert tjeneste. Se Nais-dokumentasjonen for nødvendig oppsett
for [Team Logs](https://doc.nais.io/observability/logging/how-to/team-logs/#naisyaml-configuration)
og for [Nav-logs](https://docs.nais.io/observability/logging/how-to/nav-logs-dashboards/#enable-logging-to-nav-logs)

### logback.xml-oppsett

Biblioteket har et felles logback-oppsett som det er meningen man skal inkludere i hver tjenestes `logback.xml`.

Eksempel på en minimal, fullverdig `src/main/resources/logback.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="no/nav/sykepenger/libs/logging/logback-base.xml"/>
</configuration>
```

#### Parametere

Deler av standardoppsettet er valgfritt overstyrbart ved å legge tilhørende `<property>` **før** `<include>`:

| Parameter            | Default | Beskrivelse                                                     |
|----------------------|---------|-----------------------------------------------------------------|
| `TJENESTEKALL_LEVEL` | `INFO`  | Nivå for `tjenestekall`-loggeren                                |
| `ROOT_LOG_LEVEL`     | `INFO`  | Nivå for root-loggeren (all logging som ikke overstyres ellers) |

Eksempel:

```xml

<configuration>
    <property name="TJENESTEKALL_LEVEL" value="DEBUG"/>
    <include resource="no/nav/sykepenger/libs/logging/logback-base.xml"/>
</configuration>
```

### MDC-oppsett

Biblioteket legger opp til at man skal sette verdier som man ønsker å bruke for å korrelere loggmeldinger, gjerne på
tvers av applikasjoner, på den såkalte [MDC'en](https://logback.qos.ch/manual/mdc.html). Alt som logges innenfor der
MDC'en er satt opp vil ha med seg feltene fra MDC'en. Det er derfor bedre å bruke MDC-parametere enn å logge ting som
vedtaksperiodeId etc. i hver enkelt loggmelding.

_Merk!_ MDC-parametere ender bare opp i Team Logs, ettersom de kan inneholde persondata.

Funksjonene heter `medMdc` (for synkron kode) og `coMedMdc` (for bruk i coroutines), og benyttes slik:

```kotlin
medMdc(MdcKey.VEDTAKSPERIODE_ID to vedtaksperiodeId.toString()) {
    // ...
    loggWarn("Feil vedtaksperiode")
}
```

Loggingen som utføres både rett i blokken og nedover i kallhierarkiet vil da inneholde vedtaksperiodeId som felt.

#### MdcKey

Nøklene til MDC'en er ikke fritekst, men er definert i enumen
[`MdcKey`](src/main/kotlin/no/nav/sykepenger/libs/logging/MdcKey.kt) — det felles settet med nøkler som kan settes på
MDC'en av sykepengetjenestene.

Fordi alle tjenestene henter nøklene fra samme sted, heter det samme begrepet det samme overalt, slik at et søk i Team
Logs faktisk korrelerer treff på tvers av tjenestene.

Se [KDoc-en i `MdcKey`](src/main/kotlin/no/nav/sykepenger/libs/logging/MdcKey.kt) for mer informasjon.

### Logging

Biblioteket innfører loggfunksjoner som extension-funksjoner som kan brukes på et hvilket som helst objekt. Typen man
benytter funksjonen på blir til logger-navnet.

Det betyr at i en hvilket som helst klasse kan man enkelt logge med en av extension-funksjonene, uten noe mer oppsett:

```kotlin
class MinKlasse {
    fun gjørNoeSpennende() {
        loggInfo("Dette var veldig spennende!")
    }
}
```

Der det ikke finnes noe objekt å henge loggingen på — for eksempel i en funksjon som er definert utenfor en klasse i en
Kotlin-fil, eller i andre statiske sammenhenger — bruker man i stedet en `NavngittLogger`. Den opprettes med
`navngittLogger(...)`, og navnet man oppgir blir logger-navnet:

```kotlin
private val logger = navngittLogger("no.nav.helse.ønskeliste.helpers")

fun inspiserØnskeliste(ønskeliste: Ønskeliste) {
    logger.info("Ønskelisten så fin ut")
}
```

Det finnes fem sett med funksjoner, ett for hvert loglevel:

| Extension-funksjon | `NavngittLogger`-metode | Loglevel |
|--------------------|-------------------------|----------|
| `loggError()`      | `error()`               | `ERROR`  |
| `loggWarn()`       | `warn()`                | `WARN`   |
| `loggInfo()`       | `info()`                | `INFO`   |
| `loggDebug()`      | `debug()`               | `DEBUG`  |
| `loggTrace()`      | `trace()`               | `TRACE`  |

Hver funksjon finnes i to varianter, én med en throwable og én uten. Throwable'n blir til stack trace i Team Logs.

Loggefunksjonene tar en melding og et valgfritt sett med key/value-par med detaljer til Team Logs. Alle meldinger logges
til både nav-logs og Team Logs, men det er kun den meldingen som går til Team Logs som har med detaljene. Dette
forhindrer at persondata legges ut i nav-logs, og tillater samtidig at den kan logges til Team Logs.

Eksempel:

```kotlin
loggWarn(
    "Fikk feil tilbake fra HentØnskeliste-tjenesten",
    "httpStatusCode" to status.toString(),
    "responseBody" to responseBody,
)
```

nav-logs får `Fikk feil tilbake fra HentØnskeliste-tjenesten`, mens Team Logs får
`Fikk feil tilbake fra HentØnskeliste-tjenesten - httpStatusCode: "…" responseBody: "…"`.

Detaljverdiene er `String?`, slik at det er tydelig hva som faktisk havner i loggen — konverter selv med `toString()`
der det trengs. `null` skrives som `null`, uten anførselstegn.

Alle funksjonene har en variant som tar en `Throwable` som andre argument. Stacktracen følger bare med til Team Logs,
ikke til nav-logs:

```kotlin
loggError("Klarte ikke hente snapshot", exception, "identitetsnummer" to identitetsnummer)
```

### Logging fra rammeverk

Rammeverk og tredjepartsbiblioteker logger gjennom sin egen SLF4J-`Logger`, og kjenner verken loggefunksjonene eller
oppsettet vårt. Slik logging vil havne både i nav-logs og i Team Logs. Stacktracen holdes likevel tilbake fra nav-logs,
og MDC-feltene likeså. Man trenger derfor ikke gjøre noe spesielt for å få med seg logging fra for eksempel Ktor eller
Kafka-klienten begge steder.

Stacktracer regnes som mulig persondata, og skrives derfor aldri til nav-logs — uansett hvem som logger dem.
