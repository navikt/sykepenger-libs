package no.nav.sykepenger.libs.kafka.test

import org.apache.kafka.clients.admin.AdminClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestKafkaTest {
    private val container = TestKafka.start(eksterneBootstrapServers = null)

    @AfterAll
    fun stopp() = container.stop()

    @Test
    fun `bruker ekstern Kafka og tåler at topicet finnes fra før`() {
        val ekstern = TestKafka.start(eksterneBootstrapServers = container.bootstrapServers)
        assertEquals(container.bootstrapServers, ekstern.bootstrapServers)

        ekstern.opprettTopics("test.topic")
        ekstern.opprettTopics("test.topic")
        ekstern.stop()

        AdminClient.create(container.connectionProperties()).use { admin ->
            assertTrue("test.topic" in admin.listTopics().names().get())
        }
    }
}
