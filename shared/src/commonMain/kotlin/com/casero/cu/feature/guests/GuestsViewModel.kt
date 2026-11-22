package com.casero.cu.feature.guests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.casero.cu.model.Guest
import com.casero.cu.model.PortalDates
import com.casero.cu.model.PortalDates.plusDays
import com.casero.cu.network.PortalError
import com.casero.cu.network.PortalServing
import com.casero.cu.persistence.AppSettings
import com.casero.cu.persistence.GuestCache
import com.casero.cu.persistence.PhotoCache
import com.casero.cu.resources.Res
import com.casero.cu.resources.the_end_date_must_be_on_or_after_the_start_date
import com.casero.cu.ui.UiText
import com.casero.cu.ui.toUiText
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

class GuestsViewModel(
    private val client: PortalServing,
    private val cache: GuestCache,
    private val photoCache: PhotoCache,
    private val settings: AppSettings,
    private val now: () -> Instant = PortalDates::now,
) {
    enum class Segment { Active, History }

    enum class SortOrder { NewestFirst, OldestFirst }

    var from by mutableStateOf(PortalDates.today(now()).plusDays(-settings.defaultGuestRangeDays.value))
    var to by mutableStateOf(PortalDates.today(now()))
    var guests by mutableStateOf<List<Guest>>(emptyList())
    var segment by mutableStateOf(Segment.Active)
    var sortOrder by mutableStateOf(SortOrder.NewestFirst)
    var searchText by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var isOffline by mutableStateOf(false)
        private set
    var lastFetched by mutableStateOf<Instant?>(null)
        private set
    var errorMessage by mutableStateOf<UiText?>(null)
        private set
    var isSessionExpired by mutableStateOf(false)
        private set

    fun applyDefaultRange() {
        val today = PortalDates.today(now())
        from = today.plusDays(-settings.defaultGuestRangeDays.value)
        to = today
    }

    val filteredGuests: List<Guest>
        get() {
            val instant = now()
            val bySegment = guests.filter { guest ->
                when (segment) {
                    Segment.Active -> guest.isActive(instant)
                    Segment.History -> (guest.checkOut ?: Instant.DISTANT_FUTURE) < PortalDates.startOfDay(instant)
                }
            }
            val needle = fold(searchText.trim())
            val bySearch = if (needle.isEmpty()) {
                bySegment
            } else {
                bySegment.filter { needle in fold(it.fullName) || needle in fold(it.identificador) }
            }
            if (segment == Segment.Active) {
                // Active stays are grouped by departure day: whoever leaves soonest first.
                return bySearch.sortedWith(
                    compareBy<Guest> { it.checkOut ?: Instant.DISTANT_FUTURE }.thenBy { it.fullName },
                )
            }
            val byCheckIn = compareBy<Guest> { it.checkIn ?: Instant.DISTANT_PAST }
            return bySearch.sortedWith(if (sortOrder == SortOrder.NewestFirst) byCheckIn.reversed() else byCheckIn)
        }

    /** Loads from cache first for instant offline display. */
    suspend fun loadFromCache() {
        val snapshot = cache.load() ?: return
        if (cache.isStale(snapshot.fetchedAt, settings.cacheTTL.value.interval)) {
            cache.clear()
            guests = emptyList()
            lastFetched = null
            photoCache.clear()
            return
        }
        snapshot.rangeStart?.let { from = PortalDates.day(it) }
        snapshot.rangeEnd?.let { to = PortalDates.day(it) }
        guests = snapshot.guests
        lastFetched = snapshot.fetchedAt
        isOffline = true
    }

    /** Fetches fresh data from the portal. Called on pull-to-refresh / the reload button. */
    suspend fun reload() {
        if (isLoading) return
        if (from > to) {
            errorMessage = UiText.of(Res.string.the_end_date_must_be_on_or_after_the_start_date)
            return
        }
        isLoading = true
        errorMessage = null
        val generation = cache.generation
        try {
            val start = PortalDates.startOfDay(from)
            val end = PortalDates.startOfDay(to)
            val fresh = client.listGuests(start, end, pageSize = 100, page = 1)
            guests = fresh
            lastFetched = now()
            isOffline = false
            if (generation != cache.generation) {
                guests = emptyList()
                return
            }
            cache.save(fresh, start, end, generation)
        } catch (error: CancellationException) {
            throw error
        } catch (_: PortalError.SessionExpired) {
            isSessionExpired = true
        } catch (error: Exception) {
            // Keep whatever cached data is already on screen; surface the error alongside it.
            errorMessage = error.toUiText()
            isOffline = guests.isNotEmpty()
        } finally {
            isLoading = false
        }
    }

    fun acknowledgeSessionExpired() {
        isSessionExpired = false
    }

    companion object {
        /** Whole days from today to the guest's check-out, in Cuba time. `null` without a check-out date. */
        fun daysUntilCheckOut(guest: Guest, now: Instant = PortalDates.now()): Int? =
            guest.checkOut?.let { PortalDates.daysBetween(now, it) }

        /** Case- and accent-insensitive search key. */
        internal fun fold(text: String): String = buildString {
            for (char in text.lowercase()) append(ACCENTS[char] ?: char)
        }

        private val ACCENTS = mapOf(
            'á' to 'a', 'à' to 'a', 'â' to 'a', 'ä' to 'a', 'ã' to 'a',
            'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
            'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
            'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ö' to 'o', 'õ' to 'o',
            'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
            'ñ' to 'n', 'ç' to 'c',
        )
    }
}

