package com.casero.cu.model

import com.casero.cu.network.PortalError
import kotlin.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * One guest as returned by `Huespedes/ListarHuespedes`.
 *
 * Portal JSON keys are PascalCase and dates arrive as `/Date(ms)/` strings, so
 * this decodes manually. Foreign guests are identified by passport
 * (`identificador`); nationality arrives as a numeric code plus a display name.
 */
data class Guest(
    val fullName: String,
    val identificador: String,
    val checkIn: Instant? = null,
    val checkOut: Instant? = null,
    val birthDate: Instant? = null,
    val nationalityCode: String? = null,
    val nationality: String? = null,
    val sex: String? = null,
    val stayId: String = "",
    val companionId: String? = null,
    /** Present on `RegistrarHuesped` responses: "OK" or an error message per person. */
    val response: String? = null,
) {
    val id: String get() = "$stayId|$identificador|${checkIn?.epochSeconds ?: 0}"

    /**
     * Checked in and not past check-out day (Cuba time). Same rule as the
     * Guests "Active" segment; the only stays that can get companions added.
     */
    fun isActive(now: Instant = PortalDates.now()): Boolean =
        (checkIn ?: Instant.DISTANT_PAST) <= now &&
            (checkOut ?: Instant.DISTANT_FUTURE) >= PortalDates.startOfDay(now)

    /** Encodes in the portal's own shape, so cached guests decode like fresh ones. */
    fun toJson(): JsonObject = buildJsonObject {
        put(FULL_NAME, fullName)
        put(IDENTIFICADOR, identificador)
        checkIn?.let { put(CHECK_IN, DotNetDate.string(it)) }
        checkOut?.let { put(CHECK_OUT, DotNetDate.string(it)) }
        birthDate?.let { put(BIRTH_DATE, DotNetDate.string(it)) }
        nationalityCode?.let { put(NATIONALITY_CODE, it) }
        nationality?.let { put(NATIONALITY, it) }
        sex?.let { put(SEX, it) }
        put(STAY_ID, stayId)
        companionId?.let { put(COMPANION_ID, it) }
        response?.let { put(RESPONSE, it) }
    }

    companion object {
        private const val FULL_NAME = "Biografico"
        private const val IDENTIFICADOR = "Identificador"
        private const val CHECK_IN = "FechaEntrada"
        private const val CHECK_OUT = "FechaSalida"
        private const val BIRTH_DATE = "FechaNacimiento"
        private const val NATIONALITY_CODE = "Nacionalidad"
        private const val NATIONALITY = "DescNacionalidad"
        private const val SEX = "Sexo"
        private const val STAY_ID = "EstanciaId"
        private const val COMPANION_ID = "Id"
        private const val RESPONSE = "Respuesta"

        /** Throws `InvalidResponse` without a name or identifier. */
        fun fromJson(element: JsonElement): Guest {
            val json = element as? JsonObject ?: throw PortalError.InvalidResponse
            val guest = Guest(
                fullName = json.string(FULL_NAME) ?: "",
                identificador = json.string(IDENTIFICADOR) ?: "",
                checkIn = DotNetDate.parse(json.string(CHECK_IN)),
                checkOut = DotNetDate.parse(json.string(CHECK_OUT)),
                birthDate = DotNetDate.parse(json.string(BIRTH_DATE)),
                nationalityCode = json.string(NATIONALITY_CODE),
                nationality = json.string(NATIONALITY),
                sex = json.string(SEX),
                stayId = json.string(STAY_ID) ?: "",
                companionId = json.text(COMPANION_ID),
                response = json.string(RESPONSE),
            )
            if (guest.identificador.isEmpty() || guest.fullName.isEmpty()) throw PortalError.InvalidResponse
            return guest
        }

        /** `RegistrarHuesped` returns a bare object for one person and an array for several. */
        fun listFromJson(element: JsonElement): List<Guest> =
            when (element) {
                is JsonArray -> element.map(::fromJson)
                is JsonObject -> listOf(fromJson(element))
                is JsonPrimitive -> throw PortalError.InvalidResponse
            }
    }
}

/** Envelope of `Huespedes/ListarHuespedes` and `Huespedes/ListarAcompannantes`. */
data class GuestListResponse(val page: Int, val total: Int, val guests: List<Guest>) {
    companion object {
        /** Throws `Rejected` when the portal reports an error, `InvalidResponse` without the list. */
        fun fromJson(element: JsonElement?, listKey: String = "ListaHuespedes"): GuestListResponse {
            val json = element as? JsonObject ?: throw PortalError.InvalidResponse
            val error = json.string("Error") ?: ""
            if (error.isNotEmpty()) throw PortalError.Rejected(error)
            val list = json.array(listKey) ?: throw PortalError.InvalidResponse
            return GuestListResponse(
                page = json.int("Pagina") ?: 1,
                total = json.int("Total") ?: 0,
                guests = list.map(Guest::fromJson),
            )
        }
    }
}
