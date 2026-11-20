package no.nav.sykepenger.libs.ktor.openapi

import tools.jackson.databind.SerializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

const val OPPDATER_OPENAPI_SNAPSHOT = "oppdaterOpenApiSnapshot"
const val OPPDATER_OPENAPI_SNAPSHOT_ENV = "OPPDATER_OPENAPI_SNAPSHOT"

/**
 * Sammenligner specen med snapshoten (ADR 0017), slik at hver kontraktendring blir en synlig diff i PR-en.
 * Med miljøvariabelen `OPPDATER_OPENAPI_SNAPSHOT=true` (eller system property `oppdaterOpenApiSnapshot=true`)
 * skrives specen til snapshoten i stedet. Gradle sender ikke `-D` videre til test-JVM-en, men miljøvariabler kommer fram.
 */
fun assertOpenApiSnapshot(
    spec: String,
    snapshot: Path,
) {
    val formatert = mapper.writeValueAsString(mapper.readTree(spec)) + "\n"
    val oppdater = "Kjør testen med $OPPDATER_OPENAPI_SNAPSHOT_ENV=true for å oppdatere $snapshot."

    if (System.getProperty(OPPDATER_OPENAPI_SNAPSHOT).toBoolean() || System.getenv(OPPDATER_OPENAPI_SNAPSHOT_ENV).toBoolean()) {
        Files.createDirectories(snapshot.parent)
        Files.writeString(snapshot, formatert)
    }

    assertTrue(Files.exists(snapshot), "Fant ikke $snapshot. $oppdater")
    val lagret = mapper.writeValueAsString(mapper.readTree(Files.readString(snapshot))) + "\n"
    assertEquals(lagret, formatert, "OpenAPI-specen er endret. Se over endringen. $oppdater")
}

private val mapper = jacksonMapperBuilder().enable(SerializationFeature.INDENT_OUTPUT).build()
