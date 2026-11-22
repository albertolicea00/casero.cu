package com.casero.cu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import android.text.format.DateUtils
import com.casero.cu.model.PortalDates
import java.text.DateFormat
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

actual fun formatMediumDate(date: LocalDate): String {
    val utc = TimeZone.getTimeZone("UTC")
    val calendar = Calendar.getInstance(utc).apply {
        clear()
        set(date.year, date.month.ordinal, date.day)
    }
    return DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = utc }.format(calendar.time)
}

actual fun formatDateTime(instant: Instant): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .apply { timeZone = TimeZone.getTimeZone(PortalDates.timeZone.id) }
        .format(java.util.Date(instant.toEpochMilliseconds()))

actual fun formatElapsed(from: Instant, to: Instant): String =
    DateUtils.getRelativeTimeSpanString(
        from.toEpochMilliseconds(),
        to.toEpochMilliseconds(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()

actual fun formatMonthYear(date: LocalDate): String = skeleton(date, "MMMyyyy")

actual fun formatWeekdayDayMonth(date: LocalDate): String = skeleton(date, "EEEdMMM")

private fun skeleton(date: LocalDate, pattern: String): String {
    val locale = java.util.Locale.getDefault()
    val utc = TimeZone.getTimeZone("UTC")
    val calendar = Calendar.getInstance(utc).apply {
        clear()
        set(date.year, date.month.ordinal, date.day)
    }
    val best = android.text.format.DateFormat.getBestDateTimePattern(locale, pattern)
    return java.text.SimpleDateFormat(best, locale).apply { timeZone = utc }.format(calendar.time)
}
