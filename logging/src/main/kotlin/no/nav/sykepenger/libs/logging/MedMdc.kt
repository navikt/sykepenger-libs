package no.nav.sykepenger.libs.logging

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

fun <T> medMdc(
    vararg mdcKeyValues: Pair<MdcKey, String?>,
    block: () -> T,
): T {
    val contextMap = MDC.getCopyOfContextMap().orEmpty()
    try {
        MDC.setContextMap(contextMap.withMdcKeyValues(mdcKeyValues))
        return block()
    } finally {
        MDC.setContextMap(contextMap)
    }
}

suspend fun <T> coMedMdc(
    vararg mdcKeyValues: Pair<MdcKey, String?>,
    block: suspend () -> T,
): T =
    withContext(MDCContext(MDC.getCopyOfContextMap().withMdcKeyValues(mdcKeyValues))) {
        block()
    }

private fun Map<String, String>?.withMdcKeyValues(pairs: Array<out Pair<MdcKey, String?>>): Map<String, String> {
    val removedKeys = pairs.filter { it.second == null }.map { it.first.value }.toSet()
    val changedKeyValues = pairs.filter { it.second != null }.map { it.first.value to it.second!! }

    return orEmpty()
        .filterNot { (key, _) -> key in removedKeys }
        .plus(changedKeyValues)
}
