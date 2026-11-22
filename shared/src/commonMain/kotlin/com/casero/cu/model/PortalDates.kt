package com.casero.cu.model

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Reporting dates belong to the lodging location even when the device travels. */
object PortalDates {
    val timeZone: TimeZone = TimeZone.of("America/Havana")

    fun now(): Instant = Clock.System.now()

    /** The calendar day of [instant] in Cuba. */
    fun day(instant: Instant): LocalDate = instant.toLocalDateTime(timeZone).date

    fun today(now: Instant = now()): LocalDate = day(now)

    fun startOfDay(instant: Instant): Instant = day(instant).atStartOfDayIn(timeZone)

    fun startOfDay(date: LocalDate): Instant = date.atStartOfDayIn(timeZone)

    fun addDays(instant: Instant, days: Int): Instant = instant.plus(days, DateTimeUnit.DAY, timeZone)

    fun addMonths(instant: Instant, months: Int): Instant = instant.plus(months, DateTimeUnit.MONTH, timeZone)

    /** Whole calendar days between the Cuba days of [from] and [to]. */
    fun daysBetween(from: Instant, to: Instant): Int = day(from).daysUntil(day(to))

    fun LocalDate.plusDays(days: Int): LocalDate = plus(DatePeriod(days = days))

    /** `dd/MM/yy`, the format of every date the portal accepts. */
    fun portalFormat(instant: Instant): String {
        val date = day(instant)
        return "${pad(date.day)}/${pad(date.month.ordinal + 1)}/${pad(date.year % 100)}"
    }

    /** `dd/MM/yyyy`, used in exports. */
    fun exportFormat(instant: Instant): String {
        val date = day(instant)
        return "${pad(date.day)}/${pad(date.month.ordinal + 1)}/${date.year}"
    }

    /** `dd/MM/yyyy HH:mm` in Cuba time. */
    fun timestampFormat(instant: Instant): String {
        val local = instant.toLocalDateTime(timeZone)
        return "${exportFormat(instant)} ${pad(local.hour)}:${pad(local.minute)}"
    }

    /** `yyyy-MM-dd-HHmm` in Cuba time, for file names. */
    fun fileStamp(instant: Instant): String {
        val local = instant.toLocalDateTime(timeZone)
        return "${local.year}-${pad(local.month.ordinal + 1)}-${pad(local.day)}-${pad(local.hour)}${pad(local.minute)}"
    }

    private fun pad(value: Int) = value.toString().padStart(2, '0')
}
