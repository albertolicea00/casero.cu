package com.casero.cu

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.labelColor
import platform.UIKit.systemBackgroundColor

/**
 * Covers the app whenever it stops being active: the app switcher snapshot, Control
 * Center and the Face ID prompt never show guests' names or documents. It is a UIKit
 * view, so it is on screen before iOS takes the snapshot.
 */
@OptIn(ExperimentalForeignApi::class)
internal object PrivacyCover {
    private var cover: UIView? = null

    fun install(controller: UIViewController) {
        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue
        center.addObserverForName(UIApplicationWillResignActiveNotification, null, queue) { _ -> show(controller) }
        center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, queue) { _ -> hide() }
    }

    private fun show(controller: UIViewController) {
        if (cover != null) return
        val host = controller.view.window ?: controller.view
        val view = UIView(frame = host.bounds).apply {
            backgroundColor = UIColor.systemBackgroundColor
            autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        }
        val name = UILabel().apply {
            text = "CASEROcu"
            font = UIFont.systemFontOfSize(28.0, UIFontWeightSemibold)
            textColor = UIColor.labelColor
            translatesAutoresizingMaskIntoConstraints = false
        }
        view.addSubview(name)
        NSLayoutConstraint.activateConstraints(
            listOf(
                name.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor),
                name.centerYAnchor.constraintEqualToAnchor(view.centerYAnchor),
            ),
        )
        host.addSubview(view)
        cover = view
    }

    private fun hide() {
        cover?.removeFromSuperview()
        cover = null
    }
}
