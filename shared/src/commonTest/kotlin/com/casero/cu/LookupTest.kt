package com.casero.cu

import com.casero.cu.network.CaseroClient
import com.casero.cu.network.PersonaLookup
import com.casero.cu.network.PinnedCertificate
import com.casero.cu.network.PortalError
import com.casero.cu.network.RejectionContext
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val PERSON =
    """{"IdPersona":null,"Identificador":"X0000001","Nombre":"JUAN","Nombre2":"CARLOS","Apellido1":"PEREZ",""" +
        """"Apellido2":"GOMEZ","Biografico":"JUAN CARLOS PEREZ GOMEZ","Sexo":"M","FechaNac":"/Date(122533200000)/",""" +
        """"Nacionalidad":"201","DescNacionalidad":"MEXICO","Estado":"Entrada","Respuesta":null}"""

/** Mirrors the person lookups in AGENTS.md (synthetic people, real shapes). */
class LookupTest {
    private class Recorder {
        val posts = mutableListOf<Pair<String, String>>()
        val referers = mutableListOf<String?>()
    }

    private fun client(recorder: Recorder, answer: String) = CaseroClient(
        MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                return@MockEngine respond("<input name=\"__RequestVerificationToken\" value=\"t\">", HttpStatusCode.OK)
            }
            recorder.posts += request.url.encodedPath.substringAfterLast('/') to ((request.body as TextContent).text)
            recorder.referers += request.headers[HttpHeaders.Referrer]
            respond(answer, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        },
        PinnedCertificate { byteArrayOf(1) },
    )

    @Test
    fun passportNameAndTemporaryIdUseTheirPortalTipo() = runTest {
        val recorder = Recorder()
        val client = client(recorder, "[$PERSON]")
        client.validatePersonaDiie("X0000001", PersonaLookup.ByPassport)
        client.validatePersonaDiie("JUAN_PEREZ_GOMEZ", PersonaLookup.ByName)
        client.validatePersonaDiie("T0000001", PersonaLookup.ByTemporaryCarne)
        client.validatePersonaSuin("00000000001")
        assertEquals(
            listOf(
                "ValidarPersonaDiie" to "identificador=X0000001&tipo=0&__RequestVerificationToken=t",
                "ValidarPersonaDiie" to "identificador=JUAN_PEREZ_GOMEZ&tipo=2&__RequestVerificationToken=t",
                "ValidarPersonaDiie" to "identificador=T0000001&tipo=3&__RequestVerificationToken=t",
                "ValidarPersonaSuin" to "identificador=00000000001&__RequestVerificationToken=t",
            ),
            recorder.posts,
        )
        assertTrue(recorder.referers.all { it == CaseroClient.portalUrl("Huespedes") })
    }

    @Test
    fun answerIsReadAsArrayOrBareObject() = runTest {
        val people = client(Recorder(), "[$PERSON,$PERSON]").validatePersonaDiie("JUAN_PEREZ_", PersonaLookup.ByName)
        assertEquals(2, people.size)
        val person = client(Recorder(), PERSON).validatePersonaDiie("X0000001", PersonaLookup.ByPassport).single()
        assertEquals("JUAN", person.firstName)
        assertEquals("CARLOS", person.middleName)
        assertEquals("201", person.nationalityCode)
        assertEquals("Entrada", person.status)
        assertNull(person.idPersona)
    }

    @Test
    fun notFoundIsTheRejectionWithThePortalMessage() = runTest {
        val error = assertFailsWith<PortalError.Rejected> {
            client(Recorder(), """{"success":false,"message":"Persona no encontrada.","errors":{"nombre":"Error"}}""")
                .validatePersonaSuin("00000000001")
        }
        assertEquals("Persona no encontrada.", error.portalMessage)
        assertEquals(RejectionContext.Lookup, error.context)
    }

    @Test
    fun sessionExpiredDuringLookupIsReported() = runTest {
        assertFailsWith<PortalError.SessionExpired> {
            client(Recorder(), """{"message":"REDIRECT"}""").validatePersonaDiie("X0000001", PersonaLookup.ByPassport)
        }
    }
}
