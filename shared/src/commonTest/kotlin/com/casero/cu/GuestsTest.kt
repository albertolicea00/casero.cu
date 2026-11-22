package com.casero.cu

import com.casero.cu.feature.guests.GuestsViewModel
import com.casero.cu.model.Guest
import com.casero.cu.model.PortalDates
import com.casero.cu.persistence.AppSettings
import com.casero.cu.persistence.GuestCache
import com.casero.cu.persistence.PhotoCache
import com.casero.cu.platform.KeyValueStore
import com.casero.cu.platform.SecureStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class MemoryStore : KeyValueStore {
    val values = mutableMapOf<String, Any>()
    override fun getInt(key: String) = values[key] as? Int
    override fun getBoolean(key: String) = values[key] as? Boolean
    override fun putInt(key: String, value: Int) = values.set(key, value)
    override fun putBoolean(key: String, value: Boolean) = values.set(key, value)
}

class MemorySecrets : SecureStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun set(key: String, value: String) = values.set(key, value)
    override fun remove(key: String) {
        values.remove(key)
    }
}

class GuestsTest {
    private val day = 1.days
    private fun guest(name: String, id: String, checkIn: Instant, checkOut: Instant) =
        Guest(fullName = name, identificador = id, checkIn = checkIn, checkOut = checkOut, stayId = id)

    private fun model(client: StubPortalClient = StubPortalClient(), now: Instant = cubaAfternoon): GuestsViewModel {
        val files = FakeFileSystem()
        val settings = AppSettings(MemoryStore(), MemorySecrets())
        return GuestsViewModel(
            client, GuestCache(files, "/c".toPath()) { now }, PhotoCache(files, "/c".toPath()), settings, { now },
        )
    }

    @Test
    fun activeAndHistorySplitOnCubaDays() {
        val model = model()
        val leavesToday = guest("Ana", "A", cubaAfternoon - day * 3, PortalDates.startOfDay(cubaAfternoon))
        val leftYesterday = guest("Beto", "B", cubaAfternoon - day * 5, cubaAfternoon - day)
        val notYetArrived = guest("Cris", "C", cubaAfternoon + day * 2, cubaAfternoon + day * 4)
        model.guests = listOf(leavesToday, leftYesterday, notYetArrived)
        assertEquals(listOf("A"), model.filteredGuests.map { it.identificador })
        model.segment = GuestsViewModel.Segment.History
        assertEquals(listOf("B"), model.filteredGuests.map { it.identificador })
    }

    @Test
    fun activeGuestsAreSortedBySoonestDeparture() {
        val model = model()
        model.guests = listOf(
            guest("Zoe", "Z", cubaAfternoon - day, cubaAfternoon + day * 5),
            guest("Ana", "A", cubaAfternoon - day, cubaAfternoon + day),
            guest("Beto", "B", cubaAfternoon - day, cubaAfternoon + day),
        )
        assertEquals(listOf("A", "B", "Z"), model.filteredGuests.map { it.identificador })
    }

    @Test
    fun historyFollowsTheSortOrder() {
        val model = model()
        model.segment = GuestsViewModel.Segment.History
        model.guests = listOf(
            guest("Old", "O", cubaAfternoon - day * 30, cubaAfternoon - day * 25),
            guest("New", "N", cubaAfternoon - day * 10, cubaAfternoon - day * 5),
        )
        assertEquals(listOf("N", "O"), model.filteredGuests.map { it.identificador })
        model.sortOrder = GuestsViewModel.SortOrder.OldestFirst
        assertEquals(listOf("O", "N"), model.filteredGuests.map { it.identificador })
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndMatchesDocuments() {
        val model = model()
        model.guests = listOf(
            guest("José Peña", "X0000001", cubaAfternoon - day, cubaAfternoon + day),
            guest("Ana Diaz", "Y0000002", cubaAfternoon - day, cubaAfternoon + day),
        )
        model.searchText = "jose pena"
        assertEquals(listOf("X0000001"), model.filteredGuests.map { it.identificador })
        model.searchText = "y00"
        assertEquals(listOf("Y0000002"), model.filteredGuests.map { it.identificador })
    }

    @Test
    fun reloadSavesTheRangeAndCacheRestoresIt() = runTest {
        val client = StubPortalClient().apply { results = listOf(fixtureGuest()) }
        val files = FakeFileSystem()
        val settings = AppSettings(MemoryStore(), MemorySecrets())
        val cache = GuestCache(files, "/c".toPath()) { cubaAfternoon }
        val first = GuestsViewModel(client, cache, PhotoCache(files, "/c".toPath()), settings, { cubaAfternoon })
        first.reload()
        assertFalse(first.isOffline)

        val second = GuestsViewModel(client, cache, PhotoCache(files, "/c".toPath()), settings, { cubaAfternoon })
        second.loadFromCache()
        assertTrue(second.isOffline)
        assertEquals(first.from, second.from)
        assertEquals(listOf("TEST-A"), second.guests.map { it.identificador })
    }

    @Test
    fun expiredCacheIsDroppedOnLoad() = runTest {
        val files = FakeFileSystem()
        val settings = AppSettings(MemoryStore(), MemorySecrets())
        GuestCache(files, "/c".toPath()) { cubaAfternoon - day * 30 }.save(listOf(fixtureGuest()))
        val cache = GuestCache(files, "/c".toPath()) { cubaAfternoon }
        val model = GuestsViewModel(StubPortalClient(), cache, PhotoCache(files, "/c".toPath()), settings, { cubaAfternoon })
        model.loadFromCache() // default retention is one week
        assertTrue(model.guests.isEmpty())
        assertEquals(null, cache.load())
    }

    @Test
    fun invertedRangeIsRefusedWithoutCallingThePortal() = runTest {
        val client = StubPortalClient()
        val model = model(client)
        model.from = model.to.let { kotlinx.datetime.LocalDate.fromEpochDays(it.toEpochDays() + 1) }
        model.reload()
        assertNotNull(model.errorMessage)
        assertEquals(0, client.submissions)
    }
}

class SettingsTest {
    @Test
    fun defaultsMatchTheIosApp() {
        val settings = AppSettings(MemoryStore(), MemorySecrets())
        assertEquals(AppSettings.CacheTTL.OneWeek, settings.cacheTTL.value)
        assertTrue(settings.saveImages.value)
        assertFalse(settings.autoReauth.value, "Silent sign-in is opt-in")
        assertTrue(settings.rememberCredentials.value)
        assertEquals(90, settings.defaultGuestRangeDays.value)
    }

    @Test
    fun valuesAreStoredUnderTheIosKeys() {
        val store = MemoryStore()
        AppSettings(store, MemorySecrets()).apply {
            setCacheTTL(AppSettings.CacheTTL.Forever)
            setAutoReauth(true)
            setDefaultGuestRangeDays(30)
        }
        assertEquals(0, store.values["settings.cacheTTL"])
        assertEquals(true, store.values["settings.autoReauth"])
        val reopened = AppSettings(store, MemorySecrets())
        assertEquals(AppSettings.CacheTTL.Forever, reopened.cacheTTL.value)
        assertEquals(30, reopened.defaultGuestRangeDays.value)
    }

    @Test
    fun forgettingCredentialsDeletesThem() {
        val secrets = MemorySecrets().apply {
            set(SecureStore.USERNAME, "host")
            set(SecureStore.PASSWORD, "Secret1!") // pragma: allowlist secret (test fixture)
        }
        AppSettings(MemoryStore(), secrets).setRememberCredentials(false)
        assertTrue(secrets.values.isEmpty())
    }

    @Test
    fun unknownStoredValuesFallBackToDefaults() {
        val store = MemoryStore().apply {
            values["settings.cacheTTL"] = 42
            values["settings.defaultGuestRangeDays"] = 11
        }
        val settings = AppSettings(store, MemorySecrets())
        assertEquals(AppSettings.CacheTTL.OneWeek, settings.cacheTTL.value)
        assertEquals(90, settings.defaultGuestRangeDays.value)
    }
}
