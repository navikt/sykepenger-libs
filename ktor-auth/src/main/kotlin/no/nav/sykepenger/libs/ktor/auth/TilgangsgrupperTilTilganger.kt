package no.nav.sykepenger.libs.ktor.auth

class TilgangsgrupperTilTilganger(
    private val tilgangLesGruppeIder: Set<String>,
    private val tilgangSkrivGruppeIder: Set<String>,
) {
    /**
     * Skrivetilgang gir også lesetilgang: en saksbehandler i skrivegruppen er ikke nødvendigvis
     * medlem av lesegruppen i Entra, og ville ellers fått 403 på alle endepunkter som krever [Tilgang.Les].
     */
    fun tilganger(entraGrupper: Set<String>): Set<Tilgang> =
        buildSet {
            if (tilgangSkrivGruppeIder.any { it in entraGrupper }) {
                add(Tilgang.Skriv)
                add(Tilgang.Les)
            }
            if (tilgangLesGruppeIder.any { it in entraGrupper }) add(Tilgang.Les)
        }

    companion object {
        /** `TILGANG_LES` og `TILGANG_SKRIV` kan inneholde én eller flere kommaseparerte Entra-gruppe-ID-er. */
        fun fraEnv(env: Map<String, String> = System.getenv()) =
            TilgangsgrupperTilTilganger(
                tilgangLesGruppeIder = grupperFra(env.getValue("TILGANG_LES")),
                tilgangSkrivGruppeIder = grupperFra(env.getValue("TILGANG_SKRIV")),
            )

        private fun grupperFra(verdi: String): Set<String> =
            verdi
                .split(",")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
    }
}
