package com.casero.cu.ui

import androidx.compose.runtime.Composable
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/** System back (Android back gesture); iOS screens use their toolbar back button. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** Medium date in the device's language, e.g. "22 nov 2026". */
expect fun formatMediumDate(date: LocalDate): String

/** Medium date and short time in Cuba time. */
expect fun formatDateTime(instant: Instant): String

/** "5 minutes", "2 days"… for "last updated … ago". */
expect fun formatElapsed(from: Instant, to: Instant): String

/** "Nov 2026" in the device's language. */
expect fun formatMonthYear(date: LocalDate): String

/** "Sun 22 Nov" in the device's language. */
expect fun formatWeekdayDayMonth(date: LocalDate): String
