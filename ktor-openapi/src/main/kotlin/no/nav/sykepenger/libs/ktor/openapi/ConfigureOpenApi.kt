package no.nav.sykepenger.libs.ktor.openapi

import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.OperationDescribeAttributeKey
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * Serverer OpenAPI-specen på `/api/openapi.json` og Swagger UI på `/api/swagger`.
 *
 * Specen bygges fra rutetreet, men bare ruter som er beskrevet med `.describe {}` kommer med. Da holdes helsesjekker,
 * metrikker og andre interne ruter ute uten at hver app må skjule dem.
 */
@OptIn(ExperimentalKtorApi::class)
fun Application.configureOpenApi(
    tittel: String,
    versjon: String,
) {
    val info = OpenApiInfo(title = tittel, version = versjon)
    val kilde =
        OpenApiDocSource.Routing(
            schemaInference = JacksonJsonSchemaInference,
            routes = { routingRoot.descendants().filter(Route::erBeskrevet) },
        )
    routing {
        get("/api/openapi.json") {
            val spec = checkNotNull(kilde.read(this@configureOpenApi, OpenApiDoc(info = info)))
            call.respondText(spec.content, ContentType.Application.Json)
        }.hide()
        swaggerUI("/api/swagger") {
            this.info = info
            source = kilde
            remotePath = "openapi.json"
        }
    }
}

@OptIn(ExperimentalKtorApi::class)
private fun Route.erBeskrevet(): Boolean = generateSequence(this) { it.parent }.any { OperationDescribeAttributeKey in it.attributes }
