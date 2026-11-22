package com.casero.cu.network

import android.annotation.SuppressLint
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.X509TrustManager

actual fun portalEngine(certificate: PinnedCertificate): HttpClientEngine = OkHttp.create {
    config {
        val trustManager = PinnedTrustManager(certificate)
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }
        sslSocketFactory(context.socketFactory, trustManager)
        followRedirects(false)
        followSslRedirects(false)
    }
}

actual fun isSecureConnectionFailure(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.take(8).any { it is SSLException || it is CertificateException }

/**
 * Trusts exactly the pinned leaf. The portal's issuing CA is private and is not
 * installed on user devices, so the chain is not built against the system store;
 * the leaf's dates and server-auth usage are still checked, and OkHttp's default
 * hostname verifier checks it against `casero.rem.cu`. The client never talks to
 * another host (`CaseroClient.perform`).
 */
@SuppressLint("CustomX509TrustManager")
private class PinnedTrustManager(private val certificate: PinnedCertificate) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val pinned = certificate.der ?: throw CertificateException("Pinned certificate not loaded")
        val leaf = chain?.firstOrNull() ?: throw CertificateException("Empty certificate chain")
        if (!leaf.encoded.contentEquals(pinned)) throw CertificateException("Certificate does not match the pin")
        leaf.checkValidity()
        val usages = leaf.extendedKeyUsage
        if (usages != null && SERVER_AUTH !in usages) throw CertificateException("Certificate is not for servers")
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("Client certificates are not supported")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    private companion object {
        const val SERVER_AUTH = "1.3.6.1.5.5.7.3.1"
    }
}
