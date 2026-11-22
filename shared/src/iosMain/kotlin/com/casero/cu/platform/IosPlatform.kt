package com.casero.cu.platform

import kotlin.coroutines.resume
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.MessageUI.MFMessageComposeViewController
import platform.MessageUI.MFMessageComposeViewControllerDelegateProtocol
import platform.MessageUI.MessageComposeResult
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.NSStringDrawingUsesFontLeading
import platform.UIKit.NSStringDrawingUsesLineFragmentOrigin
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererContext
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.boundingRectWithSize
import platform.UIKit.drawInRect
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/** Builds the iOS [Platform] once per process. */
object IosPlatform {
    val shared: Platform by lazy { create() }

    private fun create(): Platform {
        val caches = NSFileManager.defaultManager
            .URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            .firstOrNull()
            .let { (it as NSURL).path!! }
            .toPath()
        val temporary = NSTemporaryDirectory().toPath()
        val info = NSBundle.mainBundle.infoDictionary
        return Platform(
            fileSystem = FileSystem.SYSTEM,
            cacheDirectory = caches,
            temporaryDirectory = temporary,
            preferences = Defaults,
            secureStore = KeychainStore.also { it.migrateAccessibility() },
            deviceOwner = LocalAuthenticationCheck,
            connectivity = PathMonitor(),
            messages = MessageCompose,
            files = ShareSheet,
            pdf = IosPdfRenderer,
            appVersion = info?.get("CFBundleShortVersionString") as? String ?: "",
            buildNumber = info?.get("CFBundleVersion") as? String ?: "",
        )
    }

    private object Defaults : KeyValueStore {
        private val defaults = NSUserDefaults.standardUserDefaults
        override fun getInt(key: String) = defaults.objectForKey(key)?.let { defaults.integerForKey(key).toInt() }
        override fun getBoolean(key: String) = defaults.objectForKey(key)?.let { defaults.boolForKey(key) }
        override fun putInt(key: String, value: Int) = defaults.setInteger(value.toLong(), key)
        override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, key)
    }

    /** Face ID, Touch ID or the device passcode. Without a passcode there is no silent sign-in. */
    private object LocalAuthenticationCheck : DeviceOwnerCheck {
        override suspend fun confirm(prompt: DeviceCheckPrompt): Boolean {
            // The cancel button says "Skip": skipping only means no automatic sign-in.
            val context = LAContext().apply { localizedCancelTitle = prompt.skip }
            if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)) return false
            return suspendCancellableCoroutine { continuation ->
                context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, prompt.reason) { success, _ ->
                    if (continuation.isActive) continuation.resume(success)
                }
                continuation.invokeOnCancellation { context.invalidate() }
            }
        }
    }

    private class PathMonitor : ConnectivityMonitor {
        private val state = MutableStateFlow(true)
        override val isConnected: StateFlow<Boolean> = state.asStateFlow()
        private val monitor = nw_path_monitor_create()

        init {
            nw_path_monitor_set_update_handler(monitor) { path ->
                state.value = nw_path_get_status(path) == nw_path_status_satisfied
            }
            nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
            nw_path_monitor_start(monitor)
        }
    }

    /** `MFMessageComposeViewController`: iOS requires the user to tap Send. */
    private object MessageCompose : MessageComposer {
        private var delegate: ComposeDelegate? = null

        override val canSendText: Boolean get() = MFMessageComposeViewController.canSendText()

        override suspend fun compose(recipient: String, body: String): MessageResult {
            val presenter = topViewController() ?: return MessageResult.Failed
            if (!canSendText) return MessageResult.Failed
            return suspendCancellableCoroutine { continuation ->
                val controller = MFMessageComposeViewController()
                controller.recipients = listOf(recipient)
                controller.body = body
                val handler = ComposeDelegate { result ->
                    controller.dismissViewControllerAnimated(true, null)
                    delegate = null
                    if (continuation.isActive) continuation.resume(result)
                }
                delegate = handler
                controller.messageComposeDelegate = handler
                presenter.presentViewController(controller, animated = true, completion = null)
            }
        }
    }

    private class ComposeDelegate(private val onFinish: (MessageResult) -> Unit) :
        NSObject(), MFMessageComposeViewControllerDelegateProtocol {
        override fun messageComposeViewController(
            controller: MFMessageComposeViewController,
            didFinishWithResult: MessageComposeResult,
        ) {
            onFinish(
                when (didFinishWithResult) {
                    MessageComposeResult.MessageComposeResultSent -> MessageResult.Sent
                    MessageComposeResult.MessageComposeResultCancelled -> MessageResult.Cancelled
                    MessageComposeResult.MessageComposeResultFailed -> MessageResult.Failed
                    else -> MessageResult.Unknown
                },
            )
        }
    }

    private object ShareSheet : FileSharer {
        override fun share(path: Path, mimeType: String) {
            val presenter = topViewController() ?: return
            val url = NSURL.fileURLWithPath(path.toString())
            val controller = UIActivityViewController(listOf(url), null)
            controller.popoverPresentationController?.sourceView = presenter.view
            presenter.presentViewController(controller, animated = true, completion = null)
        }
    }

    private fun topViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.connectedScenes
            .mapNotNull { it as? UIWindowScene }
            .flatMap { scene -> scene.windows.mapNotNull { it as? UIWindow } }
            .firstOrNull { it.isKeyWindow() }
        var controller = window?.rootViewController
        while (controller?.presentedViewController != null) controller = controller.presentedViewController
        return controller
    }

    private object IosPdfRenderer : PdfRenderer {
        override fun render(pageWidth: Float, pageHeight: Float, draw: PdfCanvas.() -> Unit): ByteArray {
            val bounds = CGRectMake(0.0, 0.0, pageWidth.toDouble(), pageHeight.toDouble())
            val renderer = UIGraphicsPDFRenderer(bounds = bounds)
            val data = renderer.PDFDataWithActions { context -> RendererCanvas(context!!, bounds).draw() }
            return data.toByteArray()
        }
    }

    private class RendererCanvas(
        private val context: UIGraphicsPDFRendererContext,
        @Suppress("unused") private val bounds: CValue<CGRect>,
    ) : PdfCanvas {
        override fun beginPage() = context.beginPage()

        override fun measure(text: String, width: Float, style: PdfTextStyle): Float =
            NSString.create(string = text).boundingRectWithSize(
                CGSizeMake(width.toDouble(), Double.MAX_VALUE),
                NSStringDrawingUsesLineFragmentOrigin or NSStringDrawingUsesFontLeading,
                attributes(style),
                null,
            ).useContents { kotlin.math.ceil(size.height).toFloat() }

        override fun drawText(text: String, x: Float, y: Float, width: Float, style: PdfTextStyle) {
            val height = measure(text, width, style)
            NSString.create(string = text).drawInRect(
                CGRectMake(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble()),
                withAttributes = attributes(style),
            )
        }

        override fun drawImage(data: ByteArray, x: Float, y: Float, maxWidth: Float, maxHeight: Float): Boolean {
            val image = UIImage.imageWithData(data.toNSData()) ?: return false
            val (imageWidth, imageHeight) = image.size.useContents { width to height }
            if (imageWidth <= 0 || imageHeight <= 0) return false
            val ratio = minOf(maxWidth / imageWidth, maxHeight / imageHeight)
            image.drawInRect(CGRectMake(x.toDouble(), y.toDouble(), imageWidth * ratio, imageHeight * ratio))
            return true
        }

        private fun attributes(style: PdfTextStyle): Map<Any?, Any?> = mapOf(
            NSFontAttributeName to if (style.bold) {
                UIFont.boldSystemFontOfSize(style.size.toDouble())
            } else {
                UIFont.systemFontOfSize(style.size.toDouble())
            },
            NSForegroundColorAttributeName to if (style.secondary) UIColor.darkGrayColor else UIColor.blackColor,
        )
    }
}

@OptIn(BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
    }

internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).also { bytes -> bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) } }
}
