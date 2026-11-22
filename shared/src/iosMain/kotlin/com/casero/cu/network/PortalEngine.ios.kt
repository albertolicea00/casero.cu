package com.casero.cu.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import platform.CoreFoundation.CFArrayCreate
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeArrayCallBacks
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCopyData
import platform.Security.SecCertificateRef
import platform.Security.SecPolicyCreateSSL
import platform.Security.SecTrustCopyCertificateChain
import platform.Security.SecTrustEvaluateWithError
import platform.Security.SecTrustRef
import platform.Security.SecTrustSetAnchorCertificates
import platform.Security.SecTrustSetAnchorCertificatesOnly
import platform.Security.SecTrustSetPolicies
import platform.Security.errSecSuccess

actual fun portalEngine(certificate: PinnedCertificate): HttpClientEngine = Darwin.create {
    configureSession {
        // Ktor keeps the cookies; the system jar would leak them across sign-outs.
        setHTTPCookieStorage(null)
        setHTTPShouldSetCookies(false)
        setURLCache(null)
        setRequestCachePolicy(NSURLRequestReloadIgnoringLocalCacheData)
    }
    handleChallenge { _, _, challenge, completionHandler ->
        val (disposition, credential) = evaluate(challenge, certificate)
        completionHandler(disposition, credential)
    }
}

actual fun isSecureConnectionFailure(error: Throwable): Boolean {
    val origin = (error as? DarwinHttpRequestException)?.origin ?: return false
    if (origin.domain != NSURLErrorDomain) return false
    // Cancelled (-999) is what a refused pin produces; -1200…-1206 are TLS and certificate failures.
    return origin.code == -999L || origin.code in -1206L..-1200L
}

/**
 * Pins the `casero.rem.cu` certificate. Trust is limited to the bundled leaf, then
 * evaluated against the portal hostname and normal certificate validity rules.
 * Other hosts get default handling.
 */
private fun evaluate(
    challenge: NSURLAuthenticationChallenge,
    certificate: PinnedCertificate,
): Pair<NSURLSessionAuthChallengeDisposition, NSURLCredential?> {
    val space = challenge.protectionSpace
    if (space.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
        return NSURLSessionAuthChallengePerformDefaultHandling to null
    }
    if (space.host.lowercase() != CaseroClient.PORTAL_HOST) return NSURLSessionAuthChallengePerformDefaultHandling to null
    val cancel = NSURLSessionAuthChallengeCancelAuthenticationChallenge to null
    val trust: SecTrustRef = space.serverTrust ?: return cancel
    // No bundled certificate — refuse rather than trust blindly.
    val pinned = certificate.der ?: return cancel
    val chain = SecTrustCopyCertificateChain(trust) ?: return cancel
    try {
        if (CFArrayGetCount(chain) < 1) return cancel
        val leaf: SecCertificateRef = CFArrayGetValueAtIndex(chain, 0)?.reinterpret() ?: return cancel
        val data = SecCertificateCopyData(leaf) ?: return cancel
        val leafBytes = try {
            CFDataGetBytePtr(data)?.readBytes(CFDataGetLength(data).toInt()) ?: return cancel
        } finally {
            CFRelease(data)
        }
        if (!leafBytes.contentEquals(pinned)) return cancel

        // The portal's issuing CA is private and is not installed on user devices.
        // Trust this exact pinned leaf as the sole anchor, while still checking its
        // dates, server-auth usage, and hostname for this request.
        val host = CFStringCreateWithCString(null, space.host, kCFStringEncodingUTF8) ?: return cancel
        val policy = SecPolicyCreateSSL(true, host)
        CFRelease(host)
        val trusted = memScoped {
            val anchors = CFArrayCreate(null, allocArrayOf(leaf).reinterpret(), 1, kCFTypeArrayCallBacks.ptr)
            try {
                SecTrustSetPolicies(trust, policy) == errSecSuccess &&
                    SecTrustSetAnchorCertificates(trust, anchors) == errSecSuccess &&
                    SecTrustSetAnchorCertificatesOnly(trust, true) == errSecSuccess &&
                    SecTrustEvaluateWithError(trust, null)
            } finally {
                anchors?.let(::CFRelease)
                policy?.let(::CFRelease)
            }
        }
        if (!trusted) return cancel
        return NSURLSessionAuthChallengeUseCredential to NSURLCredential.credentialForTrust(trust)
    } finally {
        CFRelease(chain)
    }
}
