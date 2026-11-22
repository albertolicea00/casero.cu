package com.casero.cu.reporting

/**
 * Published SMS procedure supplied with the project's reference pages.
 * Each guest/companion report has a separate confirmation exchange.
 * See AGENTS.md, "SMS reporting contract".
 */
object SmsReporter {
    const val SMS_DESTINATION = "7344"
    const val ASSOCIATION_PHONE = "18808"
    const val INSTRUCTIONS_URL =
        "https://cubatramite.com/reporte-a-inmigracion-del-registro-de-turistas-extranjeros-hospedados-en-viviendas-de-alquiler/"

    enum class ValidationError { Passport, CompanionIdentity, Days, ConfirmationCode, MessageTooLong }

    class InvalidReport(val reason: ValidationError) : Exception(reason.name)

    fun reportBody(passport: String, days: String, companionIdentity: String? = null): String {
        val normalized = normalizedIdentifier(passport) ?: throw InvalidReport(ValidationError.Passport)
        val dayText = days.trim()
        val count = dayText.takeIf { text -> text.isNotEmpty() && text.all { it in '0'..'9' } }?.toIntOrNull()
        if (count == null || count <= 0) throw InvalidReport(ValidationError.Days)
        val fields = mutableListOf("HC$normalized")
        if (companionIdentity != null) {
            val identity = companionIdentity.trim()
            if (identity.length != 11 || !identity.all { it in '0'..'9' }) {
                throw InvalidReport(ValidationError.CompanionIdentity)
            }
            fields.add(identity)
        }
        fields.add(count.toString())
        return checkedLength(fields.joinToString(" "))
    }

    fun confirmationBody(code: String): String {
        val normalized = normalizedIdentifier(code)
        if (normalized == null || normalized.startsWith("HC")) throw InvalidReport(ValidationError.ConfirmationCode)
        return checkedLength("HC$normalized")
    }

    /** Plain telephone numbers can be handed to the system dialer. */
    fun dialUri(code: String): String? =
        if (code.isNotEmpty() && code.all { it in '0'..'9' || it == '+' }) "tel:$code" else null

    private fun normalizedIdentifier(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || !trimmed.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }) return null
        return trimmed.uppercase()
    }

    private fun checkedLength(body: String): String {
        if (body.length > 160) throw InvalidReport(ValidationError.MessageTooLong)
        return body
    }
}
