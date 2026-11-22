package com.casero.cu.network

/** Reads the token independently of attribute order and HTML quote style. */
object AntiForgery {
    private val inputs = Regex("""<input\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val attributes = Regex("""([\w-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""")

    fun extract(html: String): String {
        for (input in inputs.findAll(html)) {
            val values = mutableMapOf<String, String>()
            for (attribute in attributes.findAll(input.value)) {
                val value = attribute.groups[2] ?: attribute.groups[3] ?: attribute.groups[4] ?: continue
                values[attribute.groupValues[1].lowercase()] = value.value
            }
            val token = values["value"]
            if (values["name"] == "__RequestVerificationToken" && !token.isNullOrEmpty()) {
                return token.replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
            }
        }
        throw PortalError.AntiForgeryMissing
    }
}
