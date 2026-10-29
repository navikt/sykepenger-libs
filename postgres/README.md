# sykepenger-libs - postgres

Felles oppsett for Postgres i Nais-apper:

* **`DatabaseConfig.fraEnv`** — leser `DATABASE_JDBC_URL`, eller bygger URL-en fra
  `DATABASE_HOST/_PORT/_DATABASE/_USERNAME/_PASSWORD` slik Nais setter dem. Prefikset kan overstyres med
  `envVarPrefix`. `DB_POOL_SIZE` overstyrer størrelsen på poolen (standard 10), og `NAIS_APP_NAME` blir navnet på
  poolen.
* **`DatabaseConfig.dataSource()`** — lager en `HikariDataSource` med felles standardverdier (`HikariDefaults`).
* **`migrerSynkront(config)`** — kjører Flyway-migreringene med en egen, liten pool som lukkes etterpå. Venter på
  migreringslåsen, slik at flere poder som starter samtidig ikke migrerer i parallell.

## Ta i bruk

```kotlin
dependencies {
    implementation("no.nav.sykepenger.libs:postgres:<versjon>")
    testImplementation(testFixtures("no.nav.sykepenger.libs:postgres:<versjon>"))
}
```

```kotlin
val databaseConfig = DatabaseConfig.fraEnv(env)
migrerSynkront(databaseConfig)
val dataSource = databaseConfig.dataSource()
```

Migreringene leses fra `classpath:db/migration`, med mindre du sender inn andre `flywayLocations`.

## Testing

Test fixtures-artefaktet har `TestDatabase`, en Postgres-testcontainer med hjelpemetoder:

```kotlin
val testDatabase = TestDatabase.start().also { it.migrer() }

testDatabase.dataSource                // HikariDataSource mot containeren
testDatabase.tøm("tabell1", "tabell2") // truncate ... restart identity
testDatabase.antallRader("tabell1")
testDatabase.env()                     // DATABASE_JDBC_URL, for tester som starter hele appen
```
