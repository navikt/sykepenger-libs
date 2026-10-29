package no.nav.sykepenger.libs.postgres

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseConfigTest {
    @Test
    fun `bruker DATABASE_JDBC_URL når den finnes`() {
        val config = DatabaseConfig.fraEnv(mapOf("DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/mydb?user=u"))
        assertEquals("jdbc:postgresql://localhost:5432/mydb?user=u", config.jdbcUrl)
    }

    @Test
    fun `bygger jdbc-url fra enkeltvariabler når _JDBC_URL mangler`() {
        val env =
            mapOf(
                "DATABASE_HOST" to "localhost",
                "DATABASE_PORT" to "5432",
                "DATABASE_DATABASE" to "mydb",
                "DATABASE_USERNAME" to "u",
                "DATABASE_PASSWORD" to "p",
            )
        assertTrue(DatabaseConfig.fraEnv(env).jdbcUrl.startsWith("jdbc:postgresql://localhost:5432/mydb?"))
    }

    @Test
    fun `annet prefiks`() {
        val config = DatabaseConfig.fraEnv(mapOf("DB_JDBC_URL" to "jdbc:postgresql://localhost:5432/annen"), envVarPrefix = "DB")
        assertEquals("jdbc:postgresql://localhost:5432/annen", config.jdbcUrl)
    }

    @Test
    fun `standardverdier uten DB_POOL_SIZE og NAIS_APP_NAME`() {
        val config = DatabaseConfig.fraEnv(mapOf("DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/mydb"))
        assertEquals(10, config.hikariDefaults.maximumPoolSize)
        assertEquals("hikari", config.poolName)
        assertEquals(listOf("classpath:db/migration"), config.flywayLocations)
    }

    @Test
    fun `DB_POOL_SIZE og NAIS_APP_NAME overstyrer standardverdiene`() {
        val config =
            DatabaseConfig.fraEnv(
                mapOf(
                    "DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/mydb",
                    "DB_POOL_SIZE" to "3",
                    "NAIS_APP_NAME" to "sp-app",
                ),
            )
        assertEquals(3, config.hikariDefaults.maximumPoolSize)
        assertEquals("sp-app", config.poolName)
    }

    @Test
    fun `kaster tydelig feil når databasekonfigurasjon mangler`() {
        assertFailsWith<IllegalArgumentException> { DatabaseConfig.fraEnv(emptyMap()) }
    }
}
