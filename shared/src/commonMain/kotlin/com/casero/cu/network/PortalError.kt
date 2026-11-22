package com.casero.cu.network

/** Every error surfaced by the portal client. User-facing text is in `ui/Messages.kt`. */
sealed class PortalError(message: String? = null) : Exception(message) {
    /** The portal answered with the `{ "message": "REDIRECT" }` sentinel, or a 302 to login. */
    data object SessionExpired : PortalError()

    /** Login POST re-rendered the login page instead of redirecting: bad credentials. */
    data object InvalidCredentials : PortalError()

    /** The anti-forgery token could not be found in the page HTML. */
    data object AntiForgeryMissing : PortalError()

    /** The pinned certificate is not bundled, so the client refuses to connect (fail closed). */
    data object CertificateNotBundled : PortalError()

    /** Any non-success, non-redirect HTTP status. */
    data class Http(val code: Int) : PortalError("HTTP $code")

    /** Transport failure — most often the portal being unreachable from outside Cuba. */
    data object Unreachable : PortalError()

    /** TLS negotiation failed or the server certificate could not be validated. */
    data object SecureConnection : PortalError()

    /** The portal refused the request. Without a message, [context] picks the fallback text. */
    data class Rejected(
        val portalMessage: String?,
        val context: RejectionContext = RejectionContext.Request,
    ) : PortalError(portalMessage)

    data object InvalidResponse : PortalError()

    /**
     * The registration POST was sent but no answer arrived (connection lost,
     * timeout, server error). The portal may or may not have registered it.
     */
    data object RegistrationUncertain : PortalError()
}

/** What was refused, for the fallback text when the portal gives no message. */
enum class RejectionContext { Request, Lookup, Registration, CheckOut }
