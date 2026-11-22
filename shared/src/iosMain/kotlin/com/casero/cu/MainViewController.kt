package com.casero.cu

import androidx.compose.ui.window.ComposeUIViewController
import com.casero.cu.platform.IosPlatform

fun MainViewController() = ComposeUIViewController { App(IosPlatform.shared) }
