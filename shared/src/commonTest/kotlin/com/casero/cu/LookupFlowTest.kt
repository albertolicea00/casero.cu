package com.casero.cu

import com.casero.cu.feature.register.RegisterGuestViewModel
import com.casero.cu.feature.register.RegisterGuestViewModel.LookupKind
import com.casero.cu.model.PersonaValidation
import com.casero.cu.network.PersonaLookup
import com.casero.cu.network.PortalError
import com.casero.cu.network.RejectionContext
import com.casero.cu.ui.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** The Register tab's lookup, against a stub portal. */
class LookupFlowTest {
    private class Client : com.casero.cu.network.PortalServing by StubPortalClient() {
        var answer: List<PersonaValidation> = emptyList()
        var error: Exception? = null
        val calls = mutableListOf<Pair<String, PersonaLookup?>>()

        override suspend fun validatePersonaDiie(identificador: String, tipo: PersonaLookup): List<PersonaValidation> {
            calls += identificador to tipo
            error?.let { throw it }
            return answer
        }

        override suspend fun validatePersonaSuin(identificador: String): List<PersonaValidation> {
            calls += identificador to null
            error?.let { throw it }
            return answer
        }
    }

    private fun model(client: Client) = RegisterGuestViewModel(client, { cubaAfternoon })

    @Test
    fun nameLookupJoinsUppercasePartsWithUnderscores() = runTest {
        val client = Client().apply { answer = listOf(fixturePerson()) }
        val model = model(client)
        model.lookupKind = LookupKind.ForeignGuestByName
        model.firstName = " juan "
        model.lastName1 = "pérez"
        assertTrue(model.canLookUp, "First name and first surname are enough")
        model.lookUp()
        // An empty second surname keeps the trailing underscore, which the portal matches.
        assertEquals("JUAN_PÉREZ_" to PersonaLookup.ByName, client.calls.single())
    }

    @Test
    fun eachKindCallsItsRegistry() = runTest {
        val client = Client().apply { answer = listOf(fixturePerson()) }
        val model = model(client)
        for ((kind, expected) in listOf(
            LookupKind.ForeignGuest to PersonaLookup.ByPassport,
            LookupKind.TemporaryCarne to PersonaLookup.ByTemporaryCarne,
            LookupKind.CubanCompanion to null,
        )) {
            model.lookupKind = kind
            model.identificador = " ID1 "
            model.lookUp()
            assertEquals("ID1" to expected, client.calls.last())
        }
    }

    @Test
    fun oneMatchIsSelectedAndSeveralAreListedToPick() = runTest {
        val client = Client().apply { answer = listOf(fixturePerson("A")) }
        val model = model(client)
        model.identificador = "A"
        model.lookUp()
        assertEquals("A", model.foundPerson?.identificador)

        client.answer = listOf(fixturePerson("A"), fixturePerson("B"))
        model.identificador = "AB"
        model.lookUp()
        assertTrue(model.isShowingResult)
        assertNull(model.foundPerson, "The host picks one of several")
        assertFalse(model.canAdd)
        model.select(model.candidates[1])
        assertTrue(model.canAdd)
        model.addToQueue()
        assertEquals(listOf("B"), model.queue.map { it.identificador })
        assertFalse(model.isShowingResult)
    }

    @Test
    fun emptyAnswerSaysNotFound() = runTest {
        val model = model(Client())
        model.identificador = "X"
        model.lookUp()
        assertNotNull(model.lookupErrorMessage)
        assertFalse(model.isShowingResult)
    }

    @Test
    fun nameRejectionAddsTheSecondNameHint() = runTest {
        val client = Client().apply { error = PortalError.Rejected("Persona no encontrada.", RejectionContext.Lookup) }
        val model = model(client)
        model.lookupKind = LookupKind.ForeignGuestByName
        model.firstName = "JUAN CARLOS"
        model.lastName1 = "PEREZ"
        model.lookUp()
        val message = assertIs<UiText.Joined>(model.lookupErrorMessage)
        assertEquals(UiText.Raw("Persona no encontrada."), message.parts.first())

        model.lookupKind = LookupKind.ForeignGuest
        model.identificador = "X"
        model.lookUp()
        assertEquals(UiText.Raw("Persona no encontrada."), model.lookupErrorMessage)
    }

    @Test
    fun unsupportedCharactersAndPortalRefusalsBlockTheQueue() = runTest {
        val bad = fixturePerson().copy(lastName1 = "PEREZ,GOMEZ")
        val model = model(Client().apply { answer = listOf(bad) })
        model.identificador = "X"
        model.lookUp()
        assertFalse(model.canAdd)

        val refused = fixturePerson().copy(response = "Persona con entrada activa.")
        val second = model(Client().apply { answer = listOf(refused) })
        second.identificador = "X"
        second.lookUp()
        assertEquals(UiText.Raw("Persona con entrada activa."), second.validationMessage)
    }
}
