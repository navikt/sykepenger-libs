package no.nav.sykepenger.libs.leaderelection

import java.time.Instant

internal object MiljøvariabelTest {
    @JvmStatic
    fun main(args: Array<String>) {
        NaisLeaderElection().use { abonnement ->
            abonnement.start()
            val frist = Instant.now().plusSeconds(5)
            while (!abonnement.isLeader() && Instant.now() < frist) Thread.sleep(10)
            check(abonnement.isLeader()) { "Miljøvariablene ga ikke riktig leader" }
        }
    }
}
