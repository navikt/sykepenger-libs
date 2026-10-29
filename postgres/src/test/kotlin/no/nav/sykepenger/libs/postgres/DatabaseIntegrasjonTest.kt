package no.nav.sykepenger.libs.postgres

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseIntegrasjonTest {
    private val testDatabase = TestDatabase.start()

    @AfterAll
    fun stopp() = testDatabase.stop()

    @Test
    fun `migrerer og kobler til med konfigurasjon fra miljøvariablene til TestDatabase`() {
        val config = DatabaseConfig.fraEnv(testDatabase.env() + ("NAIS_APP_NAME" to "sp-test"))
        migrerSynkront(config)
        migrerSynkront(config)

        config.dataSource().use { dataSource ->
            assertEquals("sp-test", dataSource.poolName)
            dataSource.connection.use { connection ->
                connection.createStatement().use { it.executeUpdate("insert into test_tabell (verdi) values ('a'), ('b')") }
            }
        }
        assertEquals(2, testDatabase.antallRader("test_tabell"))

        testDatabase.tøm("test_tabell")
        assertEquals(0, testDatabase.antallRader("test_tabell"))
    }

    @Test
    fun `avviser ugyldige tabellnavn`() {
        assertFailsWith<IllegalArgumentException> { testDatabase.tøm("test_tabell; drop table test_tabell") }
    }
}
