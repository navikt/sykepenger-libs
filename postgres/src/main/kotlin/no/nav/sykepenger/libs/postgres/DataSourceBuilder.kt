package no.nav.sykepenger.libs.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun DatabaseConfig.dataSource(): HikariDataSource {
    val defaults = hikariDefaults
    return HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = this@dataSource.jdbcUrl
            maximumPoolSize = defaults.maximumPoolSize
            minimumIdle = defaults.minimumIdle
            idleTimeout = defaults.idleTimeout.toMillis()
            maxLifetime = defaults.maxLifetime.toMillis()
            connectionTimeout = defaults.connectionTimeout.toMillis()
            initializationFailTimeout = defaults.initializationFailTimeout.toMillis()
            leakDetectionThreshold = defaults.leakDetectionThreshold.toMillis()
            poolName = this@dataSource.poolName
        },
    )
}
