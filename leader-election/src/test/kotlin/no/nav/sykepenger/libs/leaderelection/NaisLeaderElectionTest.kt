package no.nav.sykepenger.libs.leaderelection

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertTimeout
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Timeout(10)
internal class NaisLeaderElectionTest {
    private val hostname = "test-pod"

    @Test
    fun `er leader først når elector sender vårt hostname`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname).use { abonnement ->
                val leaderElection: LeaderElection = abonnement
                assertFalse(leaderElection.isLeader())

                elector.send("""data: {"name":"$hostname"}""" + "\n\n")
                abonnement.start()

                ventTil { leaderElection.isLeader() }
            }
        }
    }

    @Test
    fun `leser URL og hostname fra miljøvariablene`() {
        TestElector().use { elector ->
            elector.send("""data: {"name":"$hostname"}""" + "\n\n")
            val prosess =
                ProcessBuilder(
                    Path(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp",
                    System.getProperty("java.class.path"),
                    MiljøvariabelTest::class.java.name,
                ).apply {
                    environment()["ELECTOR_SSE_URL"] = elector.url
                    environment()["HOSTNAME"] = hostname
                    redirectErrorStream(true)
                }.start()
            try {
                assertTrue(prosess.waitFor(8, TimeUnit.SECONDS), "Testprosessen stoppet ikke")
                assertEquals(0, prosess.exitValue(), prosess.inputStream.bufferedReader().readText())
            } finally {
                prosess.destroyForcibly()
            }
        }
    }

    @Test
    fun `leser hele SSE-hendelsen og ignorerer kommentarer og andre felt`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname).use { abonnement ->
                elector.send(
                    ": heartbeat\n" +
                        "event: leader\n" +
                        "id: 1\n" +
                        "data: {\"name\":\n" +
                        "data: \"$hostname\"}\n\n",
                )
                abonnement.start()

                ventTil { abonnement.isLeader() }
            }
        }
    }

    @Test
    fun `beholder leader ved brudd og følger ny leader etter ny tilkobling`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname, 10.milliseconds).use { abonnement ->
                val førsteSvar = elector.send("""data: {"name":"$hostname"}""" + "\n\n")
                abonnement.start()
                ventTil { abonnement.isLeader() }

                val feilsvar = elector.send("", statuskode = 503)
                førsteSvar.lukk.countDown()
                feilsvar.ventTilSendt()
                assertTrue(abonnement.isLeader())

                elector.send("""data: {"name":"en-annen-pod"}""" + "\n\n")
                feilsvar.lukk.countDown()

                ventTil { !abonnement.isLeader() }
            }
        }
    }

    @Test
    fun `er ikke leader når elector ikke har valgt noen`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname, 10.milliseconds).use { abonnement ->
                val førsteSvar = elector.send("""data: {"name":"$hostname"}""" + "\n\n")
                abonnement.start()
                ventTil { abonnement.isLeader() }

                elector.send("data: {}\n\n")
                førsteSvar.lukk.countDown()

                ventTil { !abonnement.isLeader() }
            }
        }
    }

    @Test
    fun `beholder leader ved ugyldig JSON og prøver på nytt`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname, 10.milliseconds).use { abonnement ->
                val førsteSvar = elector.send("""data: {"name":"$hostname"}""" + "\n\n")
                abonnement.start()
                ventTil { abonnement.isLeader() }

                val ugyldigSvar = elector.send("data: ugyldig JSON\n\n")
                førsteSvar.lukk.countDown()
                ugyldigSvar.ventTilSendt()
                assertTrue(abonnement.isLeader())
                elector.send("""data: {"name":"en-annen-pod"}""" + "\n\n")

                ventTil { !abonnement.isLeader() }
            }
        }
    }

    @Test
    fun `prøver igjen etter HTTP-feil før første valg`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname, 10.milliseconds).use { abonnement ->
                elector.send("", statuskode = 503)
                elector.send("""data: {"name":"$hostname"}""" + "\n\n")
                abonnement.start()

                ventTil { abonnement.isLeader() }
            }
        }
    }

    @Test
    fun `stopper mens SSE-strømmen er åpen og kan ikke startes igjen`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname).use { abonnement ->
                elector.send("""data: {"name":"$hostname"}""" + "\n\n")
                abonnement.start()
                abonnement.start()
                ventTil { abonnement.isLeader() }

                assertTimeout(java.time.Duration.ofSeconds(1)) { abonnement.close() }

                assertFalse(abonnement.isLeader())
                abonnement.close()
                assertFailsWith<IllegalStateException> { abonnement.start() }
            }
        }
    }

    @Test
    fun `stopper mens elector ennå ikke har sendt HTTP-headere`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname).use { abonnement ->
                abonnement.start()
                elector.ventTilTilkoblet()

                assertTimeout(java.time.Duration.ofSeconds(1)) { abonnement.close() }

                assertFalse(abonnement.isLeader())
            }
        }
    }

    @Test
    fun `stopper uten å vente på neste tilkobling`() {
        TestElector().use { elector ->
            NaisLeaderElection(elector.url, hostname, 30.seconds).use { abonnement ->
                elector.send("""data: {"name":"$hostname"}""" + "\n\n", holdÅpen = false)
                abonnement.start()
                ventTil { abonnement.isLeader() }

                assertTimeout(java.time.Duration.ofSeconds(1)) { abonnement.close() }

                assertFalse(abonnement.isLeader())
            }
        }
    }

    @Test
    fun `avviser ventetid som ikke er minst ett millisekund eller ikke er endelig`() {
        for (ventetid in listOf(Duration.ZERO, -1.milliseconds, 1.nanoseconds, Duration.INFINITE)) {
            assertFailsWith<IllegalArgumentException> {
                NaisLeaderElection("http://localhost/sse", hostname, ventetid).use {}
            }
        }
    }

    @Test
    fun `avviser tomt hostname`() {
        assertFailsWith<IllegalArgumentException> {
            NaisLeaderElection("http://localhost/sse", " ").use {}
        }
    }

    private fun ventTil(betingelse: () -> Boolean) {
        val frist = Instant.now().plusSeconds(5)
        while (!betingelse() && Instant.now() < frist) Thread.sleep(10)
        assertTrue(betingelse(), "Ventet for lenge på leader-endring")
    }
}
