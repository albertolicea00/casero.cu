package com.casero.cu.model

import com.casero.cu.network.PortalError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** One entry of `ViasComunicacion/ListarViasComunicacion`: `{ "Tipo": "sms", "Valor": "+535XXXXXXX" }`. */
data class CommunicationChannel(val kind: String, val value: String) {
    /**
     * Phones come back as `Tipo: "sms"`. The e-mail `Tipo` was never captured, so
     * anything else is treated as an e-mail address.
     */
    val isPhone: Boolean get() = kind.lowercase() == "sms"

    companion object {
        fun listFromJson(element: JsonElement?): List<CommunicationChannel> {
            val array = element as? JsonArray ?: throw PortalError.InvalidResponse
            return array.map {
                val json = it as? JsonObject ?: throw PortalError.InvalidResponse
                CommunicationChannel(
                    kind = json.string("Tipo") ?: throw PortalError.InvalidResponse,
                    value = json.string("Valor") ?: throw PortalError.InvalidResponse,
                )
            }
        }
    }
}
