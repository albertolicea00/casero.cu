package com.casero.cu.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.casero.cu.App
import com.casero.cu.platform.AndroidPlatform

/** `FragmentActivity` because `BiometricPrompt` needs one. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidPlatform.attach(this)
        val platform = AndroidPlatform.get(this)
        setContent { App(platform) }
    }

    override fun onResume() {
        super.onResume()
        AndroidPlatform.attach(this)
        AndroidPlatform.onResume()
    }
}
