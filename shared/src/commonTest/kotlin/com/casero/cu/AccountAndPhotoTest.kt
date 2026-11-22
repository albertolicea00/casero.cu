package com.casero.cu

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
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
    CaseroClient(MockEngine(handler), PinnedCertificate { byteArrayOf(1) })

private val IMAGE = byteArrayOf(-1, -40, -1, -32)

class AccountAndPhotoTest {
    @Test
    fun foreignPhotoSendsTheCapturedQuery() = runTest {
        var url = ""
        val photo = client { request ->
            url = request.url.toString()
            respond(IMAGE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "image/jpeg"))
        }.fetchForeignGuestPhoto("X0000001", "JUAN CARLOS PEREZ", "201", "M")
        assertContentEquals(IMAGE, photo)
        assertEquals(
            "https://casero.rem.cu/Plataforma/Huespedes/ObtenerFotoDiie?pasap=X0000001&nomb=JUAN&apel1=&apel2=&ciud=201&sexo=M",
            url,
        )
    }

    @Test
    fun companionPhotoIsLookedUpByCi() = runTest {
        var url = ""
        client { request ->
            url = request.url.toString()
            respond(IMAGE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "image/png"))
        }.fetchCompanionPhoto("00000000001")
        assertTrue(url.endsWith("Huespedes/ObtenerFotoSUINPorCi?ci=00000000001"))
    }

    @Test
    fun photoAnswersThatAreNotImagesFail() = runTest {
        assertFailsWith<PortalError.InvalidResponse> {
            client { respond("<html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html")) }
                .fetchCompanionPhoto("1")
        }
        assertFailsWith<PortalError.SessionExpired> {
            client { respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/Plataforma/Cuenta/IniciarSesion")) }
                .fetchCompanionPhoto("1")
        }
    }

    @Test
    fun profileIsReadFromThePortalPage() = runTest {
        val profile = client {
            respond(
                """<div class="row well perfil"><table><tr><td class="titulo">Usuario</td><td class="texto">X000Y</td></tr></table></div>""",
                HttpStatusCode.OK,
            )
        }.fetchProfile()
        assertEquals("X000Y", profile.username)
    }

    @Test
    fun loginPageInsteadOfProfileMeansExpiredSession() = runTest {
        assertFailsWith<PortalError.SessionExpired> {
            client { respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/Plataforma/Cuenta/IniciarSesion")) }
                .fetchProfile()
        }
    }

    @Test
    fun channelsListPhonesAndEmails() = runTest {
        var body = ""
        val channels = client { request ->
            if (request.method == HttpMethod.Get) return@client respond("<input name='__RequestVerificationToken' value='t'>")
            body = (request.body as io.ktor.http.content.TextContent).text
            respond("""[{"Tipo":"sms","Valor":"+5350000000"},{"Tipo":"correo","Valor":"host@example.invalid"}]""")
        }.listCommunicationChannels()
        assertEquals("__RequestVerificationToken=t", body)
        assertTrue(channels[0].isPhone)
        assertFalse(channels[1].isPhone)
        assertEquals("host@example.invalid", channels[1].value)
    }

    @Test
    fun signOutForgetsTheSessionCookies() = runTest {
        val cookies = mutableListOf<String?>()
        val client = client { request ->
            cookies += request.headers[HttpHeaders.Cookie]
            respond("<input name='__RequestVerificationToken' value='t'>", HttpStatusCode.OK, headersOf(HttpHeaders.SetCookie, "ASP.NET_SessionId=abc; Path=/"))
        }
        client.fetchProfileOrNull()
        client.signOut()
        client.fetchProfileOrNull()
        assertTrue(cookies[1]?.contains("ASP.NET_SessionId=abc") == true, "Cookie sent on sign-out")
        assertFalse(cookies[2]?.contains("ASP.NET_SessionId") == true, "Cookie forgotten after sign-out")
    }

    private suspend fun CaseroClient.fetchProfileOrNull() = try {
        fetchProfile()
    } catch (_: PortalError) {
        null
    }
}
