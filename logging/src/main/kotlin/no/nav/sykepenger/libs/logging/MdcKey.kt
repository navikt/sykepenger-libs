package no.nav.sykepenger.libs.logging

/**
 * Felles sett med nøkler som kan settes på MDC av sykepengetjenestene.
 *
 * Fordi alle tjenestene henter nøklene herfra, heter MDC-feltene det samme overalt, og et søk i Team Logs korrelerer
 * på tvers av tjenestene. Enumen er derfor et supersett av alt som kan settes på MDC-en av hver enkelt tjeneste,
 * og inneholder også ID-er som bare hører til ett domene eller system.
 *
 * [value] er navnet feltet får i loggen, og bør ikke endres når det først er tatt i bruk. Tvetydighet må unngås, enten
 * ved å kvalifisere (prefikse/suffikse) navnet med mer domenekontekst, eller ved å sette systemet som genererte ID-en
 * foran: [SPLEIS_BEHANDLING_ID] og [SPESIALIST_BEHANDLING_UNIK_ID] er eksempel på sistnevnte.
 */
enum class MdcKey(
    val value: String,
) {
    COMMAND_CONTEXT_ID("commandContextId"),
    FORSIKRINGSVURDERING_ID("forsikringsvurderingId"),
    IDENTITETSNUMMER("identitetsnummer"),
    MELDING_ID("meldingId"),
    MELDINGNAVN("meldingnavn"),
    OPPGAVE_ID("oppgaveId"),
    OPPRINNELIG_MELDING_ID("opprinneligMeldingId"),
    PERSON_PSEUDO_ID("personPseudoId"),
    REQUEST_METHOD("request.method"),
    REQUEST_URI("request.uri"),
    SAKSBEHANDLER_IDENT("saksbehandlerIdent"),
    SPESIALIST_BEHANDLING_UNIK_ID("spesialistBehandlingUnikId"),
    SPLEIS_BEHANDLING_ID("spleisBehandlingId"),
    VEDTAKSPERIODE_ID("vedtaksperiodeId"),
}
