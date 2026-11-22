package com.casero.cu.ui

import com.casero.cu.model.PortalDates
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSRelativeDateTimeFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithName

actual fun formatMediumDate(date: LocalDate): String = NSDateFormatter().run {
    dateStyle = NSDateFormatterMediumStyle
    timeStyle = NSDateFormatterNoStyle
    timeZone = NSTimeZone.timeZoneWithName("UTC")!!
    stringFromDate(date.atStartOfDayIn(TimeZone.UTC).toNSDate())
}

actual fun formatDateTime(instant: Instant): String = NSDateFormatter().run {
    dateStyle = NSDateFormatterMediumStyle
    timeStyle = NSDateFormatterShortStyle
    timeZone = NSTimeZone.timeZoneWithName(PortalDates.timeZone.id)!!
    stringFromDate(instant.toNSDate())
}

actual fun formatElapsed(from: Instant, to: Instant): String =
    NSRelativeDateTimeFormatter().localizedStringForDate(from.toNSDate(), relativeToDate = to.toNSDate())

private fun Instant.toNSDate(): NSDate = NSDate.dateWithTimeIntervalSince1970(toEpochMilliseconds() / 1000.0)

actual fun formatMonthYear(date: LocalDate): String = template(date, "MMM yyyy")

actual fun formatWeekdayDayMonth(date: LocalDate): String = template(date, "EEE d MMM")

private fun template(date: LocalDate, pattern: String): String = NSDateFormatter().run {
    setLocalizedDateFormatFromTemplate(pattern)
    timeZone = NSTimeZone.timeZoneWithName("UTC")!!
    stringFromDate(date.atStartOfDayIn(TimeZone.UTC).toNSDate())
}
