package com.casero.cu

import com.casero.cu.model.Guest
import com.casero.cu.model.PersonaValidation
import com.casero.cu.model.PortalDates
import com.casero.cu.model.PortalJson
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant

/** Fabricated people and documents only; see SECURITY.md. */
fun fixtureGuest(id: String = "TEST-A", response: String = "OK"): Guest = Guest.fromJson(
    PortalJson.parseToJsonElement(
        """{"Biografico":"Test Guest","Identificador":"$id","EstanciaId":"$id",""" +
            """"FechaEntrada":"/Date(1772600400000)/","FechaSalida":"/Date(1772773200000)/",""" +
            """"FechaNacimiento":"/Date(-315619200000)/","Respuesta":"$response"}""",
    ),
)

fun fixturePerson(id: String = "TEST-A", birthDate: String? = "/Date(-315619200000)/"): PersonaValidation {
    val birth = birthDate?.let { ""","FechaNac":"$it"""" } ?: ""
    return PersonaValidation.fromJson(
        PortalJson.parseToJsonElement(
            """{"Nombre":"Test","Apellido1":"Guest","Biografico":"Test Guest","Identificador":"$id",""" +
                """"Nacionalidad":"428","Sexo":"F","Respuesta":"OK"$birth}""",
        ),
    )
}

/** 2026-11-21 15:00 Cuba time, the "now" of the date tests. */
val cubaAfternoon: Instant = LocalDateTime(2026, 11, 21, 15, 0).toInstant(PortalDates.timeZone)

fun ms(millis: Long): Instant = Instant.fromEpochMilliseconds(millis)
