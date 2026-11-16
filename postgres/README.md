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

Test fixtures-artefaktet har `TestDatabase`, en Postgres-database for tester med hjelpemetoder. Som standard startes en
Postgres-testcontainer.

```kotlin
val testDatabase = TestDatabase.start().also { it.migrer() }

testDatabase.dataSource                // HikariDataSource mot containeren
testDatabase.tøm("tabell1", "tabell2") // truncate ... restart identity
testDatabase.antallRader("tabell1")
testDatabase.env()                     // DATABASE_JDBC_URL, for tester som starter hele appen
```

### Postgres utenfor testene

Er miljøvariabelen `TEST_POSTGRES_JDBC_URL` satt, startes ingen container. `TestDatabase.start()` oppretter da en ny
database med unikt navn på serveren URL-en peker på, og sletter den i `stop()` eller når JVM-en avslutter. Testene blir isolert fra hverandre selv
om de deler server. URL-en må ha med `user` og `password`, og brukeren må ha lov til å opprette databaser. Kafka har
tilsvarende støtte i [`kafka-test`](../kafka-test/README.md).

```sh
docker run -d --rm --name test-postgres -e POSTGRES_PASSWORD=test -p 5432:5432 postgres:18-alpine
TEST_POSTGRES_JDBC_URL='jdbc:postgresql://localhost:5432/postgres?user=postgres&password=test' ./gradlew test
```

Registrer variabelen som input til testene, så Gradle ikke hopper over dem når du bytter mellom container og ekstern
Postgres:

```kotlin
tasks.test {
    inputs.property("TEST_POSTGRES_JDBC_URL", providers.environmentVariable("TEST_POSTGRES_JDBC_URL").orElse(""))
}
```
