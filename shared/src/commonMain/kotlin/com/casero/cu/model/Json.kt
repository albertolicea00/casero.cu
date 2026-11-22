package com.casero.cu.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** Lenient readers for the portal's hand-rolled JSON. */
internal val PortalJson = Json { ignoreUnknownKeys = true }

internal fun parseJsonOrNull(text: String): JsonElement? =
    try {
        PortalJson.parseToJsonElement(text)
    } catch (_: Exception) {
        null
    }

internal fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

/** A string or a number, as text. */
internal fun JsonObject.text(key: String): String? {
    val value = this[key] as? JsonPrimitive ?: return null
    if (value is JsonNull) return null
    return value.contentOrNull
}

internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

internal fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

internal fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray
