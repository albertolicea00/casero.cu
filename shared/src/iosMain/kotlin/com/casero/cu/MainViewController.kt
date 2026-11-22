package com.casero.cu

import androidx.compose.ui.window.ComposeUIViewController
import com.casero.cu.platform.IosPlatform
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController =
    ComposeUIViewController { App(IosPlatform.shared) }.also {
        StatusBarTapCatcher.install(it)
        PrivacyCover.install(it)
    }
