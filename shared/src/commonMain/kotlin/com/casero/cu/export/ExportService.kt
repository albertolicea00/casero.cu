package com.casero.cu.export

import com.casero.cu.model.Guest
import com.casero.cu.model.GuestRegistration
import com.casero.cu.model.PortalDates
import com.casero.cu.platform.PdfCanvas
import com.casero.cu.platform.PdfRenderer
import com.casero.cu.platform.PdfTextStyle
import kotlin.math.max
import kotlin.time.Instant

/** Exports are generated locally from the complete cached date range. */
class ExportService(private val renderer: PdfRenderer, private val labels: ExportLabels) {

    enum class Format { Csv, Pdf }

    /**
     * What a registration PDF records. Every variant is a trace for the host;
     * only the stay IDs returned by the portal prove a registration.
     */
    sealed interface RegistrationRecord {
        /** Saved from the review sheet, before sending. */
        data class Prepared(val entries: List<GuestRegistration>) : RegistrationRecord

        /** The portal's answer, one row per person (status and stay ID). */
        data class Submitted(val guests: List<Guest>) : RegistrationRecord

        /** Sending failed or its answer was lost; the people may not be registered. */
        data class Failed(val entries: List<GuestRegistration>, val reason: String, val uncertain: Boolean) :
            RegistrationRecord
    }

    private data class RegistrationRow(
        val name: String,
        val identificador: String,
        val nationality: String?,
        val birthDate: Instant?,
        val checkIn: Instant?,
        val checkOut: Instant?,
        val status: String?,
        val stayId: String?,
    )

    suspend fun csv(guests: List<Guest>): ByteArray {
        val header = listOf(labels.name(), labels.passportId(), labels.nationality(), labels.checkIn(), labels.checkOut())
        val lines = mutableListOf(header.joinToString(",") { csvEscape(it) })
        for (guest in guests) {
            lines += listOf(
                guest.fullName,
                guest.identificador,
                guest.nationality ?: "",
                guest.checkIn?.let(PortalDates::exportFormat) ?: "",
                guest.checkOut?.let(PortalDates::exportFormat) ?: "",
            ).joinToString(",") { csvEscape(it) }
        }
        // A BOM lets Excel detect accented UTF-8 names correctly.
        return ("﻿" + lines.joinToString("\r\n")).encodeToByteArray()
    }

    suspend fun pdf(guests: List<Guest>, from: Instant, to: Instant, photos: Map<String, ByteArray> = emptyMap()): ByteArray {
        val title = labels.guestReport(PortalDates.exportFormat(from), PortalDates.exportFormat(to))
        val rows = guests.map { guest ->
            guest to listOf(
                guest.fullName,
                labels.passportIdWithNationality(guest.identificador, guest.nationality ?: ""),
                labels.checkInCheckOut(
                    guest.checkIn?.let(PortalDates::exportFormat) ?: "—",
                    guest.checkOut?.let(PortalDates::exportFormat) ?: "—",
                ),
            ).joinToString("\n")
        }
        return renderer.render(PAGE_WIDTH, PAGE_HEIGHT) {
            val width = PAGE_WIDTH - 2 * MARGIN
            var y = MARGIN
            fun newPage() {
                beginPage()
                val titleHeight = measure(title, width, TITLE)
                drawText(title, MARGIN, MARGIN, width, TITLE)
                y = MARGIN + titleHeight + 16
            }
            newPage()
            for ((guest, text) in rows) {
                val photo = photos[guest.identificador]
                val photoWidth = if (photo == null) 0f else 48f
                val textWidth = width - photoWidth
                val textHeight = measure(text, textWidth, ROW)
                val rowHeight = max(if (photo == null) 0f else 40f, textHeight)
                if (y + rowHeight > PAGE_HEIGHT - MARGIN) newPage()
                if (photo != null) drawImage(photo, MARGIN, y, 40f, 40f)
                drawText(text, MARGIN + photoWidth, y, textWidth, ROW)
                y += rowHeight + 12
            }
        }
    }

    suspend fun registrationPdf(
        record: RegistrationRecord,
        photos: Map<String, ByteArray> = emptyMap(),
        generatedAt: Instant = PortalDates.now(),
    ): ByteArray {
        val whenText = PortalDates.timestampFormat(generatedAt)
        val inPerson = labels.inPersonFallback()

        fun row(entry: GuestRegistration, status: String?) = RegistrationRow(
            name = "${entry.firstName} ${entry.middleName} ${entry.lastName1} ${entry.lastName2}",
            identificador = entry.identificador,
            nationality = entry.nationalityName,
            birthDate = entry.birthDate,
            checkIn = entry.checkIn,
            checkOut = entry.checkOut,
            status = status,
            stayId = null,
        )

        val title: String
        val notes: List<String>
        val rows: List<RegistrationRow>
        when (record) {
            is RegistrationRecord.Prepared -> {
                title = labels.registrationPreparedTitle()
                notes = listOf(labels.registrationPreparedNote(whenText))
                rows = record.entries.map { row(it, null) }
            }
            is RegistrationRecord.Submitted -> {
                val rejected = record.guests.filter { it.response != "OK" }
                title = if (rejected.isEmpty()) labels.registrationRegisteredTitle() else labels.registrationPartlyTitle()
                notes = buildList {
                    add(labels.registrationSentNote(whenText))
                    if (rejected.isNotEmpty()) {
                        add(labels.notRegisteredNote())
                        add(inPerson)
                    }
                }
                rows = record.guests.map { guest ->
                    RegistrationRow(
                        name = guest.fullName.replace("*", " "),
                        identificador = guest.identificador,
                        nationality = guest.nationality,
                        birthDate = guest.birthDate,
                        checkIn = guest.checkIn,
                        checkOut = guest.checkOut,
                        status = if (guest.response == "OK") {
                            labels.registered()
                        } else {
                            labels.notRegisteredReason(guest.response ?: labels.unconfirmed())
                        },
                        stayId = guest.stayId.ifEmpty { null },
                    )
                }
            }
            is RegistrationRecord.Failed -> {
                title = labels.registrationNotCompletedTitle()
                notes = buildList {
                    add(labels.registrationAttemptedNote(whenText, record.reason))
                    if (record.uncertain) add(labels.answerLostNote())
                    add(inPerson)
                }
                val status = if (record.uncertain) labels.notConfirmed() else labels.notRegistered()
                rows = record.entries.map { row(it, status) }
            }
        }

        val details = rows.map { item ->
            buildList {
                item.status?.let(::add)
                item.stayId?.let { add(labels.stayId(it)) }
                add(labels.passportIdWithNationality(item.identificador, item.nationality ?: ""))
                item.birthDate?.let { add(labels.born(PortalDates.exportFormat(it))) }
                if (item.checkIn != null && item.checkOut != null) {
                    val nights = max(0, PortalDates.daysBetween(item.checkIn, item.checkOut))
                    add(
                        labels.checkInCheckOutNights(
                            PortalDates.exportFormat(item.checkIn),
                            PortalDates.exportFormat(item.checkOut),
                            nights,
                        ),
                    )
                }
            }.joinToString("\n")
        }
        val noteText = notes.joinToString("\n\n")

        return renderer.render(PAGE_WIDTH, PAGE_HEIGHT) {
            val width = PAGE_WIDTH - 2 * MARGIN
            var y = MARGIN
            fun newPage() {
                beginPage()
                val titleHeight = measure(title, width, TITLE)
                drawText(title, MARGIN, MARGIN, width, TITLE)
                y = MARGIN + titleHeight + 6
                val noteHeight = measure(noteText, width, NOTE)
                drawText(noteText, MARGIN, y, width, NOTE)
                y += noteHeight + 18
            }
            newPage()
            for ((item, detail) in rows.zip(details)) {
                val photo = photos[item.identificador]
                val photoWidth = if (photo == null) 0f else 56f
                val textWidth = width - photoWidth
                val name = item.name.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
                val nameHeight = measure(name, textWidth, NAME)
                val detailsHeight = measure(detail, textWidth, ROW)
                val rowHeight = max(if (photo == null) 0f else 48f, nameHeight + 2 + detailsHeight)
                if (y + rowHeight > PAGE_HEIGHT - MARGIN) newPage()
                if (photo != null) drawImage(photo, MARGIN, y, 48f, 48f)
                drawText(name, MARGIN + photoWidth, y, textWidth, NAME)
                drawText(detail, MARGIN + photoWidth, y + nameHeight + 2, textWidth, ROW)
                y += rowHeight + 14
            }
        }
    }

    companion object {
        private const val PAGE_WIDTH = 612f
        private const val PAGE_HEIGHT = 792f
        private const val MARGIN = 36f
        private val TITLE = PdfTextStyle(16f, bold = true)
        private val NAME = PdfTextStyle(12f, bold = true)
        private val ROW = PdfTextStyle(11f)
        private val NOTE = PdfTextStyle(10f, secondary = true)

        /** File name for a registration PDF, stamped in Cuba time. */
        fun registrationFileName(generatedAt: Instant = PortalDates.now()) =
            "registration-${PortalDates.fileStamp(generatedAt)}.pdf"

        internal fun csvEscape(raw: String): String {
            // Quoting alone does not prevent Excel from interpreting formulas.
            val first = raw.trim().firstOrNull()
            val field = if (first != null && first in "=+-@") "'$raw" else raw
            if (!field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) return field
            return "\"${field.replace("\"", "\"\"")}\""
        }
    }
}

/** Localized texts of the exports. `ResourceExportLabels` reads them from the app's strings. */
interface ExportLabels {
    suspend fun name(): String
    suspend fun passportId(): String
    suspend fun nationality(): String
    suspend fun checkIn(): String
    suspend fun checkOut(): String
    suspend fun guestReport(from: String, to: String): String
    suspend fun passportIdWithNationality(id: String, nationality: String): String
    suspend fun checkInCheckOut(checkIn: String, checkOut: String): String
    suspend fun checkInCheckOutNights(checkIn: String, checkOut: String, nights: Int): String
    suspend fun born(date: String): String
    suspend fun stayId(id: String): String
    suspend fun inPersonFallback(): String
    suspend fun registrationPreparedTitle(): String
    suspend fun registrationPreparedNote(time: String): String
    suspend fun registrationRegisteredTitle(): String
    suspend fun registrationPartlyTitle(): String
    suspend fun registrationSentNote(time: String): String
    suspend fun notRegisteredNote(): String
    suspend fun registered(): String
    suspend fun notRegisteredReason(reason: String): String
    suspend fun unconfirmed(): String
    suspend fun registrationNotCompletedTitle(): String
    suspend fun registrationAttemptedNote(time: String, reason: String): String
    suspend fun answerLostNote(): String
    suspend fun notConfirmed(): String
    suspend fun notRegistered(): String
}
