package no.nav.sykepenger.libs.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

/**
 * Postgres-database med Flyway-migrering og generelle databasehjelpere for integrasjonstester.
 *
 * Som standard startes en Postgres-testcontainer. Er miljøvariabelen [TEST_POSTGRES_JDBC_URL] satt, startes ingen
 * container. Da opprettes en ny database med unikt navn på serveren URL-en peker på. Den slettes i [stop], eller
 * når JVM-en avslutter.
 *
 * Opprett én instans per testkjøring når testene skal dele en langlevende database. Kall [tøm] mellom testene for å
 * isolere testdataene.
 */
class TestDatabase private constructor(
    /** JDBC-URL med brukernavn og passord, på samme form som `<prefiks>_JDBC_URL` fra Nais. */
    val jdbcUrl: String,
    private val hikari: HikariDataSource,
    private val rydd: () -> Unit,
) : AutoCloseable {
    val dataSource: DataSource get() = hikari

    /** Miljøvariabler som [DatabaseConfig.fraEnv] forstår, for tester som starter hele appen. */
    fun env(envVarPrefix: String = "DATABASE"): Map<String, String> = mapOf("${envVarPrefix}_JDBC_URL" to jdbcUrl)

    fun migrer(locations: List<String> = listOf("classpath:db/migration")) {
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations(*locations.toTypedArray())
            .cleanDisabled(true)
            .load()
            .migrate()
    }

    fun tøm(vararg tabeller: String) {
        require(tabeller.isNotEmpty()) { "Minst én tabell må oppgis" }
        tabeller.forEach(::validerTabellnavn)

        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("truncate table ${tabeller.joinToString()} restart identity")
            }
        }
    }

    fun antallRader(tabell: String): Int {
        validerTabellnavn(tabell)
        return dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("select count(1) from $tabell").use { resultSet ->
                    check(resultSet.next()) { "Spørringen returnerte ingen rad for tabellen $tabell" }
                    resultSet.getInt(1)
                }
            }
        }
    }

    fun stop() {
        hikari.close()
        rydd()
    }

    override fun close() = stop()

    companion object {
        /** JDBC-URL med `user` og `password` til en Postgres-server som brukes i stedet for testcontaineren. */
        const val TEST_POSTGRES_JDBC_URL = "TEST_POSTGRES_JDBC_URL"

        private val postgresUrl = Regex("(jdbc:postgresql://[^/?]+)(?:/[^?]*)?(\\?.*)?")
        private val gyldigTabellnavn = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?")

        /**
         * @param eksternJdbcUrl brukes i stedet for en testcontainer når den er satt. Standardverdien er
         * miljøvariabelen [TEST_POSTGRES_JDBC_URL]. Brukeren må ha lov til å opprette og slette databaser.
         */
        fun start(
            postgresImage: String = "postgres:18-alpine",
            maximumPoolSize: Int = 3,
            eksternJdbcUrl: String? = System.getenv(TEST_POSTGRES_JDBC_URL)?.takeIf { it.isNotBlank() },
        ): TestDatabase {
            require(maximumPoolSize > 0) { "maximumPoolSize må være større enn 0" }
            return if (eksternJdbcUrl != null) {
                startMotEksternServer(eksternJdbcUrl, maximumPoolSize)
            } else {
                startContainer(postgresImage, maximumPoolSize)
            }
        }

        private fun startContainer(
            postgresImage: String,
            maximumPoolSize: Int,
        ): TestDatabase {
            val container = PostgreSQLContainer(postgresImage).apply { start() }
            val jdbcUrl = medBruker(container.jdbcUrl, container.username, container.password)
            return TestDatabase(jdbcUrl, pool(jdbcUrl, maximumPoolSize), container::stop)
        }

        private fun startMotEksternServer(
            serverJdbcUrl: String,
            maximumPoolSize: Int,
        ): TestDatabase {
            val databasenavn = "test_${UUID.randomUUID().toString().replace("-", "")}"
            utfør(serverJdbcUrl, "create database $databasenavn")
            val jdbcUrl = byttDatabase(serverJdbcUrl, databasenavn)
            val slettet = AtomicBoolean(false)
            val slett = {
                if (slettet.compareAndSet(false, true)) utfør(serverJdbcUrl, "drop database if exists $databasenavn with (force)")
            }
            // Testene stopper sjelden databasen selv, så uten denne blir det liggende igjen en database per kjøring.
            Runtime.getRuntime().addShutdownHook(Thread { runCatching(slett) })
            return TestDatabase(jdbcUrl, pool(jdbcUrl, maximumPoolSize), slett)
        }

        private fun pool(
            jdbcUrl: String,
            maximumPoolSize: Int,
        ) = HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.maximumPoolSize = maximumPoolSize
            },
        )

        private fun utfør(
            jdbcUrl: String,
            sql: String,
        ) {
            DriverManager.getConnection(jdbcUrl).use { connection ->
                connection.createStatement().use { it.executeUpdate(sql) }
            }
        }

        private fun medBruker(
            jdbcUrl: String,
            brukernavn: String,
            passord: String,
        ): String {
            val skilletegn = if ('?' in jdbcUrl) '&' else '?'
            return "$jdbcUrl${skilletegn}user=$brukernavn&password=$passord"
        }

        private fun byttDatabase(
            jdbcUrl: String,
            databasenavn: String,
        ): String {
            val match = requireNotNull(postgresUrl.matchEntire(jdbcUrl)) { "$TEST_POSTGRES_JDBC_URL må ha formen jdbc:postgresql://<vert>[:<port>][/<database>][?<parametre>]" }
            val (server, parametre) = match.destructured
            return "$server/$databasenavn$parametre"
        }

        private fun validerTabellnavn(tabell: String) {
            require(gyldigTabellnavn.matches(tabell)) { "Ugyldig tabellnavn: $tabell" }
        }
    }
}
