package com.casero.cu.network

import com.casero.cu.model.CommunicationChannel
import com.casero.cu.model.CompanionRegistration
import com.casero.cu.model.Guest
import com.casero.cu.model.GuestRegistration
import com.casero.cu.model.PersonaValidation
import com.casero.cu.model.PortalProfile
import kotlin.time.Instant

/** Feature boundary used by tests without contacting the real portal. */
interface PortalServing {
    suspend fun listGuests(from: Instant, to: Instant, pageSize: Int = 100, page: Int = 1): List<Guest>
    suspend fun listCompanions(estanciaId: String, pageSize: Int = 100, page: Int = 1): List<Guest>
    suspend fun validatePersonaDiie(identificador: String, tipo: PersonaLookup): List<PersonaValidation>
    suspend fun validatePersonaSuin(identificador: String): List<PersonaValidation>
    suspend fun validateCompanionForStay(identificador: String): List<PersonaValidation>
    suspend fun registerGuests(people: List<GuestRegistration>): List<Guest>
    suspend fun addCompanions(people: List<CompanionRegistration>, stayId: String): List<Guest>
    suspend fun updateCheckOut(stayId: String, companionId: String? = null, checkOut: Instant): String
    suspend fun fetchProfile(): PortalProfile
    suspend fun listCommunicationChannels(): List<CommunicationChannel>
    suspend fun fetchForeignGuestPhoto(passport: String, name: String, nationalityCode: String, sex: String): ByteArray
    suspend fun fetchCompanionPhoto(carne: String): ByteArray
    suspend fun signOut()
}
