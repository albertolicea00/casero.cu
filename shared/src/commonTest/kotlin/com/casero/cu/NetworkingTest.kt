package com.casero.cu

import com.casero.cu.model.CompanionRegistration
import com.casero.cu.model.GuestRegistration
import com.casero.cu.network.CaseroClient
import com.casero.cu.network.PinnedCertificate
import com.casero.cu.network.PortalError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val TOKEN_HTML = "<input name=\"__RequestVerificationToken\" value=\"fixture-token\">"

private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
    CaseroClient(MockEngine(handler), PinnedCertificate { byteArrayOf(1) })

private fun MockRequestHandleScope.html() =
    respond(TOKEN_HTML, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))

private fun MockRequestHandleScope.json(body: String) =
    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

private val HttpRequestData.form: String get() = (body as? TextContent)?.text ?: ""

private fun registration() = GuestRegistration(
    firstName = "Test", lastName1 = "Guest", lastName2 = "", identificador = "TEST", nationalityCode = "428",
    sex = "F", birthDate = cubaAfternoon, checkIn = cubaAfternoon, checkOut = cubaAfternoon, procedenciaCode = "428",
)

class NetworkingTest {
    @Test
    fun guestListLoadsEveryPage() = runTest {
        val client = client { request ->
            if (request.method == HttpMethod.Get) return@client html()
            val page = if ("pagina=1" in request.form) 1 else 2
            json("""{"Error":"","Pagina":$page,"Total":2,"ListaHuespedes":[{"Biografico":"Test","Identificador":"TEST-$page","EstanciaId":"$page"}]}""")
        }
        val guests = client.listGuests(cubaAfternoon, cubaAfternoon, pageSize = 1)
        assertEquals(listOf("TEST-1", "TEST-2"), guests.map { it.identificador })
    }

    @Test
    fun sessionRecoveryRetriesWholeReadWithFreshToken() = runTest {
        var recovered = false
        var gets = 0
        val client = client { request ->
            if (request.method == HttpMethod.Get) {
                gets += 1
                return@client html()
            }
            if (!recovered) json("""{"message":"REDIRECT"}""") else json("""{"Error":"","Pagina":1,"Total":0,"ListaHuespedes":[]}""")
        }
        client.recoverSession = {
            recovered = true
            true
        }
        assertTrue(client.listGuests(cubaAfternoon, cubaAfternoon).isEmpty())
        assertEquals(2, gets)
    }

    /** Captured from the portal when it refuses a registration: the app must show the portal's own message. */
    @Test
    fun registrationRejectionShowsPortalMessage() = runTest {
        val client = client { request ->
            if (request.method == HttpMethod.Get) html()
            else json("""{"success":false,"message":"No se ha registrado ningún huésped.","errors":{"nombre":"Error"}}""")
        }
        val error = assertFailsWith<PortalError.Rejected> { client.registerGuests(listOf(registration())) }
        assertEquals("No se ha registrado ningún huésped.", error.portalMessage)
    }

    /**
     * Mirrors a browser capture of adding a companion from the active-guests page
     * (synthetic person): CI lookup with `tipoDocumento=1`, then
     * `RegistrarAcompannantes` with the stay ID and a 7-field entry whose name has spaces.
     */
    @Test
    fun addCompanionMatchesPortal() = runTest {
        val bodies = mutableListOf<String>()
        val client = client { request ->
            val path = request.url.encodedPath
            if (request.method == HttpMethod.Get) {
                assertTrue(path.endsWith("Huespedes/HuespedesActivos"))
                return@client html()
            }
            bodies += request.form
            if (path.endsWith("ValidarPersonaSuin")) {
                json("""[{"IdPersona":"x","Identificador":"00000000001","Nombre":"ANA","Nombre2":"MARIA","Apellido1":"LOPEZ","Apellido2":"DIAZ","Biografico":"ANA MARIA LOPEZ DIAZ","Sexo":"F","FechaNac":"\/Date(481611600000)\/","Nacionalidad":"247","DescNacionalidad":"CUBA"}]""")
            } else {
                assertTrue(path.endsWith("RegistrarAcompannantes"))
                json("""[{"Biografico":"ANA MARIA LOPEZ DIAZ","Identificador":"00000000001","FechaEntrada":"\/Date(1795323600000)\/","FechaSalida":"\/Date(1795410000000)\/","FechaNacimiento":"\/Date(481611600000)\/","Sexo":"F","EstanciaId":"20000001","Id":"1","Respuesta":"OK"}]""")
            }
        }
        val person = client.validateCompanionForStay("00000000001").first()
        val entry = CompanionRegistration(
            fullName = person.fullName, identificador = person.identificador,
            nationalityCode = person.nationalityCode ?: "", sex = person.sex ?: "",
            birthDate = person.birthDate!!, checkIn = ms(1_795_323_600_000), checkOut = ms(1_795_410_000_000),
        )
        assertEquals("ANA MARIA LOPEZ DIAZ,00000000001,247,F,06/04/85,22/11/26,23/11/26", entry.wireFormat)
        val answer = client.addCompanions(listOf(entry), "20000001")
        assertEquals("20000001", answer.first().stayId)
        assertEquals("1", answer.first().companionId)
        assertEquals("OK", answer.first().response)
        assertTrue(bodies[0].startsWith("identificador=00000000001&tipoDocumento=1&"))
        assertTrue(
            bodies[1].startsWith(
                "estanciaId=20000001&personas=ANA%20MARIA%20LOPEZ%20DIAZ%2C00000000001%2C247%2CF%2C06%2F04%2F85%2C22%2F11%2F26%2C23%2F11%2F26&",
            ),
        )
    }

    /**
     * Mirrors browser captures of the check-out updates (synthetic stay): guest and
     * companion endpoints, `acompananteId` spelling, `dd/MM/yy`, and both answers.
     */
    @Test
    fun updateCheckOutMatchesPortal() = runTest {
        val bodies = mutableListOf<String>()
        val client = client { request ->
            if (request.method == HttpMethod.Get) return@client html()
            bodies += request.url.encodedPath.substringAfterLast("/") + "?" + request.form
            if (bodies.size == 3) {
                json("""{"success":false,"message":"Fecha no válida."}""")
            } else {
                json("""{"success":true,"message":"La operación se ha realizado satisfactoriamente."}""")
            }
        }
        val date = ms(1_795_410_000_000) // 23/11/26 in Cuba
        assertEquals("La operación se ha realizado satisfactoriamente.", client.updateCheckOut("20000001", checkOut = date))
        client.updateCheckOut("20000001", "1", date)
        assertTrue(bodies[0].startsWith("ActualizarFechaSalidaHuesped?estanciaId=20000001&fechaSalida=23%2F11%2F26&"))
        assertTrue(bodies[1].startsWith("ActualizarFechaSalidaAcompannante?estanciaId=20000001&acompananteId=1&fechaSalida=23%2F11%2F26&"))
        val error = assertFailsWith<PortalError.Rejected> { client.updateCheckOut("20000001", checkOut = date) }
        assertEquals("Fecha no válida.", error.portalMessage)
    }

    @Test
    fun uncertainRegistrationFailureIsNeverAutomaticallyRetried() = runTest {
        var recovered = false
        var posts = 0
        val client = client { request ->
            if (request.method == HttpMethod.Get) return@client html()
            posts += 1
            throw IllegalStateException("timed out")
        }
        client.recoverSession = {
            recovered = true
            true
        }
        assertFailsWith<PortalError.RegistrationUncertain> { client.registerGuests(listOf(registration())) }
        assertEquals(1, posts)
        assertFalse(recovered)
    }

    @Test
    fun serverErrorAfterSendingIsUncertain() = runTest {
        val client = client { request ->
            if (request.method == HttpMethod.Get) html() else respond("", HttpStatusCode.BadGateway)
        }
        assertFailsWith<PortalError.RegistrationUncertain> { client.registerGuests(listOf(registration())) }
    }

    @Test
    fun missingCertificateFailsClosed() = runTest {
        val client = CaseroClient(MockEngine { html() }, PinnedCertificate { null })
        assertFailsWith<PortalError.CertificateNotBundled> { client.listGuests(cubaAfternoon, cubaAfternoon) }
    }

    @Test
    fun repeatedPageFailsInsteadOfExportingIncompleteData() = runTest {
        val client = client { request ->
            if (request.method == HttpMethod.Get) html()
            else json("""{"Error":"","Total":3,"ListaHuespedes":[{"Biografico":"Test","Identificador":"TEST","EstanciaId":"1"}]}""")
        }
        assertFailsWith<PortalError.InvalidResponse> { client.listGuests(cubaAfternoon, cubaAfternoon, pageSize = 1) }
    }

    @Test
    fun loginAcceptsOnlyARedirectIntoThePortal() = runTest {
        fun loginClient(location: String?) = client { request ->
            if (request.method == HttpMethod.Get) return@client html()
            if (location == null) html() else respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, location))
        }
        loginClient("/Plataforma/Principal").login("user", "Passw0rd!")
        assertFailsWith<PortalError.InvalidCredentials> { loginClient("/Plataforma/Cuenta/IniciarSesion").login("user", "Passw0rd!") }
        assertFailsWith<PortalError.InvalidCredentials> { loginClient("https://example.invalid/Plataforma").login("user", "Passw0rd!") }
        assertFailsWith<PortalError.InvalidCredentials> { loginClient(null).login("user", "Passw0rd!") }
    }
}
