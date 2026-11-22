package com.casero.cu.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.casero.cu.export.ExportService
import com.casero.cu.export.ResourceExportLabels
import com.casero.cu.network.CaseroClient
import com.casero.cu.network.PinnedCertificate
import com.casero.cu.network.portalEngine
import com.casero.cu.persistence.AppSettings
import com.casero.cu.persistence.GuestCache
import com.casero.cu.persistence.PhotoCache
import com.casero.cu.platform.Platform
import com.casero.cu.platform.SecureStore
import com.casero.cu.resources.Res
import com.casero.cu.resources.sign_in_to_the_app_session_with_your_saved
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** Process-wide state: the portal client, session status, and shared services. */
class AppState(val platform: Platform) {
    var isSignedIn by mutableStateOf(false)
    var hasCachedGuests by mutableStateOf(false)
    var isSigningOut by mutableStateOf(false)
        private set

    /**
     * Set once a lazily-detected session expiry could not be silently recovered.
     * Drives the "session lost, tap to sign in" banner instead of an immediate bounce.
     */
    var sessionExpiredBanner by mutableStateOf(false)

    /** Bumped after the portal accepts a registration or a stay change so Guests reloads. */
    var registrationsVersion by mutableIntStateOf(0)
        private set

    val client = CaseroClient(portalEngine(PinnedCertificate.Bundled), PinnedCertificate.Bundled)
    val settings = AppSettings(platform.preferences, platform.secureStore)
    val guestCache = GuestCache(platform.fileSystem, platform.cacheDirectory)
    val photoCache = PhotoCache(platform.fileSystem, platform.cacheDirectory)
    val photos = GuestPhotoService(client, settings, photoCache)
    val exports = ExportService(platform.pdf, ResourceExportLabels)
    val secureStore: SecureStore get() = platform.secureStore

    private val scope = CoroutineScope(SupervisorJob())
    private var recovery: CompletableDeferred<Boolean>? = null
    private var recoveryJob: Job? = null

    init {
        client.recoverSession = { recoverSession() }
    }

    private suspend fun recoverSession(): Boolean {
        recovery?.let { return it.await() }
        if (!settings.autoReauth.value || !settings.rememberCredentials.value) return false
        val user = secureStore.get(SecureStore.USERNAME) ?: return false
        val password = secureStore.get(SecureStore.PASSWORD) ?: return false
        val result = CompletableDeferred<Boolean>()
        recovery = result
        // Concurrent expiries share one recovery, so the user sees one prompt.
        recoveryJob = scope.launch {
            result.complete(
                try {
                    confirmDeviceOwner() && run {
                        client.login(user, password)
                        true
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    false
                },
            )
        }
        recoveryJob?.invokeOnCompletion { if (!result.isCompleted) result.complete(false) }
        return try {
            result.await()
        } finally {
            recovery = null
            recoveryJob = null
        }
    }

    /**
     * Saved credentials sign in to an official account in the host's name, so
     * every use requires the device owner (biometrics or passcode).
     * Without a device lock there is no silent sign-in. See SECURITY.md.
     */
    private suspend fun confirmDeviceOwner(): Boolean =
        platform.deviceOwner.confirm(getString(Res.string.sign_in_to_the_app_session_with_your_saved))

    suspend fun restoreLocalData() {
        val snapshot = guestCache.load()
        if (snapshot == null) {
            hasCachedGuests = false
            return
        }
        if (guestCache.isStale(snapshot.fetchedAt, settings.cacheTTL.value.interval)) {
            guestCache.clear()
            photoCache.clear()
            hasCachedGuests = false
        } else {
            hasCachedGuests = snapshot.guests.isNotEmpty()
        }
    }

    /**
     * Silent sign-in at launch with the saved credentials, gated by the same
     * settings as expiry recovery. On failure the sign-in banner stays.
     */
    suspend fun restoreSession() {
        if (isSignedIn || !recoverSession()) return
        isSignedIn = true
    }

    fun guestsDidChange() {
        registrationsVersion += 1
    }

    /**
     * Called by any feature that catches `PortalError.SessionExpired`.
     * Lazy by design: no background polling, this only reacts to a real failed call.
     */
    fun reportSessionExpired() {
        isSignedIn = false
        sessionExpiredBanner = true
    }

    fun didSignIn() {
        sessionExpiredBanner = false
        isSignedIn = true
    }

    suspend fun signOutCompletely() {
        if (isSigningOut) return
        isSigningOut = true
        try {
            hasCachedGuests = false
            recoveryJob?.cancel()
            recoveryJob = null
            isSignedIn = false
            client.signOut()
            guestCache.clear()
            photoCache.clear()
            secureStore.clearAll()
            sessionExpiredBanner = false
        } finally {
            isSigningOut = false
        }
    }
}
