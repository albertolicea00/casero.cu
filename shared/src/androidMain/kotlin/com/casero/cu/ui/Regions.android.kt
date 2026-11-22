package com.casero.cu.ui

import java.util.Locale

actual fun isoRegionNames(): Map<String, String> = buildMap {
    for (code in Locale.getISOCountries()) {
        for (language in listOf("es", "en")) {
            put(Locale("", code).getDisplayCountry(Locale(language)), code)
        }
    }
}
