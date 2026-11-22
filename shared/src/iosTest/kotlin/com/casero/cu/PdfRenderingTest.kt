package com.casero.cu

import com.casero.cu.export.ExportService
import com.casero.cu.export.ResourceExportLabels
import com.casero.cu.model.GuestRegistration
import com.casero.cu.platform.IosPlatform
import com.casero.cu.platform.toNSData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import platform.PDFKit.PDFDocument

/** Renders real PDFs with UIKit and reads them back with PDFKit. */
class PdfRenderingTest {
    private val exports = ExportService(IosPlatform.shared.pdf, ResourceExportLabels)

    @Test
    fun guestReportRendersEveryGuestOnSeveralPages() = runTest {
        val data = exports.pdf((0 until 40).map { fixtureGuest("TEST-$it") }, cubaAfternoon, cubaAfternoon)
        val document = assertNotNull(PDFDocument(data.toNSData()))
        assertTrue(document.pageCount > 1u)
        val text = document.string ?: ""
        assertTrue("TEST-0" in text && "TEST-39" in text)
    }

    @Test
    fun registrationRecordRendersStatusAndStayId() = runTest {
        val data = exports.registrationPdf(
            ExportService.RegistrationRecord.Submitted(listOf(fixtureGuest("TEST-A"), fixtureGuest("TEST-B", "Rejected"))),
        )
        val text = PDFDocument(data.toNSData())?.string ?: ""
        assertTrue("TEST-A" in text && "Rejected" in text)
    }

    @Test
    fun registrationRecordDrawsAPhoto() = runTest {
        val entry = GuestRegistration(
            firstName = "TEST", lastName1 = "A", lastName2 = "", identificador = "X0000001", nationalityCode = "428",
            sex = "F", birthDate = cubaAfternoon, checkIn = cubaAfternoon, checkOut = cubaAfternoon, procedenciaCode = "428",
        )
        val data = exports.registrationPdf(ExportService.RegistrationRecord.Prepared(listOf(entry)), mapOf("X0000001" to PNG_1X1))
        assertEquals(1, PDFDocument(data.toNSData())?.pageCount?.toInt())
    }
}

/** A 1×1 PNG. */
private val PNG_1X1 = byteArrayOf(
    -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, -60,
    -119, 0, 0, 0, 13, 73, 68, 65, 84, 120, -38, 99, -8, -1, -1, 63, 0, 5, -2, 2, -2, -89, 53, -127, -124, 0, 0, 0, 0,
    73, 69, 78, 68, -82, 66, 96, -126,
)
