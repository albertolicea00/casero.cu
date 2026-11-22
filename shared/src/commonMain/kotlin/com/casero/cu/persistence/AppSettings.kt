package com.casero.cu.persistence

import com.casero.cu.platform.KeyValueStore
import com.casero.cu.platform.SecureStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-facing preferences. Everything here is local device configuration —
 * never portal data. Keys match the native iOS app so its settings carry over.
 */
class AppSettings(private val store: KeyValueStore, private val secureStore: SecureStore) {

    enum class CacheTTL(val seconds: Int) {
        OneHour(3600),
        SixHours(21600),
        OneDay(86400),
        ThreeDays(259200),
        OneWeek(604800),
        OneMonth(2_592_000),
        ThreeMonths(7_776_000),
        SixMonths(15_552_000),
        TwelveMonths(31_536_000),
        Forever(0),
        ;

        /** `null` means "forever" (never considered stale). */
        val interval: Duration? get() = if (this == Forever) null else seconds.seconds

        companion object {
            fun of(seconds: Int?): CacheTTL? = entries.firstOrNull { it.seconds == seconds }
        }
    }

    private object Keys {
        const val CACHE_TTL = "settings.cacheTTL"
        const val SAVE_IMAGES = "settings.saveImages"
        const val AUTO_REAUTH = "settings.autoReauth"
        const val REMEMBER_CREDENTIALS = "settings.rememberCredentials"
        const val DEFAULT_GUEST_RANGE_DAYS = "settings.defaultGuestRangeDays"
    }

    private val _cacheTTL = MutableStateFlow(CacheTTL.of(store.getInt(Keys.CACHE_TTL)) ?: CacheTTL.OneWeek)
    val cacheTTL: StateFlow<CacheTTL> = _cacheTTL.asStateFlow()

    /** When `false`, guest photos are never written to the on-disk cache. */
    private val _saveImages = MutableStateFlow(store.getBoolean(Keys.SAVE_IMAGES) ?: true)
    val saveImages: StateFlow<Boolean> = _saveImages.asStateFlow()

    /**
     * When `true`, an expired session is retried silently using the saved
     * password before falling back to the "session lost, tap to sign in" banner.
     */
    private val _autoReauth = MutableStateFlow(store.getBoolean(Keys.AUTO_REAUTH) ?: false)
    val autoReauth: StateFlow<Boolean> = _autoReauth.asStateFlow()

    private val _rememberCredentials = MutableStateFlow(store.getBoolean(Keys.REMEMBER_CREDENTIALS) ?: true)
    val rememberCredentials: StateFlow<Boolean> = _rememberCredentials.asStateFlow()

    private val _defaultGuestRangeDays = MutableStateFlow(
        store.getInt(Keys.DEFAULT_GUEST_RANGE_DAYS)?.takeIf { it in GUEST_RANGE_OPTIONS } ?: 90,
    )
    val defaultGuestRangeDays: StateFlow<Int> = _defaultGuestRangeDays.asStateFlow()

    fun setCacheTTL(value: CacheTTL) {
        _cacheTTL.value = value
        store.putInt(Keys.CACHE_TTL, value.seconds)
    }

    fun setSaveImages(value: Boolean) {
        _saveImages.value = value
        store.putBoolean(Keys.SAVE_IMAGES, value)
    }

    fun setAutoReauth(value: Boolean) {
        _autoReauth.value = value
        store.putBoolean(Keys.AUTO_REAUTH, value)
    }

    fun setRememberCredentials(value: Boolean) {
        _rememberCredentials.value = value
        store.putBoolean(Keys.REMEMBER_CREDENTIALS, value)
        if (!value) secureStore.clearAll()
    }

    fun setDefaultGuestRangeDays(value: Int) {
        _defaultGuestRangeDays.value = value
        store.putInt(Keys.DEFAULT_GUEST_RANGE_DAYS, value)
    }

    companion object {
        val GUEST_RANGE_OPTIONS = listOf(7, 15, 30, 60, 90, 180, 365)
    }
}
