package com.casero.cu.model

import com.casero.cu.network.PortalError
import kotlin.time.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Response of `Huespedes/ValidarPersonaDiie` (foreign guests, DIIE registry) and
 * `Huespedes/ValidarPersonaSuin` (Cuban companions, SUIN/carné registry).
 */
data class PersonaValidation(
    val idPersona: String? = null,
    val identificador: String = "",
    val firstName: String = "",
    val middleName: String? = null,
    val lastName1: String? = null,
    val lastName2: String? = null,
    val fullName: String = "",
    val sex: String? = null,
    val birthDate: Instant? = null,
    val nationalityCode: String? = null,
    val nationality: String? = null,
    /** Present for DIIE lookups: e.g. "Entrada" if the person is currently checked in elsewhere. */
    val status: String? = null,
    val response: String? = null,
) {
    companion object {
        fun fromJson(element: JsonElement): PersonaValidation {
            val json = element as? JsonObject ?: throw PortalError.InvalidResponse
            return PersonaValidation(
                idPersona = json.text("IdPersona"),
                identificador = json.string("Identificador") ?: "",
                firstName = json.string("Nombre") ?: "",
                middleName = json.string("Nombre2"),
                lastName1 = json.string("Apellido1"),
                lastName2 = json.string("Apellido2"),
                fullName = json.string("Biografico") ?: "",
                sex = json.string("Sexo"),
                birthDate = DotNetDate.parse(json.string("FechaNac")),
                nationalityCode = json.text("Nacionalidad"),
                nationality = json.string("DescNacionalidad"),
                status = json.string("Estado"),
                response = json.string("Respuesta"),
            )
        }
    }
}

/**
 * A person queued for `Huespedes/RegistrarHuesped`, built from a validated
 * `PersonaValidation` plus the stay's dates.
 *
 * Portal wire format (one entry, `|`-joined for multiple), confirmed against a
 * browser capture:
 * `NOMBRE*NOMBRE2*APELLIDO1*APELLIDO2,IDENTIFICADOR,NACIONALIDAD,SEXO,dd/MM/yy(nac),dd/MM/yy(entrada),dd/MM/yy(salida),PROCEDENCIA`
 * An empty second name leaves `**` (`JUAN**PEREZ*GOMEZ`).
 */
data class GuestRegistration(
    val firstName: String,
    /** Second given name (`Nombre2`); empty when the person has none. */
    val middleName: String = "",
    val lastName1: String,
    val lastName2: String,
    val identificador: String,
    val nationalityCode: String,
    val sex: String,
    val birthDate: Instant,
    val checkIn: Instant,
    val checkOut: Instant,
    /**
     * Country/origin code. The captured traffic always sends this equal to
     * `nationalityCode`; kept separate in case the portal expects a distinct
     * "coming from" code for connecting itineraries — unverified.
     */
    val procedenciaCode: String,
    /** Display only (queue card); never sent to the portal. */
    val nationalityName: String? = null,
    /**
     * Found through the CI lookup (`ValidarPersonaSuin`), i.e. a Cuban companion.
     * Not derived from nationality: Cubans living abroad are found by passport
     * and register as main guests. Never sent to the portal.
     */
    val isCompanion: Boolean = false,
) {
    /** Encodes one `personas` segment: the four name parts joined by `*`. */
    val wireFormat: String
        get() = listOf(
            "$firstName*$middleName*$lastName1*$lastName2",
            identificador,
            nationalityCode,
            sex,
            PortalDates.portalFormat(birthDate),
            PortalDates.portalFormat(checkIn),
            PortalDates.portalFormat(checkOut),
            procedenciaCode,
        ).joinToString(",")
}

/**
 * A Cuban companion added to an existing stay through
 * `Huespedes/RegistrarAcompannantes` (confirmed against a browser capture).
 *
 * Unlike `GuestRegistration`: the full name goes with spaces, and there is no
 * origin field. One entry (`|`-joined for several, assumed):
 * `FULL NAME,CI,NACIONALIDAD,SEXO,dd/MM/yy(birth),dd/MM/yy(check-in),dd/MM/yy(check-out)`
 */
data class CompanionRegistration(
    val fullName: String,
    val identificador: String,
    val nationalityCode: String,
    val sex: String,
    val birthDate: Instant,
    val checkIn: Instant,
    val checkOut: Instant,
) {
    val wireFormat: String
        get() = listOf(
            fullName.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" "),
            identificador,
            nationalityCode,
            sex,
            PortalDates.portalFormat(birthDate),
            PortalDates.portalFormat(checkIn),
            PortalDates.portalFormat(checkOut),
        ).joinToString(",")
}
