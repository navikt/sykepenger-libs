package no.nav.sykepenger.libs.postgres

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EksternTestDatabaseTest {
    private val server = TestDatabase.start(eksternJdbcUrl = null)

    @AfterAll
    fun stopp() = server.stop()

    @Test
    fun `oppretter en egen database på serveren og sletter den når den stoppes`() {
        val første = TestDatabase.start(eksternJdbcUrl = server.jdbcUrl).also { it.migrer() }
        val andre = TestDatabase.start(eksternJdbcUrl = server.jdbcUrl).also { it.migrer() }

        første.dataSource.connection.use { connection ->
            connection.createStatement().use { it.executeUpdate("insert into test_tabell (verdi) values ('a')") }
        }
        assertEquals(1, første.antallRader("test_tabell"))
        assertEquals(0, andre.antallRader("test_tabell"))
        assertNotEquals(databasenavn(første), databasenavn(andre))

        val navn = databasenavn(første)
        første.stop()
        andre.stop()
        assertFalse(navn in databaserPåServeren())
    }

    private fun databasenavn(database: TestDatabase) =
        database.dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("select current_database()").use {
                    it.next()
                    it.getString(1)
                }
            }
        }

    private fun databaserPåServeren() =
        server.dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("select datname from pg_database").use { resultSet ->
                    buildList { while (resultSet.next()) add(resultSet.getString(1)) }
                }
            }
        }
}
