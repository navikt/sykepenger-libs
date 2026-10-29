package no.nav.sykepenger.libs.ktor.auth

data class SaksbehandlerPrincipal(
    val saksbehandler: Saksbehandler,
    val tilganger: Set<Tilgang>,
    val accessToken: AccessToken,
) {
    fun harTilgang(tilgang: Tilgang) = tilgang in tilganger
}
