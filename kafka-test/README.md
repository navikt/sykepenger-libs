# sykepenger-libs - kafka-test

`TestKafka` gir integrasjonstester en Kafka. Som standard startes en Kafka-testcontainer.

```kotlin
val kafka = TestKafka.start()
kafka.opprettTopics("tbd.rapid.v1")
kafka.bootstrapServers       // til appens Kafka-oppsett
kafka.connectionProperties() // bootstrap.servers og PLAINTEXT, for egne produsenter og konsumenter
kafka.stop()
```

## Kafka utenfor testene

Er miljøvariabelen `TEST_KAFKA_BOOTSTRAP_SERVERS` satt, startes ingen container. Testene bruker da Kafka på adressen
i variabelen, for eksempel `localhost:9092`. Det er nyttig der testene ikke får starte containere selv, som i en
sandkasse for en kodeagent. Postgres har tilsvarende støtte i `TestDatabase` i [`postgres`](../postgres/README.md).

Kafka kan da ha topics og meldinger fra tidligere kjøringer. `opprettTopics` tåler at topicene finnes fra før. Tester
som leser fra et topic, bør begynne fra slutten eller filtrere på egne meldinger.

```sh
docker run -d --rm --name test-kafka -p 9092:9092 apache/kafka-native:3.9.1
TEST_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 ./gradlew test
```

## Ta i bruk

```kotlin
dependencies {
    testImplementation("no.nav.sykepenger.libs:kafka-test:<versjon>")
}
```
