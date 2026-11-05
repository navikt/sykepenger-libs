package no.nav.sykepenger.libs.leaderelection

fun interface LeaderElection {
    fun isLeader(): Boolean
}
