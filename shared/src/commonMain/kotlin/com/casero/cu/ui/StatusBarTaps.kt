package com.casero.cu.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Taps on the iPhone status bar, the system "scroll to top" gesture. Compose has no
 * `UIScrollView`, so iOS forwards the tap here (`StatusBarTapCatcher`) and lists collect it.
 */
object StatusBarTaps {
    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val taps: SharedFlow<Unit> = events.asSharedFlow()

    fun post() {
        events.tryEmit(Unit)
    }
}
