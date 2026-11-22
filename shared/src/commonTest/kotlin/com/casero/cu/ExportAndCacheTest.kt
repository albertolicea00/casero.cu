package com.casero.cu

import com.casero.cu.app.GuestPhotoService
import com.casero.cu.export.ExportLabels
import com.casero.cu.export.ExportService
import com.casero.cu.feature.settings.SettingsViewModel
import com.casero.cu.model.Guest
import com.casero.cu.model.PortalDates
import com.casero.cu.persistence.AppSettings
import com.casero.cu.persistence.GuestCache
import com.casero.cu.persistence.PhotoCache
import com.casero.cu.platform.ConnectivityMonitor
import com.casero.cu.platform.DeviceOwnerCheck
import com.casero.cu.platform.FileSharer
import com.casero.cu.platform.KeyValueStore
import com.casero.cu.platform.MessageComposer
import com.casero.cu.platform.MessageResult
import com.casero.cu.platform.PdfCanvas
import com.casero.cu.platform.PdfRenderer
import com.casero.cu.platform.PdfTextStyle
import com.casero.cu.platform.Platform
import com.casero.cu.platform.SecureStore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

/** Records pages and text instead of drawing; one line of text is 14 points high. */
private class RecordingPdf : PdfRenderer {
    var pages = 0
    val text = StringBuilder()
    override fun render(pageWidth: Float, pageHeight: Float, draw: PdfCanvas.() -> Unit): ByteArray {
        object : PdfCanvas {
            override fun beginPage() {
                pages += 1
            }
            override fun measure(text: String, width: Float, style: PdfTextStyle) = 14f * text.lines().size
            override fun drawText(text: String, x: Float, y: Float, width: Float, style: PdfTextStyle) {
                this@RecordingPdf.text.appendLine(text)
            }
            override fun drawImage(data: ByteArray, x: Float, y: Float, maxWidth: Float, maxHeight: Float) = true
        }.draw()
        return text.toString().encodeToByteArray()
    }
}

/** English labels without loading resources. */
private object PlainLabels : ExportLabels {
    override suspend fun name() = "Name"
    override suspend fun passportId() = "Passport/ID"
    override suspend fun nationality() = "Nationality"
    override suspend fun checkIn() = "Check-in"
    override suspend fun checkOut() = "Check-out"
    override suspend fun guestReport(from: String, to: String) = "Guest report: $from – $to"
    override suspend fun passportIdWithNationality(id: String, nationality: String) = "Passport/ID: $id — $nationality"
    override suspend fun checkInCheckOut(checkIn: String, checkOut: String) = "Check-in: $checkIn  Check-out: $checkOut"
    override suspend fun checkInCheckOutNights(checkIn: String, checkOut: String, nights: Int) = "$checkIn $checkOut ($nights nights)"
    override suspend fun born(date: String) = "Born: $date"
    override suspend fun stayId(id: String) = "Stay ID: $id"
    override suspend fun inPersonFallback() = "In person"
    override suspend fun registrationPreparedTitle() = "Prepared"
    override suspend fun registrationPreparedNote(time: String) = "Prepared $time"
    override suspend fun registrationRegisteredTitle() = "Registered"
    override suspend fun registrationPartlyTitle() = "Partly"
    override suspend fun registrationSentNote(time: String) = "Sent $time"
    override suspend fun notRegisteredNote() = "Not registered note"
    override suspend fun registered() = "Registered"
    override suspend fun notRegisteredReason(reason: String) = "Not registered: $reason"
    override suspend fun unconfirmed() = "Unconfirmed"
    override suspend fun registrationNotCompletedTitle() = "Not completed"
    override suspend fun registrationAttemptedNote(time: String, reason: String) = "Attempted $time: $reason"
    override suspend fun answerLostNote() = "Answer lost"
    override suspend fun notConfirmed() = "Not confirmed"
    override suspend fun notRegistered() = "Not registered"
}

private class MemoryPreferences : KeyValueStore {
    private val values = mutableMapOf<String, Any>()
    override fun getInt(key: String) = values[key] as? Int
    override fun getBoolean(key: String) = values[key] as? Boolean
    override fun putInt(key: String, value: Int) = values.set(key, value)
    override fun putBoolean(key: String, value: Boolean) = values.set(key, value)
}

private class MemorySecureStore : SecureStore {
    private val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun set(key: String, value: String) = values.set(key, value)
    override fun remove(key: String) {
        values.remove(key)
    }
}

private fun testPlatform(fileSystem: FakeFileSystem, pdf: PdfRenderer) = Platform(
    fileSystem = fileSystem,
    cacheDirectory = "/cache".toPath(),
    temporaryDirectory = "/tmp".toPath().also { fileSystem.createDirectories(it) },
    preferences = MemoryPreferences(),
    secureStore = MemorySecureStore(),
    deviceOwner = object : DeviceOwnerCheck {
        override suspend fun confirm(reason: String) = false
    },
    connectivity = object : ConnectivityMonitor {
        override val isConnected = MutableStateFlow(true)
    },
    messages = object : MessageComposer {
        override val canSendText = false
        override suspend fun compose(recipient: String, body: String) = MessageResult.Failed
    },
    files = object : FileSharer {
        override fun share(path: Path, mimeType: String) {}
    },
    pdf = pdf,
    appVersion = "0.1.0",
    buildNumber = "1",
)

class ExportAndCacheTest {
    @Test
    fun csvPreservesUtf8AndEscapesFormulasQuotesAndNewlines() = runTest {
        val guest = Guest(fullName = "=SUM(1,2)\n\"José\"", identificador = "TEST", stayId = "1")
        val text = ExportService(RecordingPdf(), PlainLabels).csv(listOf(guest)).decodeToString()
        assertTrue(text.startsWith("﻿"))
        assertTrue("\"'=SUM(1,2)\n\"\"José\"\"\"" in text)
    }

    @Test
    fun pdfSpansPagesAndKeepsLastGuest() = runTest {
        val pdf = RecordingPdf()
        val guests = (0 until 40).map { fixtureGuest("TEST-$it") }
        ExportService(pdf, PlainLabels).pdf(guests, cubaAfternoon, cubaAfternoon)
        assertTrue(pdf.pages > 1)
        assertTrue("TEST-39" in pdf.text)
    }

    @Test
    fun registrationPdfMarksUncertainPeople() = runTest {
        val pdf = RecordingPdf()
        val entry = com.casero.cu.model.GuestRegistration(
            firstName = "TEST", lastName1 = "A", lastName2 = "", identificador = "X0000001", nationalityCode = "428",
            sex = "F", birthDate = cubaAfternoon, checkIn = cubaAfternoon, checkOut = cubaAfternoon, procedenciaCode = "428",
        )
        ExportService(pdf, PlainLabels).registrationPdf(
            ExportService.RegistrationRecord.Failed(listOf(entry), reason = "Timeout", uncertain = true),
        )
        assertTrue("Answer lost" in pdf.text)
        assertTrue("Not confirmed" in pdf.text)
    }

    @Test
    fun photoRetentionRemovesExpiredData() = runTest {
        val fileSystem = FakeFileSystem()
        val cache = PhotoCache(fileSystem, "/cache".toPath())
        cache.store(byteArrayOf(1, 2, 3), "https://example.invalid/test-photo")
        assertContentEquals(byteArrayOf(1, 2, 3), cache.data("https://example.invalid/test-photo"))
        assertNull(cache.data("https://example.invalid/test-photo", ttl = (-1).seconds))
        assertNull(cache.data("https://example.invalid/test-photo"))
    }

    @Test
    fun guestCacheRoundTripAndClearOnDisk() = runTest {
        val cache = GuestCache(FakeFileSystem(), "/cache".toPath())
        val guest = fixtureGuest()
        cache.save(listOf(guest), guest.checkIn, guest.checkOut)
        val restored = cache.load()
        assertEquals(guest.birthDate, restored?.guests?.first()?.birthDate)
        assertEquals(guest.checkIn, restored?.rangeStart)
        cache.clear()
        assertNull(cache.load())
    }

    @Test
    fun cacheClearRejectsAnOlderInFlightFetch() = runTest {
        val cache = GuestCache(FakeFileSystem(), "/cache".toPath())
        val generation = cache.generation
        cache.clear()
        cache.save(listOf(fixtureGuest()), generation = generation)
        assertNull(cache.load())
    }

    @Test
    fun companionSnapshotsAreKeptPerStay() = runTest {
        val cache = GuestCache(FakeFileSystem(), "/cache".toPath())
        cache.saveCompanions(listOf(fixtureGuest("C-1")), "STAY-1")
        cache.saveCompanions(listOf(fixtureGuest("C-2")), "STAY-2")
        assertEquals("C-1", cache.loadCompanions("STAY-1")?.companions?.first()?.identificador)
        cache.removeCompanions("STAY-1")
        assertNull(cache.loadCompanions("STAY-1"))
        assertNotNull(cache.loadCompanions("STAY-2"))
    }

    @Test
    fun singleDayExportIncludesMidnightAndRemovesTemporaryFile() = runTest {
        val fileSystem = FakeFileSystem()
        val platform = testPlatform(fileSystem, RecordingPdf())
        val settings = AppSettings(platform.preferences, platform.secureStore)
        settings.setCacheTTL(AppSettings.CacheTTL.Forever)
        val cache = GuestCache(fileSystem, platform.cacheDirectory)
        val photos = PhotoCache(fileSystem, platform.cacheDirectory)
        val model = SettingsViewModel(
            platform, settings, cache, photos, GuestPhotoService(StubPortalClient(), settings, photos),
            ExportService(platform.pdf, PlainLabels),
        )
        val guest = fixtureGuest()
        val day = PortalDates.day(guest.checkIn!!)
        model.exportFrom = day
        model.exportTo = day
        cache.save(listOf(guest), PortalDates.startOfDay(day), PortalDates.startOfDay(day))
        model.export()
        val file = model.exportedFile?.path
        assertNotNull(file, model.errorMessage?.toString())
        assertTrue("TEST-A" in fileSystem.read(file) { readUtf8() })
        model.removeExport()
        assertFalse(fileSystem.exists(file))
    }
}
