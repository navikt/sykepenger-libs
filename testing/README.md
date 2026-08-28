# sykepenger-libs - testing

Hjelpemidler til bruk i tester:

* **`assertJsonEquals`** — sammenlikner JSON uavhengig av feltrekkefølge, med mulighet for å se bort fra
  felter angitt med punktseparerte stier.
* **Andre assertions** — tidspunkter (`assertIFortiden`, `assertAfter`, `assertEqualsByMicrosecond` m.fl.) og
  tall (`assertAtLeast`, `assertIsNumber`).
* **Testdata** — tilfeldige verdier og sammenhengende data-klasser: navn, identitetsnummer (syntetiske
  fødselsnummer og D-nummer), aktørId, organisasjonsnummer og -navn, datoer og NAV-identer.

Funksjonene og typene er dokumentert med KDoc, så bruk autofullføring og quick documentation i IDE-en for
detaljene.

## Ta i bruk

```kotlin
dependencies {
    testImplementation("com.github.navikt.tbd-libs:test:<version>")
}
```
