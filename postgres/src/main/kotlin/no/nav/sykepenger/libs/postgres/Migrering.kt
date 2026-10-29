package no.nav.sykepenger.libs.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

/**
 * Kjører Flyway-migreringene med en egen, liten pool som lukkes etterpå. Venter på migreringslåsen så lenge
 * det trengs, slik at flere poder som starter samtidig ikke migrerer i parallell.
 */
fun migrerSynkront(config: DatabaseConfig) {
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            maximumPoolSize = 2
            minimumIdle = 1
            poolName = "${config.poolName}-migrering"
        },
    ).use { migreringsDataSource ->
        Flyway
            .configure()
            .dataSource(migreringsDataSource)
            .locations(*config.flywayLocations.toTypedArray())
            .cleanDisabled(true)
            .lockRetryCount(-1)
            .validateMigrationNaming(true)
            .load()
            .migrate()
    }
}
