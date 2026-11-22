package com.casero.cu

import com.casero.cu.feature.register.RegisterGuestViewModel
import com.casero.cu.model.CommunicationChannel
import com.casero.cu.model.CompanionRegistration
import com.casero.cu.model.Guest
import com.casero.cu.model.GuestRegistration
import com.casero.cu.model.PersonaValidation
import com.casero.cu.model.PortalJson
import com.casero.cu.model.PortalProfile
import com.casero.cu.network.PersonaLookup
import com.casero.cu.network.PortalError
import com.casero.cu.network.PortalServing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class StubPortalClient : PortalServing {
    var person: PersonaValidation? = null
    var results: List<Guest> = emptyList()
    var submitError: Exception? = null
    var submissions = 0

    override suspend fun validatePersonaDiie(identificador: String, tipo: PersonaLookup) = listOfNotNull(person)
    override suspend fun validatePersonaSuin(identificador: String) = listOfNotNull(person)
    override suspend fun validateCompanionForStay(identificador: String) = listOfNotNull(person)
    override suspend fun registerGuests(people: List<GuestRegistration>): List<Guest> {
        submissions += 1
        submitError?.let { throw it }
        return results
    }
    override suspend fun listGuests(from: Instant, to: Instant, pageSize: Int, page: Int) = results
    override suspend fun listCompanions(estanciaId: String, pageSize: Int, page: Int) = emptyList<Guest>()
    override suspend fun addCompanions(people: List<CompanionRegistration>, stayId: String) = emptyList<Guest>()
    override suspend fun updateCheckOut(stayId: String, companionId: String?, checkOut: Instant) = ""
    override suspend fun fetchProfile() = PortalProfile()
    override suspend fun listCommunicationChannels() = emptyList<CommunicationChannel>()
    override suspend fun fetchForeignGuestPhoto(passport: String, name: String, nationalityCode: String, sex: String) = ByteArray(0)
    override suspend fun fetchCompanionPhoto(carne: String) = ByteArray(0)
    override suspend fun signOut() {}
}

class RegistrationTest {
    private fun model(client: StubPortalClient) = RegisterGuestViewModel(client) { cubaAfternoon }

    @Test
    fun missingBirthDateCannotBeQueued() = runTest {
        val client = StubPortalClient().apply { person = fixturePerson(birthDate = null) }
        val model = model(client)
        model.identificador = "TEST-A"
        model.lookUp()
        assertFalse(model.canAdd)
        assertNotNull(model.validationMessage)
        model.addToQueue()
        assertTrue(model.queue.isEmpty())
    }

    @Test
    fun duplicateQueueAndInvalidDatesAreRejected() = runTest {
        val client = StubPortalClient().apply { person = fixturePerson() }
        val model = model(client)
        model.identificador = "TEST-A"
        model.lookUp()
        model.checkOut = model.checkIn.let { kotlinx.datetime.LocalDate.fromEpochDays(it.toEpochDays() - 1) }
        assertFalse(model.canAdd)
        model.checkOut = model.checkIn.let { kotlinx.datetime.LocalDate.fromEpochDays(it.toEpochDays() + 1) }
        model.addToQueue()
        model.identificador = "TEST-A"
        model.lookUp()
        assertFalse(model.canAdd)
        model.addToQueue()
        assertEquals(1, model.queue.size)
    }

    /** A Cuban living abroad is found by passport and is a main guest; only people found by CI count as companions. */
    @Test
    fun companionRuleFollowsLookupNotNationality() = runTest {
        val client = StubPortalClient()
        client.person = PersonaValidation.fromJson(
            PortalJson.parseToJsonElement(
                """{"Nombre":"Test","Apellido1":"Guest","Biografico":"Test Guest","Identificador":"X1","Nacionalidad":"247","Sexo":"F","FechaNac":"/Date(0)/"}""",
            ),
        )
        val guests = model(client)
        guests.lookupKind = RegisterGuestViewModel.LookupKind.ForeignGuest
        guests.identificador = "X1"
        guests.lookUp()
        guests.addToQueue()
        assertNull(guests.companionWarning)
        assertTrue(guests.canSubmit)

        val companions = model(client)
        companions.lookupKind = RegisterGuestViewModel.LookupKind.CubanCompanion
        companions.identificador = "00000000001"
        companions.lookUp()
        companions.addToQueue()
        assertNotNull(companions.companionWarning)
        assertTrue(companions.canSubmit, "Warning only: the portal decides")
    }

    @Test
    fun partialSuccessRetainsRejectedPeople() = runTest {
        val client = StubPortalClient()
        val model = model(client)
        for (id in listOf("TEST-A", "TEST-B")) {
            client.person = fixturePerson(id)
            model.identificador = id
            model.lookUp()
            model.addToQueue()
        }
        client.results = listOf(fixtureGuest(), fixtureGuest("TEST-B", "Rejected"))
        model.submit()
        assertEquals(listOf("TEST-B"), model.queue.map { it.identificador })
        assertNotNull(model.errorMessage)
    }

    @Test
    fun uncertainTransportFailureDoesNotLoseQueue() = runTest {
        val client = StubPortalClient().apply { person = fixturePerson() }
        val model = model(client)
        model.identificador = "TEST-A"
        model.lookUp()
        model.addToQueue()
        client.submitError = PortalError.RegistrationUncertain
        model.submit()
        assertEquals(1, model.queue.size)
        assertEquals(1, client.submissions)
        assertTrue(model.isUncertain(model.queue.first()))
    }

    /** The banner must not wait for the Register tab to be on screen again. */
    @Test
    fun expiredSessionIsReportedAtOnce() = runTest {
        val client = StubPortalClient().apply { person = fixturePerson() }
        var reported = 0
        val model = RegisterGuestViewModel(client, { cubaAfternoon }) { reported += 1 }
        model.identificador = "TEST-A"
        model.lookUp()
        model.addToQueue()
        client.submitError = PortalError.SessionExpired
        model.submit()
        assertEquals(1, reported)
        assertEquals(1, model.queue.size)
    }
}
