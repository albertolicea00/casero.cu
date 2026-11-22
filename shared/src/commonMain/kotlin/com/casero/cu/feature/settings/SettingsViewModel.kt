package com.casero.cu.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.casero.cu.app.GuestPhotoService
import com.casero.cu.export.ExportService
import com.casero.cu.model.Guest
import com.casero.cu.model.PortalDates
import com.casero.cu.model.PortalDates.plusDays
import com.casero.cu.persistence.AppSettings
import com.casero.cu.persistence.GuestCache
import com.casero.cu.persistence.PhotoCache
import com.casero.cu.platform.Platform
import com.casero.cu.resources.Res
import com.casero.cu.resources.cached_data_has_expired_reload_guests_before
import com.casero.cu.resources.check_out_range_end_must_be_after_its_start
import com.casero.cu.resources.could_not_build_export_x
import com.casero.cu.resources.nothing_cached_yet_reload_the_guests_tab_first
import com.casero.cu.resources.some_photos_could_not_be_loaded_the_export
import com.casero.cu.resources.the_selected_dates_are_outside_the_cached_range
import com.casero.cu.ui.UiText
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import okio.Path

@OptIn(ExperimentalUuidApi::class)
class SettingsViewModel(
    private val platform: Platform,
    private val settings: AppSettings,
    private val cache: GuestCache,
    private val photoCache: PhotoCache,
    private val photos: GuestPhotoService,
    private val exports: ExportService,
    private val now: () -> Instant = PortalDates::now,
) {
    data class ExportedFile(val path: Path, val mimeType: String)

    /** Number of cached guests and when they were fetched, or `null` without a cache. */
    data class CacheSummary(val count: Int, val fetchedAt: Instant)

    var exportFrom by mutableStateOf(PortalDates.today(now()).minus(DatePeriod(months = 1)))
    var exportTo by mutableStateOf(PortalDates.today(now()))
    var exportFormat by mutableStateOf(ExportService.Format.Csv)
    var includePhotosInPDF by mutableStateOf(false)
    var isExporting by mutableStateOf(false)
        private set
    var exportedFile by mutableStateOf<ExportedFile?>(null)
    var errorMessage by mutableStateOf<UiText?>(null)
        private set
    var noticeMessage by mutableStateOf<UiText?>(null)
        private set
    var cacheSummary by mutableStateOf<CacheSummary?>(null)
        private set

    private var lastExport: Path? = null

    fun removeExport() {
        lastExport?.let { platform.fileSystem.delete(it, mustExist = false) }
        exportedFile = null
        lastExport = null
    }

    suspend fun refreshCacheSummary() {
        cacheSummary = cache.load()?.let { CacheSummary(it.guests.size, it.fetchedAt) }
    }

    suspend fun clearCache() {
        cache.clear()
        photoCache.clear()
        refreshCacheSummary()
    }

    suspend fun export() {
        if (exportTo < exportFrom) {
            errorMessage = UiText.of(Res.string.check_out_range_end_must_be_after_its_start)
            return
        }
        isExporting = true
        errorMessage = null
        noticeMessage = null
        try {
            val snapshot = cache.load()
            if (snapshot == null) {
                errorMessage = UiText.of(Res.string.nothing_cached_yet_reload_the_guests_tab_first)
                return
            }
            if (cache.isStale(snapshot.fetchedAt, settings.cacheTTL.value.interval)) {
                clearCache()
                errorMessage = UiText.of(Res.string.cached_data_has_expired_reload_guests_before)
                return
            }
            val start = PortalDates.startOfDay(exportFrom)
            val end = PortalDates.startOfDay(exportTo.plusDays(1))
            val cachedFrom = snapshot.rangeStart
            val cachedTo = snapshot.rangeEnd
            if (cachedFrom != null && cachedTo != null &&
                (exportFrom < PortalDates.day(cachedFrom) || exportTo > PortalDates.day(cachedTo))
            ) {
                errorMessage = UiText.of(Res.string.the_selected_dates_are_outside_the_cached_range)
                return
            }
            val range = snapshot.guests.filter { guest ->
                val checkIn = guest.checkIn ?: return@filter false
                checkIn >= start && checkIn < end
            }
            val (data, extension, mimeType) = when (exportFormat) {
                ExportService.Format.Csv -> Triple(exports.csv(range), "csv", "text/csv")
                ExportService.Format.Pdf -> {
                    val images = if (includePhotosInPDF) fetchPhotos(range) else emptyMap()
                    Triple(exports.pdf(range, start, PortalDates.startOfDay(exportTo), images), "pdf", "application/pdf")
                }
            }
            val file = platform.temporaryDirectory / "guests-${Uuid.random()}.$extension"
            platform.fileSystem.write(file) { write(data) }
            removeExport()
            lastExport = file
            exportedFile = ExportedFile(file, mimeType)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = UiText.of(Res.string.could_not_build_export_x, error.message ?: error.toString())
        } finally {
            isExporting = false
        }
    }

    /** Best-effort: skips any guest whose photo fails to download rather than failing the whole export. */
    private suspend fun fetchPhotos(guests: List<Guest>): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        for (guest in guests) {
            try {
                result[guest.identificador] = photos.image(guest)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
        if (result.size < guests.size) noticeMessage = UiText.of(Res.string.some_photos_could_not_be_loaded_the_export)
        return result
    }
}
