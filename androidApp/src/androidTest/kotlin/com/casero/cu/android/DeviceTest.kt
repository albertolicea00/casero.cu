package com.casero.cu.android

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.casero.cu.export.ExportService
import com.casero.cu.export.ResourceExportLabels
import com.casero.cu.model.Guest
import com.casero.cu.network.PinnedCertificate
import com.casero.cu.platform.AndroidPlatform
import com.casero.cu.platform.SecureStore
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/** What only a real Android device can check: Keystore, PDF rendering and bundled resources. */
@OptIn(kotlin.time.ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class DeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val platform = AndroidPlatform.get(context)

    @Test
    fun pinnedCertificateShipsInTheApk() = runTest {
        val certificate = PinnedCertificate()
        assertTrue(certificate.load())
        assertEquals(0x30.toByte(), certificate.der!![0])
    }

    @Test
    fun keystoreRoundTripsAndForgetsCredentials() {
        val store = platform.secureStore
        store.set(SecureStore.USERNAME, "test-host")
        store.set(SecureStore.PASSWORD, "Test-0001!") // pragma: allowlist secret (test fixture)
        assertEquals("test-host", store.get(SecureStore.USERNAME))
        assertEquals("Test-0001!", store.get(SecureStore.PASSWORD))
        store.clearAll()
        assertNull(store.get(SecureStore.USERNAME))
        assertNull(store.get(SecureStore.PASSWORD))
    }

    @Test
    fun guestReportRendersSeveralPages() = runTest {
        val day = Instant.fromEpochMilliseconds(1_772_600_400_000)
        val guests = (0 until 40).map {
            Guest(fullName = "Test Guest $it", identificador = "TEST-$it", checkIn = day, checkOut = day, stayId = "$it")
        }
        val data = ExportService(platform.pdf, ResourceExportLabels).pdf(guests, day, day)
        assertEquals("%PDF", data.copyOfRange(0, 4).decodeToString())
        val file = File.createTempFile("guests", ".pdf", context.cacheDir).apply { writeBytes(data) }
        val pages = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { it.pageCount }
        file.delete()
        assertTrue(pages > 1, "40 guests need more than one page, got $pages")
    }
}
