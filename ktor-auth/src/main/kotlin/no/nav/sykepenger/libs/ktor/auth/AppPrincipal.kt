package no.nav.sykepenger.libs.ktor.auth

/** Klient-ID-en (`azp`) til appen som kaller. */
@JvmInline
value class KlientId(
    val value: String,
)

/** En app som kaller med et maskin-til-maskin-token (client credentials). */
data class AppPrincipal(
    val klientId: KlientId,
)
