package com.casero.cu.platform

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

/** Builds the Android [Platform]. Call once from the application's first activity. */
object AndroidPlatform {
    private var activity = WeakReference<FragmentActivity>(null)
    private val composerResult = MutableStateFlow<CompletableDeferred<MessageResult>?>(null)
    private var platform: Platform? = null

    /** The activity that hosts the UI; dialogs and share sheets attach to it. */
    fun attach(activity: FragmentActivity) {
        this.activity = WeakReference(activity)
    }

    /** Called from `onResume`: the user came back from the SMS app. */
    fun onResume() {
        composerResult.value?.complete(MessageResult.Unknown)
        composerResult.value = null
    }

    fun get(context: Context): Platform = platform ?: create(context.applicationContext).also { platform = it }

    private fun create(context: Context): Platform {
        val cache = context.cacheDir.toOkioPath()
        val temporary = (context.cacheDir.toOkioPath() / "exports").also { FileSystem.SYSTEM.createDirectories(it) }
        val version = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val build = if (Build.VERSION.SDK_INT >= 28) version.longVersionCode.toString() else version.versionCode.toString()
        return Platform(
            fileSystem = FileSystem.SYSTEM,
            cacheDirectory = cache,
            temporaryDirectory = temporary,
            preferences = Preferences(context),
            secureStore = KeystoreSecureStore(context),
            deviceOwner = BiometricCheck(),
            connectivity = NetworkMonitor(context),
            messages = SmsComposer(context),
            files = FileShare(context),
            pdf = AndroidPdfRenderer,
            appVersion = version.versionName ?: "",
            buildNumber = build,
        )
    }

    private class Preferences(context: Context) : KeyValueStore {
        private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        override fun getInt(key: String) = if (prefs.contains(key)) prefs.getInt(key, 0) else null
        override fun getBoolean(key: String) = if (prefs.contains(key)) prefs.getBoolean(key, false) else null
        override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
        override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    }

    /**
     * AES-GCM key in the Android Keystore, usable only while the device is unlocked.
     * The ciphertext lives in private preferences; backups are off (`allowBackup=false`),
     * and the key never leaves this device anyway.
     */
    private class KeystoreSecureStore(context: Context) : SecureStore {
        private val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

        override fun get(key: String): String? {
            val stored = prefs.getString(key, null) ?: return null
            return try {
                val bytes = Base64.decode(stored, Base64.NO_WRAP)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_LENGTH))
                cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH).decodeToString()
            } catch (_: Exception) {
                null
            }
        }

        override fun set(key: String, value: String) {
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, key())
                val encrypted = cipher.iv + cipher.doFinal(value.encodeToByteArray())
                prefs.edit().putString(key, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
            } catch (_: Exception) {
                remove(key)
            }
        }

        override fun remove(key: String) = prefs.edit().remove(key).apply()

        private fun key(): SecretKey {
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
            val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .apply { if (Build.VERSION.SDK_INT >= 28) setUnlockedDeviceRequired(true) }
                .build()
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply { init(spec) }.generateKey()
        }

        private companion object {
            const val KEYSTORE = "AndroidKeyStore"
            const val ALIAS = "com.casero.cu.portal"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val IV_LENGTH = 12
        }
    }

    /** Biometrics or the screen lock, through `BiometricPrompt`. */
    private class BiometricCheck : DeviceOwnerCheck {
        override suspend fun confirm(reason: String): Boolean {
            val host = activity.get() ?: return false
            val allowed = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            if (BiometricManager.from(host).canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
                val keyguard = host.getSystemService(KeyguardManager::class.java)
                if (keyguard?.isDeviceSecure != true) return false
            }
            return suspendCancellableCoroutine { continuation ->
                val prompt = BiometricPrompt(
                    host,
                    ContextCompat.getMainExecutor(host),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    },
                )
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(host.applicationInfo.loadLabel(host.packageManager))
                    .setSubtitle(reason)
                    .setAllowedAuthenticators(allowed)
                    .build()
                prompt.authenticate(info)
                continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            }
        }
    }

    private class NetworkMonitor(context: Context) : ConnectivityMonitor {
        private val manager = context.getSystemService(ConnectivityManager::class.java)
        private val state = MutableStateFlow(currentlyConnected())
        override val isConnected: StateFlow<Boolean> = state.asStateFlow()

        init {
            manager?.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        state.value = true
                    }

                    override fun onLost(network: Network) {
                        state.value = currentlyConnected()
                    }

                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        state.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    }
                },
            )
        }

        private fun currentlyConnected(): Boolean {
            val network = manager?.activeNetwork ?: return false
            return manager.getNetworkCapabilities(network)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
    }

    /**
     * Opens the default SMS app with recipient and body filled in. The user sends it
     * there; Android does not report back, so the result is always `Unknown`.
     */
    private class SmsComposer(private val context: Context) : MessageComposer {
        override val canSendText: Boolean
            get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING) ||
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).resolveActivity(context.packageManager) != null

        override suspend fun compose(recipient: String, body: String): MessageResult {
            val host: Activity = activity.get() ?: return MessageResult.Failed
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$recipient")).putExtra("sms_body", body)
            val result = CompletableDeferred<MessageResult>()
            composerResult.value?.complete(MessageResult.Unknown)
            composerResult.value = result
            return try {
                host.startActivity(intent)
                result.await()
            } catch (_: ActivityNotFoundException) {
                composerResult.value = null
                MessageResult.Failed
            }
        }
    }

    private class FileShare(private val context: Context) : FileSharer {
        override fun share(path: Path, mimeType: String) {
            val host = activity.get() ?: return
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", path.toFile())
            val send = Intent(Intent.ACTION_SEND)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            host.startActivity(Intent.createChooser(send, null))
        }
    }

    private object AndroidPdfRenderer : PdfRenderer {
        override fun render(pageWidth: Float, pageHeight: Float, draw: PdfCanvas.() -> Unit): ByteArray {
            val document = PdfDocument()
            val canvas = DocumentCanvas(document, pageWidth.toInt(), pageHeight.toInt())
            try {
                canvas.draw()
                canvas.finish()
                return ByteArrayOutputStream().use { output ->
                    document.writeTo(output)
                    output.toByteArray()
                }
            } finally {
                document.close()
            }
        }
    }

    private class DocumentCanvas(
        private val document: PdfDocument,
        private val width: Int,
        private val height: Int,
    ) : PdfCanvas {
        private var page: PdfDocument.Page? = null
        private val canvas: Canvas get() = page?.canvas ?: error("beginPage() not called")

        override fun beginPage() {
            finish()
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, document.pages.size + 1).create())
        }

        fun finish() {
            page?.let(document::finishPage)
            page = null
        }

        override fun measure(text: String, width: Float, style: PdfTextStyle): Float = layout(text, width, style).height.toFloat()

        override fun drawText(text: String, x: Float, y: Float, width: Float, style: PdfTextStyle) {
            val layout = layout(text, width, style)
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }

        override fun drawImage(data: ByteArray, x: Float, y: Float, maxWidth: Float, maxHeight: Float): Boolean {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return false
            val ratio = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
            canvas.drawBitmap(bitmap, null, RectF(x, y, x + bitmap.width * ratio, y + bitmap.height * ratio), null)
            bitmap.recycle()
            return true
        }

        private fun layout(text: String, width: Float, style: PdfTextStyle): StaticLayout {
            val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                textSize = style.size
                typeface = if (style.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                color = if (style.secondary) Color.DKGRAY else Color.BLACK
            }
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
        }
    }
}
