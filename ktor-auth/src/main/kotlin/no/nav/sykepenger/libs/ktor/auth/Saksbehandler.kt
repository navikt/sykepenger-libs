package no.nav.sykepenger.libs.ktor.auth

@JvmInline
value class NavIdent(
    val value: String,
)

@JvmInline
value class SaksbehandlerOid(
    val value: String,
)

data class Saksbehandler(
    val navIdent: NavIdent,
    val oid: SaksbehandlerOid,
    val navn: String,
)

/** Rå JWT-bearer-token til innlogget saksbehandler. Brukes videre til OBO-tokenveksling (Texas) og skal aldri logges. */
@JvmInline
value class AccessToken(
    val value: String,
) {
    override fun toString() = "AccessToken(***)"
}
