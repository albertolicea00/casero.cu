package com.casero.cu.platform

import kotlinx.coroutines.flow.StateFlow
import okio.FileSystem
import okio.Path

/** Local key-value preferences (`SharedPreferences` / `UserDefaults`). Never portal data. */
interface KeyValueStore {
    fun getInt(key: String): Int?
    fun getBoolean(key: String): Boolean?
    fun putInt(key: String, value: Int)
    fun putBoolean(key: String, value: Boolean)
}

/**
 * Device-bound storage for the host's own portal credentials: Android Keystore,
 * iOS Keychain `WhenUnlockedThisDeviceOnly`. Never guest data, never portal tokens.
 * See SECURITY.md, "Saved credentials".
 */
interface SecureStore {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun remove(key: String)

    fun clearAll() {
        remove(USERNAME)
        remove(PASSWORD)
    }

    companion object {
        const val USERNAME = "username"
        const val PASSWORD = "password" // pragma: allowlist secret (Keychain account name, not a credential)
    }
}

/** Biometrics or the device passcode. */
interface DeviceOwnerCheck {
    /** `false` when the device has no lock screen or the user cancels. */
    suspend fun confirm(reason: String): Boolean
}

/** Whether the device currently has internet connectivity, for the connectivity banner. */
interface ConnectivityMonitor {
    val isConnected: StateFlow<Boolean>
}

/** How the user left the system SMS composer. */
enum class MessageResult { Sent, Cancelled, Failed, Unknown }

/** Hands a message to the system SMS app. The user always taps Send. */
interface MessageComposer {
    val canSendText: Boolean
    suspend fun compose(recipient: String, body: String): MessageResult
}

/** Opens the system share sheet for a file. */
interface FileSharer {
    fun share(path: Path, mimeType: String)
}

/** Everything the shared code needs from the platform. Built once per process. */
class Platform(
    val fileSystem: FileSystem,
    val cacheDirectory: Path,
    val temporaryDirectory: Path,
    val preferences: KeyValueStore,
    val secureStore: SecureStore,
    val deviceOwner: DeviceOwnerCheck,
    val connectivity: ConnectivityMonitor,
    val messages: MessageComposer,
    val files: FileSharer,
    val pdf: PdfRenderer,
    val appVersion: String,
    val buildNumber: String,
)
