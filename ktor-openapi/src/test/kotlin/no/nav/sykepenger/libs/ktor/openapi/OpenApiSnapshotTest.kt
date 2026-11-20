package no.nav.sykepenger.libs.ktor.openapi

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenApiSnapshotTest {
    @TempDir
    lateinit var mappe: Path

    private val snapshot get() = mappe.resolve("openapi/app.json")

    @AfterEach
    fun ryddOpp() {
        System.clearProperty(OPPDATER_OPENAPI_SNAPSHOT)
    }

    @Test
    fun `godtar en spec som er lik snapshoten, uavhengig av formatering`() {
        Files.createDirectories(snapshot.parent)
        Files.writeString(snapshot, "{\n  \"openapi\" : \"3.1.1\"\n}\n")

        assertOpenApiSnapshot("""{"openapi":"3.1.1"}""", snapshot)
    }

    @Test
    fun `feiler og forteller hvordan snapshoten oppdateres, når specen er endret`() {
        Files.createDirectories(snapshot.parent)
        Files.writeString(snapshot, """{"openapi":"3.1.0"}""")

        val feil = assertFailsWith<AssertionError> { assertOpenApiSnapshot("""{"openapi":"3.1.1"}""", snapshot) }

        assertTrue(feil.message!!.contains("$OPPDATER_OPENAPI_SNAPSHOT_ENV=true"))
    }

    @Test
    fun `feiler når snapshoten mangler`() {
        assertFailsWith<AssertionError> { assertOpenApiSnapshot("""{"openapi":"3.1.1"}""", snapshot) }
    }

    @Test
    fun `skriver specen formatert til snapshoten når oppdatering er slått på`() {
        System.setProperty(OPPDATER_OPENAPI_SNAPSHOT, "true")

        assertOpenApiSnapshot("""{"openapi":"3.1.1"}""", snapshot)

        assertEquals("{\n  \"openapi\" : \"3.1.1\"\n}\n", Files.readString(snapshot))
    }
}
