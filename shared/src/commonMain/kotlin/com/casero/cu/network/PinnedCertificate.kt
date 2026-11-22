package com.casero.cu.network

import com.casero.cu.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The DER certificate of `casero.rem.cu`. The portal serves a certificate that
 * fails validation against the device's public CA store, so the platform engines
 * trust exactly this leaf for that host (see `PortalEngine.kt`). It ships as
 * `composeResources/files/casero_rem_cu.cer`; if it is absent every request
 * fails with `CertificateNotBundled` (fail closed).
 */
class PinnedCertificate(private val loader: suspend () -> ByteArray? = ::bundled) {
    private val mutex = Mutex()

    /** Set once [load] succeeds; read by the TLS callbacks, which cannot suspend. */
    var der: ByteArray? = null
        private set

    /** Loads the certificate once. Returns whether it is available. */
    suspend fun load(): Boolean {
        der?.let { return true }
        return mutex.withLock {
            if (der == null) der = loader()?.takeIf { it.isNotEmpty() }
            der != null
        }
    }

    companion object {
        const val RESOURCE = "files/casero_rem_cu.cer"

        /** The app's certificate, shared by the engine and the client. */
        val Bundled: PinnedCertificate by lazy { PinnedCertificate() }

        private suspend fun bundled(): ByteArray? =
            try {
                Res.readBytes(RESOURCE)
            } catch (_: Exception) {
                null
            }
    }
}

/** Builds the platform engine that pins [certificate] for `casero.rem.cu`. */
expect fun portalEngine(certificate: PinnedCertificate): io.ktor.client.engine.HttpClientEngine

/** Whether [error] is a TLS handshake or certificate failure on this platform. */
expect fun isSecureConnectionFailure(error: Throwable): Boolean
