package no.nav.sykepenger.libs.kafka.test

import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.testcontainers.kafka.KafkaContainer
import java.util.Properties
import java.util.concurrent.ExecutionException

/**
 * Kafka for integrasjonstester.
 *
 * Som standard startes en Kafka-testcontainer. Er miljøvariabelen [TEST_KAFKA_BOOTSTRAP_SERVERS] satt, startes ingen
 * container, og testene bruker Kafka på adressen i variabelen. Kafka kan da ha topics og meldinger fra tidligere
 * kjøringer, så [opprettTopics] tåler at topicene finnes fra før.
 */
class TestKafka private constructor(
    val bootstrapServers: String,
    private val container: KafkaContainer?,
) : AutoCloseable {
    /** Klientoppsett med `bootstrap.servers` og `PLAINTEXT`, som grunnlag for produsenter, konsumenter og admin. */
    fun connectionProperties() =
        Properties().apply {
            put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(SECURITY_PROTOCOL_CONFIG, SecurityProtocol.PLAINTEXT.name)
        }

    fun opprettTopics(
        vararg topics: String,
        partisjoner: Int = 1,
    ) {
        AdminClient.create(connectionProperties()).use { admin ->
            topics.forEach { topic ->
                try {
                    admin.createTopics(listOf(NewTopic(topic, partisjoner, 1))).all().get()
                } catch (e: ExecutionException) {
                    if (e.cause !is TopicExistsException) throw e
                }
            }
        }
    }

    fun stop() {
        container?.stop()
    }

    override fun close() = stop()

    companion object {
        /** `bootstrap.servers` til en Kafka som brukes i stedet for testcontaineren, for eksempel `localhost:9092`. */
        const val TEST_KAFKA_BOOTSTRAP_SERVERS = "TEST_KAFKA_BOOTSTRAP_SERVERS"

        /**
         * @param eksterneBootstrapServers brukes i stedet for en testcontainer når den er satt. Standardverdien er
         * miljøvariabelen [TEST_KAFKA_BOOTSTRAP_SERVERS].
         */
        fun start(
            kafkaImage: String = "apache/kafka-native:3.9.1",
            eksterneBootstrapServers: String? = System.getenv(TEST_KAFKA_BOOTSTRAP_SERVERS)?.takeIf { it.isNotBlank() },
        ): TestKafka {
            if (eksterneBootstrapServers != null) return TestKafka(eksterneBootstrapServers, null)
            val container = KafkaContainer(kafkaImage).apply { start() }
            return TestKafka(container.bootstrapServers, container)
        }
    }
}
