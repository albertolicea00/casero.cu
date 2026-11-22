package com.casero.cu

import com.casero.cu.network.PinnedCertificate
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * iOS only: Android local unit tests cannot read Compose resources (they ship as
 * APK assets). On Android the check is that `assets/composeResources/…/files/casero_rem_cu.cer`
 * is in the APK.
 */
class CertificateTest {
    /** The pinned certificate must ship with the app, or every request fails closed. */
    @Test
    fun publicCertificateIsIncludedInTheApp() = runTest {
        val certificate = PinnedCertificate()
        assertTrue(certificate.load())
        // DER starts with an ASN.1 SEQUENCE.
        assertNotNull(certificate.der).let { assertTrue(it[0] == 0x30.toByte()) }
    }
}
