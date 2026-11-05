package no.nav.sykepenger.libs.outbox

import java.sql.Connection
import java.util.UUID

object Outbox {
    fun leggTil(
        connection: Connection,
        fødselsnummer: String,
        melding: String,
    ) {
        connection
            .prepareStatement("INSERT INTO outbox (id, fodselsnummer, melding) VALUES (?, ?, cast(? as jsonb))")
            .use { statement ->
                statement.setObject(1, UUID.randomUUID())
                statement.setString(2, fødselsnummer)
                statement.setString(3, melding)
                statement.executeUpdate()
            }
    }
}
