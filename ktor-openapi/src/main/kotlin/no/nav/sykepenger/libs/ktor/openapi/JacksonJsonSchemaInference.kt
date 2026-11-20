package no.nav.sykepenger.libs.ktor.openapi

import io.ktor.openapi.AdditionalProperties
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonSchemaInference
import io.ktor.openapi.JsonType
import io.ktor.openapi.ReferenceOr
import io.ktor.openapi.reflect.ReflectionJsonSchemaInference
import io.ktor.openapi.reflect.SchemaReflectionAdapter
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

/**
 * Lager skjema med refleksjon over Kotlin-klassene, slik at DTO-ene ikke trenger `@Serializable` når Jackson
 * serialiserer dem. Refleksjonen i Ktor kjenner ikke alle typene Jackson skriver som tall eller tekst, så de
 * byttes ut med skjemaet som passer med det Jackson skriver. Refleksjonen merker heller ikke tall og
 * sannhetsverdier som kan være null, så felter som ikke er påkrevd, får null som lovlig type.
 */
internal object JacksonJsonSchemaInference : JsonSchemaInference {
    private val skalarer: Map<String, JsonSchema> =
        mapOf(
            BigDecimal::class to JsonSchema(type = JsonType.NUMBER),
            YearMonth::class to JsonSchema(type = JsonType.STRING, pattern = "^\\d{4}-\\d{2}$"),
            UUID::class to JsonSchema(type = JsonType.STRING, format = "uuid"),
        ).mapKeys { (klasse, _) -> checkNotNull(klasse.qualifiedName) }

    private val refleksjon =
        ReflectionJsonSchemaInference(
            object : SchemaReflectionAdapter {
                override fun <T : Any> getProperties(kClass: KClass<T>): Collection<KProperty1<T, *>> = if (kClass.qualifiedName in skalarer) emptyList() else super.getProperties(kClass)
            },
        )

    override fun buildSchema(type: KType): JsonSchema = refleksjon.buildSchema(type).medSkalarer()

    private fun JsonSchema.medSkalarer(): JsonSchema {
        skalarer[title]?.let { skalar ->
            // Refleksjonen gir skalaren typen object, eller object og null når feltet kan være null.
            if (type == JsonType.OBJECT) return skalar
            return skalar.copy(type = JsonSchema.SchemaType.AnyOf(listOfNotNull(skalar.type as? JsonType, JsonType.NULL)))
        }
        return copy(
            properties =
                properties?.mapValues { (navn, skjema) ->
                    skjema.medSkalarer().let { if (required.orEmpty().contains(navn)) it else it.kanVæreNull() }
                },
            items = items?.medSkalarer(),
            oneOf = oneOf?.map { it.medSkalarer() },
            anyOf = anyOf?.map { it.medSkalarer() },
            allOf = allOf?.map { it.medSkalarer() },
            additionalProperties =
                when (val ekstra = additionalProperties) {
                    is AdditionalProperties.PSchema -> AdditionalProperties.PSchema(ekstra.value.medSkalarer())
                    else -> ekstra
                },
        )
    }

    private fun ReferenceOr<JsonSchema>.kanVæreNull(): ReferenceOr<JsonSchema> {
        if (this !is ReferenceOr.Value) return this
        val enkelType = value.type as? JsonType ?: return this
        if (enkelType == JsonType.NULL) return this
        return ReferenceOr.Value(value.copy(type = JsonSchema.SchemaType.AnyOf(listOf(enkelType, JsonType.NULL))))
    }

    private fun ReferenceOr<JsonSchema>.medSkalarer(): ReferenceOr<JsonSchema> =
        when (this) {
            is ReferenceOr.Value -> ReferenceOr.Value(value.medSkalarer())
            is ReferenceOr.Reference -> this
        }
}
