package com.casero.cu.model

import kotlin.time.Instant

/** ASP.NET dates use UTC milliseconds; only DateTime.MinValue is absent. */
object DotNetDate {
    private val pattern = Regex("""^/Date\((-?\d+)(?:[+-]\d{4})?\)/$""")

    fun parse(raw: String?): Instant? {
        val millis = raw?.let { pattern.find(it) }?.groupValues?.get(1)?.toLongOrNull() ?: return null
        if (millis <= -62_135_578_800_000) return null
        return Instant.fromEpochMilliseconds(millis)
    }

    fun string(instant: Instant): String = "/Date(${instant.toEpochMilliseconds()})/"
}
