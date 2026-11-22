package com.casero.cu.model

import com.casero.cu.network.PortalError

/**
 * The host's account as shown on `GET /Cuenta/Perfil`. The page renders it as an HTML
 * table of `td.titulo` / `td.texto` pairs inside `div.perfil`; there is no JSON endpoint.
 */
data class PortalProfile(
    val firstName: String = "",
    val secondName: String = "",
    val lastName1: String = "",
    val lastName2: String = "",
    val identityNumber: String = "",
    val province: String = "",
    val municipality: String = "",
    val username: String = "",
    val address: String = "",
) {
    val fullName: String
        get() = listOf(firstName, secondName, lastName1, lastName2).filter { it.isNotEmpty() }.joinToString(" ")

    companion object {
        private val profileClass = Regex("""class="[^"]*\bperfil\b[^"]*"""")
        private val cell = Regex("""<td\b([^>]*)>(.*?)</td>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

        /**
         * Reads the profile table. Throws `InvalidResponse` when the page has no profile
         * block (for example the login page served instead of the profile).
         */
        fun parse(html: String): PortalProfile {
            val start = profileClass.find(html) ?: throw PortalError.InvalidResponse
            // The password form follows the profile block; stop there.
            val end = html.indexOf("<form", start.range.last + 1).takeIf { it >= 0 } ?: html.length
            val block = html.substring(start.range.last + 1, end)

            var profile = PortalProfile()
            var label: String? = null
            var found = false
            for (match in cell.findAll(block)) {
                val attributes = match.groupValues[1]
                val text = HtmlText.plain(match.groupValues[2])
                if ("titulo" in attributes) {
                    label = text
                } else if ("texto" in attributes && label != null) {
                    val assigned = profile.assign(text, label)
                    label = null
                    if (assigned != null) {
                        profile = assigned
                        found = true
                    }
                }
            }
            if (!found) throw PortalError.InvalidResponse
            return profile
        }
    }

    /** Returns the updated profile, or `null` when the label is not a known field. */
    private fun assign(value: String, label: String): PortalProfile? =
        when (HtmlText.fold(label)) {
            "primer nombre" -> copy(firstName = value)
            "segundo nombre" -> copy(secondName = value)
            "primer apellido" -> copy(lastName1 = value)
            "segundo apellido" -> copy(lastName2 = value)
            "ci" -> copy(identityNumber = value)
            "provincia" -> copy(province = value)
            "municipio" -> copy(municipality = value)
            "usuario" -> copy(username = value)
            "direccion" -> copy(address = value)
            else -> null
        }
}

/** Case- and accent-insensitive folding for Spanish text. */
object HtmlTextFold {
    fun fold(text: String): String = HtmlText.fold(text)
}

internal object HtmlText {
    private val tags = Regex("<[^>]+>")
    private val numeric = Regex("&#(x?)([0-9a-fA-F]+);")
    private val named = mapOf(
        "&aacute;" to "á", "&eacute;" to "é", "&iacute;" to "í", "&oacute;" to "ó", "&uacute;" to "ú",
        "&Aacute;" to "Á", "&Eacute;" to "É", "&Iacute;" to "Í", "&Oacute;" to "Ó", "&Uacute;" to "Ú",
        "&ntilde;" to "ñ", "&Ntilde;" to "Ñ", "&uuml;" to "ü", "&Uuml;" to "Ü", "&nbsp;" to " ",
        "&quot;" to "\"", "&#39;" to "'", "&lt;" to "<", "&gt;" to ">",
    )
    private val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')

    /** Drops inner tags, decodes the entities the portal uses and collapses whitespace. */
    fun plain(html: String): String {
        var text = html.replace(tags, "")
        for ((entity, character) in named) text = text.replace(entity, character)
        text = numeric.replace(text) { match ->
            val radix = if (match.groupValues[1].isNotEmpty()) 16 else 10
            match.groupValues[2].toIntOrNull(radix)?.let(::codePointToString) ?: match.value
        }
        text = text.replace("&amp;", "&")
        return text.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** Case- and accent-insensitive key for Spanish labels. */
    fun fold(text: String): String = text.lowercase().map { accents[it] ?: it }.joinToString("")

    private fun codePointToString(code: Int): String? =
        when {
            code < 0 || code > 0x10FFFF -> null
            code < 0x10000 -> code.toChar().toString()
            else -> {
                val offset = code - 0x10000
                charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
            }
        }
}
