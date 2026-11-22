package com.casero.cu.ui

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.ISOCountryCodes
import platform.Foundation.localeWithLocaleIdentifier

actual fun isoRegionNames(): Map<String, String> = buildMap {
    val locales = listOf("es", "en").map { NSLocale.localeWithLocaleIdentifier(it) }
    for (code in NSLocale.ISOCountryCodes.mapNotNull { it as? String }) {
        for (locale in locales) {
            locale.displayNameForKey(NSLocaleCountryCode, code)?.let { put(it, code) }
        }
    }
}
