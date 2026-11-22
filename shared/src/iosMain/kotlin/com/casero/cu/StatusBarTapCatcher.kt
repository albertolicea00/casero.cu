package com.casero.cu

import com.casero.cu.ui.StatusBarTaps
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject

/**
 * iOS sends a status-bar tap to the one `UIScrollView` with `scrollsToTop`. This tiny,
 * never-scrolled one sits behind the Compose view and turns the tap into [StatusBarTaps].
 */
internal object StatusBarTapCatcher : NSObject(), UIScrollViewDelegateProtocol {
    override fun scrollViewShouldScrollToTop(scrollView: UIScrollView): Boolean {
        StatusBarTaps.post()
        return false
    }

    fun install(controller: UIViewController) {
        val scrollView = UIScrollView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0))
        scrollView.scrollsToTop = true
        // iOS only asks a scroll view that is not already at the top.
        scrollView.setContentSize(CGSizeMake(1.0, 2.0))
        scrollView.setContentOffset(CGPointMake(0.0, 1.0), animated = false)
        scrollView.delegate = this
        controller.view.insertSubview(scrollView, atIndex = 0)
    }
}
