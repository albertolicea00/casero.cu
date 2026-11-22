package com.casero.cu.network

import com.casero.cu.model.CommunicationChannel
import com.casero.cu.model.CompanionRegistration
import com.casero.cu.model.Guest
import com.casero.cu.model.GuestListResponse
import com.casero.cu.model.GuestRegistration
import com.casero.cu.model.PersonaValidation
import com.casero.cu.model.PortalDates
import com.casero.cu.model.PortalProfile
import com.casero.cu.model.bool
import com.casero.cu.model.parseJsonOrNull
import com.casero.cu.model.string
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Discriminates the `ValidarPersonaDiie` lookup modes seen in captured
 * traffic: `tipo=0` with a passport number, `tipo=2` with an underscore-joined
 * full name, `tipo=3` with a temporary carné number. `tipo=1` was never seen.
 * Passport, name and `ValidarPersonaSuin` lookups were captured returning an
 * array; `tipo=3` was only captured as "not found" and is assumed to match.
 */
enum class PersonaLookup(val code: Int) {
    ByPassport(0),
    ByName(2),
    ByTemporaryCarne(3),
}

/**
 * Thin client over the CASERO web portal. It behaves like a browser: cookies
 * and the anti-forgery token are carried on every request, and the portal's
 * login redirect / `REDIRECT` sentinel are mapped to `PortalError.SessionExpired`.
 *
 * [engine] carries the platform's TLS pinning (see `PortalEngine.kt`); tests pass
 * a mock engine. [certificate] loads the pinned certificate: without it every
 * request fails with `CertificateNotBundled`.
 */
class CaseroClient(
    engine: HttpClientEngine,
    private val certificate: PinnedCertificate,
) : PortalServing {

    companion object {
        const val BASE_URL = "https://casero.rem.cu/Plataforma/"
        const val PORTAL_HOST = "casero.rem.cu"

        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
        private const val TOKEN = "__RequestVerificationToken"

        fun portalUrl(path: String): String = BASE_URL + path
    }

    private val cookies = ClearableCookiesStorage()

    private val http = HttpClient(engine) {
        expectSuccess = false
        // Do not follow redirects: the login 302 and the session-expiry 302 are signals.
        followRedirects = false
        install(HttpCookies) { storage = cookies }
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000
            socketTimeoutMillis = 45_000
            connectTimeoutMillis = 45_000
        }
        defaultRequest { header(HttpHeaders.UserAgent, USER_AGENT) }
    }

    /**
     * Called only after an explicit session-expired response, never after a
     * timeout or an uncertain registration result.
     */
    var recoverSession: (suspend () -> Boolean)? = null

    private suspend fun <T> authenticated(operation: suspend () -> T): T =
        try {
            operation()
        } catch (_: PortalError.SessionExpired) {
            if (recoverSession?.invoke() != true) throw PortalError.SessionExpired
            operation()
        }

    // region Public API

    /** Authenticate. Throws `PortalError.InvalidCredentials` when the portal rejects the login. */
    suspend fun login(user: String, password: String) {
        val token = AntiForgery.extract(getHTML("Cuenta/IniciarSesion"))
        val body = formEncoded(listOf(TOKEN to token, "User" to user, "Password" to password))
        val response = perform(HttpMethod.Post, portalUrl("Cuenta/IniciarSesion")) {
            setBody(TextContent(body, ContentType.Application.FormUrlEncoded))
        }
        when (response.status.value) {
            in 300..399 -> {
                val location = response.headers[HttpHeaders.Location] ?: throw PortalError.InvalidCredentials
                val destination = try {
                    URLBuilder(BASE_URL).takeFrom(location).build()
                } catch (_: Exception) {
                    throw PortalError.InvalidCredentials
                }
                val valid = destination.protocol.name == "https" &&
                    destination.host == PORTAL_HOST &&
                    destination.encodedPath.startsWith("/Plataforma") &&
                    !destination.encodedPath.lowercase().contains("iniciarsesion")
                if (!valid) throw PortalError.InvalidCredentials
            }
            200 -> throw PortalError.InvalidCredentials
            else -> throw PortalError.Http(response.status.value)
        }
    }

    /** Fetches every page so filtering and exports cannot silently lose stays. */
    override suspend fun listGuests(from: Instant, to: Instant, pageSize: Int, page: Int): List<Guest> {
        if (from > to || pageSize !in 1..200 || page < 1) throw PortalError.InvalidResponse
        return authenticated {
            collectPages(page, pageSize) { currentPage ->
                val token = AntiForgery.extract(getHTML("Huespedes/HuespedesRegistrados"))
                val body = formEncoded(
                    listOf(
                        "fechaInicio" to PortalDates.portalFormat(from),
                        "fechaFin" to PortalDates.portalFormat(to),
                        "cantMaxRegistros" to pageSize.toString(),
                        "pagina" to currentPage.toString(),
                        TOKEN to token,
                    ),
                )
                val text = postForJSON("Huespedes/ListarHuespedes", body, referer = "Huespedes/HuespedesRegistrados")
                GuestListResponse.fromJson(parseJsonOrNull(text))
            }
        }
    }

    override suspend fun signOut() {
        withTimeoutOrNull(5_000) {
            try {
                perform(HttpMethod.Get, portalUrl("Cuenta/CerrarSesion"))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
        cookies.clear()
    }

    // endregion

    // region Profile

    /** The host's account details, read from the profile page's HTML table. */
    override suspend fun fetchProfile(): PortalProfile = authenticated {
        PortalProfile.parse(getHTML("Cuenta/Perfil"))
    }

    // endregion

    // region Communication channels

    /**
     * The host's registered phones and e-mails. Read-only: adding or removing
     * channels is left to the official page because that action was never captured.
     */
    override suspend fun listCommunicationChannels(): List<CommunicationChannel> = authenticated {
        val token = AntiForgery.extract(getHTML("ViasComunicacion"))
        val text = postForJSON(
            "ViasComunicacion/ListarViasComunicacion",
            formEncoded(listOf(TOKEN to token)),
            referer = "ViasComunicacion",
        )
        val json = parseJsonOrNull(text)
        rejection(json, RejectionContext.Request)?.let { throw it }
        CommunicationChannel.listFromJson(json)
    }

    // endregion

    // region Companions

    /**
     * Companions of a guest (Cuban nationals, identified by carné). Same paging
     * contract as `ListarHuespedes` per AGENTS.md, keyed by the guest's stay id.
     *
     * ASSUMPTION (unverified against live traffic): the request carries
     * `estanciaId` alongside the paging fields, and the response envelope is
     * shaped like `GuestListResponse` under a `ListaAcompannantes` key. Confirm
     * against a real capture before relying on this in production.
     */
    override suspend fun listCompanions(estanciaId: String, pageSize: Int, page: Int): List<Guest> {
        if (estanciaId.isEmpty() || pageSize !in 1..200 || page < 1) throw PortalError.InvalidResponse
        return authenticated {
            collectPages(page, pageSize) { currentPage ->
                val token = AntiForgery.extract(getHTML("Huespedes/HuespedesRegistrados"))
                val body = formEncoded(
                    listOf(
                        "estanciaId" to estanciaId,
                        "cantMaxRegistros" to pageSize.toString(),
                        "pagina" to currentPage.toString(),
                        TOKEN to token,
                    ),
                )
                val text = postForJSON("Huespedes/ListarAcompannantes", body, referer = "Huespedes/HuespedesRegistrados")
                GuestListResponse.fromJson(parseJsonOrNull(text), listKey = "ListaAcompannantes")
            }
        }
    }

    /** Walks pages until the reported total is reached; a repeated person or a short total fails. */
    private suspend fun collectPages(
        firstPage: Int,
        pageSize: Int,
        fetch: suspend (Int) -> GuestListResponse,
    ): List<Guest> {
        val people = mutableListOf<Guest>()
        val seen = mutableSetOf<String>()
        for (currentPage in firstPage until firstPage + 1000) {
            val result = fetch(currentPage)
            if (result.guests.isEmpty()) {
                if (result.total > people.size) throw PortalError.InvalidResponse
                return people
            }
            for (person in result.guests) {
                if (!seen.add(person.id)) throw PortalError.InvalidResponse
                people.add(person)
            }
            if (result.total > 0) {
                if (people.size >= result.total) return people
            } else if (result.guests.size < pageSize) {
                return people
            }
        }
        throw PortalError.InvalidResponse
    }

    // endregion

    // region Registration

    /**
     * Looks up a foreign guest in the DIIE registry. [identificador] is a
     * passport number (`ByPassport`), an underscore-joined full name (`ByName`)
     * or a temporary carné number (`ByTemporaryCarne`), per captured traffic.
     */
    override suspend fun validatePersonaDiie(identificador: String, tipo: PersonaLookup): List<PersonaValidation> =
        authenticated {
            validatePersona(
                "Huespedes/ValidarPersonaDiie",
                listOf("identificador" to identificador, "tipo" to tipo.code.toString()),
            )
        }

    /** Looks up a Cuban companion by carné de identidad in the SUIN registry. */
    override suspend fun validatePersonaSuin(identificador: String): List<PersonaValidation> = authenticated {
        validatePersona("Huespedes/ValidarPersonaSuin", listOf("identificador" to identificador))
    }

    /**
     * Captured responses come as an array (several matches are possible for a
     * name lookup), as a bare object in older captures, or as
     * `{ "success": false, "message": "Persona no encontrada." }` when nothing matches.
     */
    private suspend fun validatePersona(
        path: String,
        params: List<Pair<String, String>>,
        page: String = "Huespedes",
    ): List<PersonaValidation> {
        val token = AntiForgery.extract(getHTML(page))
        val text = postForJSON(path, formEncoded(params + (TOKEN to token)), referer = page)
        val json = parseJsonOrNull(text) ?: throw PortalError.InvalidResponse
        rejection(json, RejectionContext.Lookup)?.let { throw it }
        return when (json) {
            is JsonArray -> json.map(PersonaValidation::fromJson)
            else -> listOf(PersonaValidation.fromJson(json))
        }
    }

    /** Registers one or more guests/companions in a single call (`|`-joined `personas`). */
    override suspend fun registerGuests(people: List<GuestRegistration>): List<Guest> = authenticated {
        if (people.isEmpty()) return@authenticated emptyList()
        val token = AntiForgery.extract(getHTML("Huespedes"))
        val body = formEncoded(
            listOf("personas" to people.joinToString("|") { it.wireFormat }, TOKEN to token),
        )
        postRegistration("Huespedes/RegistrarHuesped", body, referer = "Huespedes")
    }

    // endregion

    // region Companions of an active stay

    /** CI lookup as the active-guests page does it (adds `tipoDocumento=1`). */
    override suspend fun validateCompanionForStay(identificador: String): List<PersonaValidation> = authenticated {
        validatePersona(
            "Huespedes/ValidarPersonaSuin",
            listOf("identificador" to identificador, "tipoDocumento" to "1"),
            page = "Huespedes/HuespedesActivos",
        )
    }

    /**
     * Adds Cuban companions to an existing stay. The answer carries the guest's
     * `EstanciaId` and the companion's own `Id`.
     */
    override suspend fun addCompanions(people: List<CompanionRegistration>, stayId: String): List<Guest> =
        authenticated {
            if (people.isEmpty() || stayId.isEmpty()) return@authenticated emptyList()
            val token = AntiForgery.extract(getHTML("Huespedes/HuespedesActivos"))
            val body = formEncoded(
                listOf(
                    "estanciaId" to stayId,
                    "personas" to people.joinToString("|") { it.wireFormat },
                    TOKEN to token,
                ),
            )
            postRegistration("Huespedes/RegistrarAcompannantes", body, referer = "Huespedes/HuespedesActivos")
        }

    /**
     * Changes the check-out date of an active stay (`companionId == null`) or of one
     * of its companions. Returns the portal's confirmation message. Re-sending the
     * same date is harmless, so a lost answer is an ordinary error here.
     */
    override suspend fun updateCheckOut(stayId: String, companionId: String?, checkOut: Instant): String =
        authenticated {
            val token = AntiForgery.extract(getHTML("Huespedes/HuespedesActivos"))
            val params = buildList {
                add("estanciaId" to stayId)
                if (companionId != null) add("acompananteId" to companionId)
                add("fechaSalida" to PortalDates.portalFormat(checkOut))
                add(TOKEN to token)
            }
            val path = if (companionId == null) {
                "Huespedes/ActualizarFechaSalidaHuesped"
            } else {
                "Huespedes/ActualizarFechaSalidaAcompannante"
            }
            val text = postForJSON(path, formEncoded(params), referer = "Huespedes/HuespedesActivos")
            val json = parseJsonOrNull(text) as? JsonObject ?: throw PortalError.InvalidResponse
            val success = json.bool("success") ?: throw PortalError.InvalidResponse
            if (!success) throw PortalError.Rejected(json.string("message"), RejectionContext.CheckOut)
            json.string("message") ?: ""
        }

    /**
     * Shared by every write that registers people: a lost answer after sending is
     * `RegistrationUncertain`, a `{ success: false }` answer is `Rejected`.
     */
    private suspend fun postRegistration(path: String, body: String, referer: String): List<Guest> {
        val text = try {
            postForJSON(path, body, referer)
        } catch (_: PortalError.Unreachable) {
            // The form was already sent: losing the answer does not mean nothing was registered.
            throw PortalError.RegistrationUncertain
        } catch (error: PortalError.Http) {
            if (error.code >= 500) throw PortalError.RegistrationUncertain
            throw error
        }
        val json = parseJsonOrNull(text) ?: throw PortalError.InvalidResponse
        rejection(json, RejectionContext.Registration)?.let { throw it }
        return Guest.listFromJson(json)
    }

    // endregion

    // region Photos

    /** Foreign guest photo. Query parameters mirror the captured request exactly. */
    override suspend fun fetchForeignGuestPhoto(
        passport: String,
        name: String,
        nationalityCode: String,
        sex: String,
    ): ByteArray = authenticated {
        val givenName = name.trim().split(Regex("\\s+")).firstOrNull() ?: ""
        val photoUrl = URLBuilder(portalUrl("Huespedes/ObtenerFotoDiie")).apply {
            parameters.append("pasap", passport)
            parameters.append("nomb", givenName)
            parameters.append("apel1", "")
            parameters.append("apel2", "")
            parameters.append("ciud", nationalityCode)
            parameters.append("sexo", sex)
        }.buildString()
        fetchPhoto(photoUrl)
    }

    /** Companion (Cuban national) photo, looked up by carné de identidad. */
    override suspend fun fetchCompanionPhoto(carne: String): ByteArray = authenticated {
        val photoUrl = URLBuilder(portalUrl("Huespedes/ObtenerFotoSUINPorCi")).apply {
            parameters.append("ci", carne)
        }.buildString()
        fetchPhoto(photoUrl)
    }

    private suspend fun fetchPhoto(photoUrl: String): ByteArray {
        val response = perform(HttpMethod.Get, photoUrl)
        val status = response.status.value
        if (status !in 200..299) {
            if (status in 300..399) throw PortalError.SessionExpired
            throw PortalError.Http(status)
        }
        val data = response.bodyAsBytes()
        if (isRedirectSentinel(data.decodeToString())) throw PortalError.SessionExpired
        if (response.headers[HttpHeaders.ContentType]?.startsWith("image/") != true) throw PortalError.InvalidResponse
        return data
    }

    // endregion

    // region Requests

    private suspend fun getHTML(path: String): String {
        val response = perform(HttpMethod.Get, portalUrl(path))
        val status = response.status.value
        if (status in 300..399) throw PortalError.SessionExpired
        if (status !in 200..299) throw PortalError.Http(status)
        return response.bodyAsText()
    }

    private suspend fun postForJSON(path: String, body: String, referer: String): String {
        val response = perform(HttpMethod.Post, portalUrl(path)) {
            setBody(TextContent(body, ContentType.parse("application/x-www-form-urlencoded; charset=UTF-8")))
            header("X-Requested-With", "XMLHttpRequest")
            header(HttpHeaders.Accept, "application/json, text/javascript, */*; q=0.01")
            header(HttpHeaders.Referrer, portalUrl(referer))
        }
        val status = response.status.value
        if (status in 300..399) throw PortalError.SessionExpired
        if (status !in 200..299) throw PortalError.Http(status)
        val text = response.bodyAsText()
        if (isRedirectSentinel(text)) throw PortalError.SessionExpired
        return text
    }

    private suspend fun perform(
        method: HttpMethod,
        target: String,
        configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        if (!certificate.load()) throw PortalError.CertificateNotBundled
        // The client pins one host; refuse anything else rather than trust it.
        if (Url(target).host != PORTAL_HOST) throw PortalError.InvalidResponse
        return try {
            http.request {
                this.method = method
                url(target)
                configure()
            }.also { it.bodyAsBytes() }
        } catch (error: CancellationException) {
            if (error is io.ktor.client.plugins.HttpRequestTimeoutException) throw PortalError.Unreachable
            throw error
        } catch (error: PortalError) {
            throw error
        } catch (error: Throwable) {
            throw if (isSecureConnectionFailure(error)) PortalError.SecureConnection else PortalError.Unreachable
        }
    }

    // endregion

    // region Helpers

    /** The portal returns `{ "message": "REDIRECT" }` on AJAX calls when the session is dead. */
    private fun isRedirectSentinel(text: String): Boolean =
        (parseJsonOrNull(text) as? JsonObject)?.string("message") == "REDIRECT"

    /** The error for a `{ success: false, message }` answer, or `null` for anything else. */
    private fun rejection(json: kotlinx.serialization.json.JsonElement?, context: RejectionContext): PortalError.Rejected? {
        val obj = json as? JsonObject ?: return null
        if (obj.bool("success") != false) return null
        return PortalError.Rejected(obj.string("message"), context)
    }

    // endregion
}

/** Percent-encodes like the iOS client: only `A-Z a-z 0-9 - . _ ~` stay as is. */
internal fun formEncoded(params: List<Pair<String, String>>): String =
    params.joinToString("&") { (key, value) -> "${percentEncode(key)}=${percentEncode(value)}" }

private fun percentEncode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val char = (byte.toInt() and 0xFF).toChar()
        if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._~") {
            append(char)
        } else {
            append('%')
            append(((byte.toInt() and 0xFF) or 0x100).toString(16).substring(1).uppercase())
        }
    }
}

/** In-memory cookie jar that sign-out can empty. */
private class ClearableCookiesStorage : CookiesStorage {
    private var inner = AcceptAllCookiesStorage()

    fun clear() {
        inner.close()
        inner = AcceptAllCookiesStorage()
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = inner.get(requestUrl)

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = inner.addCookie(requestUrl, cookie)

    override fun close() = inner.close()
}
