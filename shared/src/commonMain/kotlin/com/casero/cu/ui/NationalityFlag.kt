package com.casero.cu.ui

import com.casero.cu.model.HtmlTextFold

object NationalityFlag {
    private val aliases = mapOf(
        "CUBA" to "CU",
        "ESPANA" to "ES",
        "ESTADOS UNIDOS DE AMERICA" to "US",
        "ESTADOS UNIDOS" to "US",
        "REINO UNIDO" to "GB",
        "INGLATERRA" to "GB",
        "RUSIA" to "RU",
        "PAISES BAJOS" to "NL",
        "HOLANDA" to "NL",
        "REPUBLICA CHECA" to "CZ",
        "COREA DEL SUR" to "KR",
        "COREA DEL NORTE" to "KP",
        "REPUBLICA DOMINICANA" to "DO",
        "COSTA RICA" to "CR",
        "PUERTO RICO" to "PR",
        "VENEZUELA" to "VE",
        "MEXICO" to "MX",
        "CANADA" to "CA",
        "ALEMANIA" to "DE",
        "FRANCIA" to "FR",
        "ITALIA" to "IT",
        "PORTUGAL" to "PT",
        "BRASIL" to "BR",
        "ARGENTINA" to "AR",
        "CHILE" to "CL",
        "COLOMBIA" to "CO",
        "PANAMA" to "PA",
        "JAPON" to "JP",
        "CHINA" to "CN",
        "AUSTRALIA" to "AU",
    )

    /** Region names in Spanish and English, folded, to ISO codes. */
    private val localizedCodes: Map<String, String> by lazy {
        isoRegionNames().mapKeys { (name, _) -> fold(name) }
    }

    fun emoji(nationality: String?): String? {
        val normalized = fold(nationality ?: return null)
        val code = aliases[normalized] ?: localizedCodes[normalized] ?: return null
        if (code.length != 2) return null
        return code.uppercase().map { 127_397 + it.code }.joinToString("") { codePoint ->
            val offset = codePoint - 0x10000
            charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
        }
    }

    private fun fold(value: String): String = HtmlTextFold.fold(value).trim().uppercase()
}

/** Region display names in Spanish and English, keyed by name, valued by ISO 3166 code. */
expect fun isoRegionNames(): Map<String, String>
