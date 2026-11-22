package com.casero.cu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.casero.cu.model.Guest
import com.casero.cu.model.PortalDates
import com.casero.cu.network.CaseroClient
import com.casero.cu.resources.Res
import com.casero.cu.resources.beta
import com.casero.cu.resources.born
import com.casero.cu.resources.check_in
import com.casero.cu.resources.check_out
import com.casero.cu.resources.cuba
import com.casero.cu.resources.done
import com.casero.cu.resources.from_country
import com.casero.cu.resources.in_x
import com.casero.cu.resources.leaves_in_x_days
import com.casero.cu.resources.leaves_today
import com.casero.cu.resources.leaves_tomorrow
import com.casero.cu.resources.n_1_day
import com.casero.cu.resources.n_1_month
import com.casero.cu.resources.n_1_night
import com.casero.cu.resources.n_1_year
import com.casero.cu.resources.no_departure_date
import com.casero.cu.resources.passport_id_x
import com.casero.cu.resources.this_section_uses_an_unofficial_connection_to
import com.casero.cu.resources.today
import com.casero.cu.resources.x_ago
import com.casero.cu.resources.x_days
import com.casero.cu.resources.x_months
import com.casero.cu.resources.x_nights
import com.casero.cu.resources.x_years
import kotlin.math.max
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.periodUntil
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

/** Decodes portal photo bytes; `null` when they are not an image. */
@OptIn(ExperimentalResourceApi::class)
fun ByteArray.toImageBitmapOrNull(): ImageBitmap? = try {
    decodeToImageBitmap()
} catch (_: Exception) {
    null
}

/** Display text for stays and departures, shared by Register, Guests and the detail screen. */
object GuestStayText {
    /** "Leaves today", "Leaves in 3 days"; `days` from `GuestsViewModel.daysUntilCheckOut`. */
    fun departure(checkOut: Instant?, now: Instant = PortalDates.now()): UiText =
        departure(checkOut?.let { PortalDates.daysBetween(now, it) })

    fun departure(days: Int?): UiText = when (days) {
        null -> UiText.of(Res.string.no_departure_date)
        0 -> UiText.of(Res.string.leaves_today)
        1 -> UiText.of(Res.string.leaves_tomorrow)
        else -> UiText.of(Res.string.leaves_in_x_days, days)
    }

    fun duration(guest: Guest): UiText? {
        val checkIn = guest.checkIn ?: return null
        val checkOut = guest.checkOut ?: return null
        return nights(checkIn, checkOut)
    }

    fun nights(checkIn: Instant, checkOut: Instant): UiText = nights(PortalDates.day(checkIn), PortalDates.day(checkOut))

    fun nights(checkIn: LocalDate, checkOut: LocalDate): UiText {
        val count = max(0, checkIn.daysUntil(checkOut))
        return if (count == 1) UiText.of(Res.string.n_1_night) else UiText.of(Res.string.x_nights, count)
    }

    /** "2 years, 3 months ago" / "In 4 days" / "Today", counted in Cuba days. */
    fun age(date: Instant, now: Instant = PortalDates.now()): UiText {
        val day = PortalDates.day(date)
        val today = PortalDates.today(now)
        val isFuture = day > today
        val period = if (isFuture) today.periodUntil(day) else day.periodUntil(today)
        val units = buildList {
            unit(period.years, Res.string.n_1_year, Res.string.x_years)?.let(::add)
            unit(period.months, Res.string.n_1_month, Res.string.x_months)?.let(::add)
            unit(period.days, Res.string.n_1_day, Res.string.x_days)?.let(::add)
        }
        if (units.isEmpty()) return UiText.of(Res.string.today)
        return UiText.Listed(if (isFuture) Res.string.in_x else Res.string.x_ago, units)
    }

    private fun unit(count: Int, one: StringResource, many: StringResource): UiText? = when {
        count <= 0 -> null
        count == 1 -> UiText.of(one)
        else -> UiText.of(many, count)
    }
}

/** Initials or photo; tapping the photo shows it full screen. */
@Composable
fun GuestAvatar(name: String, photo: ImageBitmap?, size: Int, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(enabled = photo != null) { expanded = true },
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(photo, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                initials(name),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize * (size / 46f)),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (expanded && photo != null) {
        Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                Image(photo, contentDescription = name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                IconButton(onClick = { expanded = false }, Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

private fun initials(name: String): String {
    val parts = name.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val first = parts.firstOrNull()?.take(1) ?: "?"
    val last = parts.drop(1).lastOrNull()?.take(1) ?: ""
    return (first + last).uppercase()
}

/** One-line name; tapping it shows the full name for a moment when it is cut. */
@Composable
fun GuestName(name: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(2_500)
            expanded = false
        }
    }
    Text(
        name,
        modifier.clickable { expanded = !expanded },
        style = style,
        fontWeight = FontWeight.SemiBold,
        maxLines = if (expanded) Int.MAX_VALUE else 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The guest's nationality, or Cuba for a companion with an 11-digit CI. */
@Composable
fun Guest.displayNationality(isCompanion: Boolean): String? {
    nationality?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    if (isCompanion && identificador.length == 11 && identificador.all { it in '0'..'9' }) {
        return stringResource(Res.string.cuba)
    }
    return null
}

/**
 * Guest-details style person block: photo, one-line name, document, then origin
 * and birth date. Used in the lookup results and on the registration cards.
 */
@Composable
fun PersonDetails(name: String, identificador: String, nationality: String?, birthDate: Instant?, photo: ImageBitmap?) {
    Column {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GuestAvatar(name, photo, 58)
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GuestName(name, style = MaterialTheme.typography.titleLarge)
                SelectionContainer {
                    Text(
                        stringResource(Res.string.passport_id_x, identificador),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        CardDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (nationality != null) {
                Fact(
                    stringResource(Res.string.from_country),
                    listOfNotNull(NationalityFlag.emoji(nationality), nationality).joinToString(" "),
                )
            }
            if (birthDate != null) Fact(stringResource(Res.string.born), formatMediumDate(PortalDates.day(birthDate)))
        }
    }
}

@Composable
fun Fact(title: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("$title: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer {
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Check-in and check-out rows. */
@Composable
fun StayTimeline(checkIn: Instant?, checkOut: Instant?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TimelineRow(stringResource(Res.string.check_in), checkIn, Icons.AutoMirrored.Filled.Login, MaterialTheme.colorScheme.primary)
        TimelineRow(stringResource(Res.string.check_out), checkOut, Icons.AutoMirrored.Filled.Logout, Orange)
    }
}

@Composable
private fun TimelineRow(title: String, date: Instant?, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            date?.let { formatMediumDate(PortalDates.day(it)) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** "Nov 21, 2026 → Nov 22, 2026 · 1 night" with the moon icon. */
@Composable
fun StayRange(checkIn: Instant, checkOut: Instant, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Bedtime, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            "${formatMediumDate(PortalDates.day(checkIn))} → ${formatMediumDate(PortalDates.day(checkOut))} · " +
                GuestStayText.nights(checkIn, checkOut).resolve(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A portal page offered as a fallback in [SectionInfoSheet]. */
data class PortalLink(val title: StringResource, val path: String)

/**
 * Short "what is this screen" sheet for tabs backed by the official portal.
 * Flags the feature as beta and links to the matching portal pages as a fallback.
 */
@Composable
fun SectionInfoSheet(title: String, summary: String, links: List<PortalLink>, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    CaseroSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }) {
                    Text(stringResource(Res.string.done))
                }
            }
            Text(summary, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodyMedium)
            Card(spacing = 6) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.beta), color = Orange, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    stringResource(Res.string.this_section_uses_an_unofficial_connection_to),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (link in links) {
                SecondaryButton(stringResource(link.title), { uriHandler.openUri(CaseroClient.portalUrl(link.path)) }, icon = Icons.Default.Public)
            }
        }
    }
}

/** The flag of [nationality]; tapping it shows the country name for a moment, like the iOS app. */
@Composable
fun NationalityFlagWithName(nationality: String?, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium) {
    val name = nationality?.trim()?.takeIf { it.isNotEmpty() } ?: return
    val flag = NationalityFlag.emoji(name) ?: return
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(1_800)
            visible = false
        }
    }
    Box {
        Text(flag, Modifier.clickable { visible = !visible }, style = style)
        if (visible) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopEnd,
                offset = androidx.compose.ui.unit.IntOffset(0, with(androidx.compose.ui.platform.LocalDensity.current) { 30.dp.roundToPx() }),
                onDismissRequest = { visible = false },
            ) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        name,
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
