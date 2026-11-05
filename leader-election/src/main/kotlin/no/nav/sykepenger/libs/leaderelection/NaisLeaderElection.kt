package no.nav.sykepenger.libs.leaderelection

import no.nav.sykepenger.libs.logging.loggError
import no.nav.sykepenger.libs.logging.loggInfo
import no.nav.sykepenger.libs.logging.loggWarn
import tools.jackson.core.JacksonException
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class NaisLeaderElection(
    electorSseUrl: String = requireNotNull(System.getenv("ELECTOR_SSE_URL")) { "ELECTOR_SSE_URL mangler" },
    private val hostname: String = requireNotNull(System.getenv("HOSTNAME")) { "HOSTNAME mangler" },
    private val ventetidFørNyTilkobling: Duration = 5.seconds,
) : LeaderElection,
    AutoCloseable {
    init {
        require(hostname.isNotBlank()) { "HOSTNAME kan ikke være tomt" }
        require(ventetidFørNyTilkobling.isFinite() && ventetidFørNyTilkobling.inWholeMilliseconds >= 1) {
            "Ventetid før ny tilkobling må være endelig og minst ett millisekund"
        }
    }

    private val objectMapper = jacksonObjectMapper()
    private val request =
        HttpRequest
            .newBuilder(URI(electorSseUrl))
            .header("Accept", "text/event-stream")
            .GET()
            .build()
    private val httpClient = HttpClient.newBuilder().connectTimeout(1.seconds.toJavaDuration()).build()
    private var jobb: Thread? = null

    @Volatile
    private var leader: String? = null

    @Volatile
    private var stoppet = false

    @Volatile
    private var strøm: InputStream? = null

    override fun isLeader(): Boolean = !stoppet && leader == hostname

    @Synchronized
    fun start() {
        check(!stoppet) { "Leader election er stoppet" }
        if (jobb != null) return
        jobb = thread(name = "leader-election", isDaemon = true) { lyttPåElector() }
    }

    private fun lyttPåElector() {
        var feiledeTilkoblingerPåRad = 0
        while (!stoppet) {
            try {
                lesStrømmenTilDenLukkes()
                feiledeTilkoblingerPåRad = 0
                if (!stoppet) loggInfo("Elector lukket SSE-strømmen. Kobler til på nytt")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: IOException) {
                if (stoppet) return
                loggTilkoblingsfeil(++feiledeTilkoblingerPåRad, e)
            } catch (e: JacksonException) {
                if (stoppet) return
                loggTilkoblingsfeil(++feiledeTilkoblingerPåRad, e)
            }
            try {
                Thread.sleep(ventetidFørNyTilkobling.inWholeMilliseconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    private fun loggTilkoblingsfeil(
        antall: Int,
        feil: Exception,
    ) {
        if (antall >= 12) {
            loggError("Har ikke klart å lytte på leader-endringer fra elector på en stund", feil)
        } else {
            loggWarn("Mistet eller fikk ikke kontakt med elector. Prøver igjen", feil)
        }
    }

    @Synchronized
    override fun close() {
        stoppet = true
        httpClient.shutdownNow()
        try {
            strøm?.close()
        } finally {
            jobb?.interrupt()
            jobb?.join()
        }
    }

    private fun lesStrømmenTilDenLukkes() {
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { body ->
            strøm = body
            try {
                if (response.statusCode() != 200) throw IOException("Fikk HTTP ${response.statusCode()} fra elector")
                if (stoppet) return
                body.bufferedReader().useLines { linjer ->
                    val data = StringBuilder()
                    linjer.forEach { linje ->
                        when {
                            linje.isEmpty() && data.isNotEmpty() -> {
                                oppdaterLeader(data.toString())
                                data.clear()
                            }
                            linje.startsWith("data:") -> {
                                data.append(linje.removePrefix("data:").removePrefix(" ")).append('\n')
                            }
                        }
                    }
                }
            } finally {
                strøm = null
            }
        }
    }

    private fun oppdaterLeader(data: String) {
        val nyLeader =
            objectMapper
                .readTree(data)
                .path("name")
                .asString()
                .ifBlank { null }
        if (nyLeader != leader) {
            loggInfo("Leader er endret", "leader" to nyLeader, "erLeader" to (nyLeader == hostname).toString())
        }
        // Elector velger leader til poden slettes, så siste kjente leader gjelder også ved brudd i SSE-strømmen.
        leader = nyLeader
    }
}
