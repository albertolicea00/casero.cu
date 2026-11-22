package com.casero.cu.feature.guests

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.casero.cu.ui.CaseroSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.casero.cu.app.AppState
import com.casero.cu.model.Guest
import com.casero.cu.model.PortalDates
import com.casero.cu.resources.Res
import com.casero.cu.resources.active
import com.casero.cu.resources.active_guests_in_the_portal
import com.casero.cu.resources.apply_dates
import com.casero.cu.resources.browse_your_active_guests_and_your_registration
import com.casero.cu.resources.check_in
import com.casero.cu.resources.check_out
import com.casero.cu.resources.date_range
import com.casero.cu.resources.default_guest_range_last_x_days_change_this_in
import com.casero.cu.resources.from
import com.casero.cu.resources.guests
import com.casero.cu.resources.guests_information
import com.casero.cu.resources.history
import com.casero.cu.resources.large_date_ranges_may_take_longer_to_load
import com.casero.cu.resources.last_updated_x
import com.casero.cu.resources.n_1_companion
import com.casero.cu.resources.name_or_passport
import com.casero.cu.resources.newest_first
import com.casero.cu.resources.no_guests_in_this_range
import com.casero.cu.resources.oldest_first
import com.casero.cu.resources.registered_guests_in_the_portal
import com.casero.cu.resources.reload_guests
import com.casero.cu.resources.retry
import com.casero.cu.resources.scroll_by_date
import com.casero.cu.resources.showing_cached_data
import com.casero.cu.resources.sort_guests
import com.casero.cu.resources.stay_x
import com.casero.cu.resources.to
import com.casero.cu.resources.unknown_date
import com.casero.cu.resources.x_companions
import com.casero.cu.ui.DateRow
import com.casero.cu.ui.GuestStayText
import com.casero.cu.ui.NationalityFlag
import com.casero.cu.ui.Orange
import com.casero.cu.ui.PortalLink
import com.casero.cu.ui.Red
import com.casero.cu.ui.SectionInfoSheet
import com.casero.cu.ui.card
import com.casero.cu.ui.formatDateTime
import com.casero.cu.ui.formatMonthYear
import com.casero.cu.ui.formatWeekdayDayMonth
import com.casero.cu.ui.displayNationality
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.jetbrains.compose.resources.stringResource

/** State of the Guests tab that outlives the list screen while a guest is open. */
class GuestsTab(app: AppState) {
    val model = GuestsViewModel(
        app.client, app.guestCache, app.photoCache, app.settings,
        onSessionExpired = app::reportSessionExpired,
    )

    /** Companions per stay, filled as guests are opened. */
    val companionCounts = androidx.compose.runtime.mutableStateMapOf<String, Int>()

    // What the list last reacted to, so coming back from a guest neither reloads
    // for nothing nor misses a change made there.
    internal var loaded = false
    internal var seenTTL = app.settings.cacheTTL.value
    internal var seenDefaultDays = app.settings.defaultGuestRangeDays.value
    internal var seenVersion = app.registrationsVersion
    internal var wasSignedIn = app.isSignedIn

    /** Nationality names learned from the list, for guests whose name came back empty. */
    var nationalityNamesByCode: Map<String, String> = emptyMap()
        internal set
}

/** A list section: check-in month (History) or departure day (Active). */
private data class GuestGroup(val id: String, val title: String, val detail: String?, val guests: List<Guest>)

@Composable
private fun groupedByCheckInMonth(guests: List<Guest>): List<GuestGroup> {
    val unknown = stringResource(Res.string.unknown_date)
    val groups = mutableListOf<GuestGroup>()
    for (guest in guests) {
        val day = guest.checkIn?.let(PortalDates::day)
        val month = day?.let { LocalDate(it.year, it.month, 1) }
        val id = month?.toString() ?: "unknown"
        if (groups.lastOrNull()?.id == id) {
            groups[groups.lastIndex] = groups.last().copy(guests = groups.last().guests + guest)
        } else {
            groups += GuestGroup(id, month?.let(::formatMonthYear) ?: unknown, null, listOf(guest))
        }
    }
    return groups
}

/** Active stays by how many days are left: "Leaves today", "Leaves in 3 days". */
@Composable
private fun groupedByDeparture(guests: List<Guest>): List<GuestGroup> {
    val groups = mutableListOf<GuestGroup>()
    for (guest in guests) {
        val days = GuestsViewModel.daysUntilCheckOut(guest)
        val id = days?.toString() ?: "unknown"
        if (groups.lastOrNull()?.id == id) {
            groups[groups.lastIndex] = groups.last().copy(guests = groups.last().guests + guest)
        } else {
            groups += GuestGroup(
                id,
                GuestStayText.departure(days).resolve(),
                guest.checkOut?.let { formatWeekdayDayMonth(PortalDates.day(it)) },
                listOf(guest),
            )
        }
    }
    return groups
}

/** Short lists fit on screen or scroll easily: the scrubber appears only past this many guests. */
private const val SCRUBBER_MINIMUM_GUESTS = 8

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuestsScreen(app: AppState, tab: GuestsTab, onOpenGuest: (Guest) -> Unit) {
    val model = tab.model
    val scope = rememberCoroutineScope()
    val cacheTTL by app.settings.cacheTTL.collectAsState()
    val defaultDays by app.settings.defaultGuestRangeDays.collectAsState()
    var isInfoPresented by remember { mutableStateOf(false) }
    var isDateFilterPresented by remember { mutableStateOf(false) }

    // Loads run in app.tasks, not in these effects, so leaving the tab or opening a
    // guest does not cancel them; the list is ready when the host comes back.
    LaunchedEffect(Unit) {
        if (!tab.loaded) {
            tab.loaded = true
            tab.wasSignedIn = app.isSignedIn
            app.tasks.launch {
                model.loadFromCache()
                if (app.isSignedIn) model.reload()
            }
        }
    }
    LaunchedEffect(model.guests.size) { app.hasCachedGuests = model.guests.isNotEmpty() }
    LaunchedEffect(cacheTTL) {
        if (cacheTTL != tab.seenTTL) {
            tab.seenTTL = cacheTTL
            app.tasks.launch { model.loadFromCache() }
        }
    }
    LaunchedEffect(defaultDays) {
        if (defaultDays != tab.seenDefaultDays) {
            tab.seenDefaultDays = defaultDays
            model.applyDefaultRange()
            if (app.isSignedIn) app.tasks.launch { model.reload() }
        }
    }
    LaunchedEffect(app.registrationsVersion) {
        if (app.registrationsVersion != tab.seenVersion) {
            tab.seenVersion = app.registrationsVersion
            if (app.isSignedIn) app.tasks.launch { model.reload() }
        }
    }
    LaunchedEffect(app.isSignedIn) {
        if (app.isSignedIn && !tab.wasSignedIn) app.tasks.launch { model.reload() }
        tab.wasSignedIn = app.isSignedIn
    }
    LaunchedEffect(model.isSessionExpired) {
        if (model.isSessionExpired) {
            app.reportSessionExpired()
            model.acknowledgeSessionExpired()
        }
    }

    val guests = model.filteredGuests
    val groups = if (model.segment == GuestsViewModel.Segment.Active) groupedByDeparture(guests) else groupedByCheckInMonth(guests)
    val knownNationalities = nationalityNamesByCode(model.guests).also { tab.nationalityNamesByCode = it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.guests)) },
                actions = {
                    IconButton(onClick = { isInfoPresented = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(Res.string.guests_information))
                    }
                    IconButton(onClick = { app.tasks.launch { model.reload() } }, enabled = !model.isLoading && app.isSignedIn) {
                        if (model.isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.reload_guests))
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = model.isLoading,
            onRefresh = { if (app.isSignedIn) app.tasks.launch { model.reload() } },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val listState = rememberLazyListState()
            // Lazy item index of each guest, past the controls and section headers.
            val itemIndex = remember(groups) {
                var index = 1
                buildMap {
                    for (group in groups) {
                        index += 1
                        for (guest in group.guests) put(guest.id, index++)
                    }
                }
            }
            val groupTitles = remember(groups) { groups.flatMap { group -> group.guests.map { it.id to group.title } }.toMap() }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentControls(model) { isDateFilterPresented = true }
                        SearchControls(model)
                        val message = model.errorMessage
                        if (message != null && guests.isNotEmpty()) {
                            Text(message.resolve(), style = MaterialTheme.typography.bodySmall, color = Red)
                        }
                        if (model.isOffline) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Orange, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.showing_cached_data), style = MaterialTheme.typography.bodySmall, color = Orange)
                            }
                        }
                        model.lastFetched?.let {
                            Text(
                                stringResource(Res.string.last_updated_x, formatDateTime(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (guests.isEmpty()) {
                    item(key = "empty") {
                        Box(Modifier.fillMaxWidth().heightIn(min = 320.dp), contentAlignment = Alignment.Center) {
                            EmptyState(model) { app.tasks.launch { model.reload() } }
                        }
                    }
                } else {
                    for (group in groups) {
                        stickyHeader(key = "header-${group.id}") {
                            Row(
                                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(group.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                group.detail?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        items(group.guests, key = { it.id }) { guest ->
                            GuestRow(guest, tab.companionCounts[guest.id], knownNationalities) { onOpenGuest(guest) }
                        }
                    }
                }
            }

            if (guests.size > SCRUBBER_MINIMUM_GUESTS) {
                GuestScrubber(
                    guests = guests,
                    titles = groupTitles,
                    current = {
                        val first = listState.firstVisibleItemIndex
                        guests.indexOfLast { (itemIndex[it.id] ?: 0) <= first }.coerceAtLeast(0)
                    },
                    onSelect = { index -> scope.launch { listState.scrollToItem(itemIndex[guests[index].id] ?: 0) } },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }

    if (isInfoPresented) {
        SectionInfoSheet(
            title = stringResource(Res.string.guests),
            summary = stringResource(Res.string.browse_your_active_guests_and_your_registration),
            links = listOf(
                PortalLink(Res.string.active_guests_in_the_portal, "Huespedes/HuespedesActivos"),
                PortalLink(Res.string.registered_guests_in_the_portal, "Huespedes/HuespedesRegistrados"),
            ),
            onDismiss = { isInfoPresented = false },
        )
    }

    if (isDateFilterPresented) {
        GuestDateRangeSheet(
            initialFrom = model.from,
            initialTo = model.to,
            defaultDays = defaultDays,
            isLoading = model.isLoading,
            onDismiss = { isDateFilterPresented = false },
            onApply = { from, to ->
                model.from = from
                model.to = to
                isDateFilterPresented = false
                app.tasks.launch { model.reload() }
            },
        )
    }
}

/** Nationality names by portal code, when every guest with that code agrees on one country. */
private fun nationalityNamesByCode(guests: List<Guest>): Map<String, String> =
    guests.mapNotNull { guest ->
        val code = guest.nationalityCode?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val nationality = guest.nationality?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        if (NationalityFlag.emoji(nationality) == null) return@mapNotNull null
        code to nationality
    }.groupBy({ it.first }, { it.second }).mapNotNull { (code, names) ->
        val countries = names.mapNotNull(NationalityFlag::emoji).toSet()
        if (countries.size == 1) code to names.first() else null
    }.toMap()

@Composable
private fun SegmentControls(model: GuestsViewModel, onDateRange: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val segments = GuestsViewModel.Segment.entries
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            segments.forEachIndexed { index, segment ->
                SegmentedButton(
                    selected = model.segment == segment,
                    onClick = { model.segment = segment },
                    shape = SegmentedButtonDefaults.itemShape(index, segments.size),
                ) {
                    Text(stringResource(if (segment == GuestsViewModel.Segment.Active) Res.string.active else Res.string.history))
                }
            }
        }
        IconButton(onClick = onDateRange) {
            Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(Res.string.date_range), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SearchControls(model: GuestsViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).height(44.dp).background(MaterialTheme.colorScheme.card, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (model.searchText.isEmpty()) {
                    Text(stringResource(Res.string.name_or_passport), color = MaterialTheme.colorScheme.outline)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = model.searchText,
                    onValueChange = { model.searchText = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (model.searchText.isNotEmpty()) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp).clickable { model.searchText = "" },
                )
            }
        }
        // Active is always ordered by departure, so sorting only applies to History.
        if (model.segment == GuestsViewModel.Segment.History) {
            var open by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { open = true }) {
                    Icon(Icons.Default.SwapVert, contentDescription = stringResource(Res.string.sort_guests), tint = MaterialTheme.colorScheme.outline)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    for (order in GuestsViewModel.SortOrder.entries) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(if (order == GuestsViewModel.SortOrder.NewestFirst) Res.string.newest_first else Res.string.oldest_first))
                            },
                            onClick = {
                                model.sortOrder = order
                                open = false
                            },
                            trailingIcon = if (model.sortOrder == order) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(model: GuestsViewModel, onRetry: () -> Unit) {
    val message = model.errorMessage
    when {
        model.isLoading -> CircularProgressIndicator()
        message != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Orange, modifier = Modifier.size(34.dp))
            Text(message.resolve(), color = Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
        }
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(38.dp))
            Text(stringResource(Res.string.no_guests_in_this_range), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GuestRow(guest: Guest, companionCount: Int?, knownNationalities: Map<String, String>, onClick: () -> Unit) {
    val nationality = guest.displayNationality(isCompanion = false)
        ?: guest.nationalityCode?.trim()?.let(knownNationalities::get)
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.card, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                guest.fullName,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            NationalityFlag.emoji(nationality)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        }
        if (guest.checkIn != null || guest.checkOut != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StayDate(stringResource(Res.string.check_in), guest.checkIn?.let(PortalDates::portalFormat), Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp).size(16.dp),
                )
                StayDate(stringResource(Res.string.check_out), guest.checkOut?.let(PortalDates::portalFormat), Modifier.weight(1f))
            }
        }
        val duration = GuestStayText.duration(guest)
        val companions = companionCount?.takeIf { it > 0 }
        if (duration != null || companions != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (duration != null) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.stay_x, duration.resolve()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                if (companions != null) {
                    Text(
                        if (companions == 1) stringResource(Res.string.n_1_companion) else stringResource(Res.string.x_companions, companions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StayDate(title: String, date: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(date ?: "—", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

/** Right-edge thumb that jumps through the list; shows the section title while dragging. */
@Composable
private fun GuestScrubber(
    guests: List<Guest>,
    titles: Map<String, String>,
    current: () -> Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(0) }
    val visible by remember { derivedStateOf { current() } }
    val index = if (scrubbing) selected else visible
    val scrollLabel = stringResource(Res.string.scroll_by_date)
    BoxWithConstraints(
        modifier.fillMaxHeight().width(44.dp).padding(end = 2.dp, top = 8.dp, bottom = 8.dp)
            .semantics { contentDescription = scrollLabel },
    ) {
        val height = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val thumb = with(density) { 36.dp.toPx() }
        val travel = (height - thumb).coerceAtLeast(0f)
        val position = if (guests.size > 1) travel * index / (guests.size - 1) else 0f
        Box(
            Modifier.fillMaxSize().pointerInput(guests) {
                fun select(y: Float) {
                    val fraction = (y / size.height).coerceIn(0f, 1f)
                    val target = (fraction * (guests.size - 1)).roundToInt().coerceIn(0, guests.size - 1)
                    if (target != selected || !scrubbing) {
                        selected = target
                        onSelect(target)
                    }
                }
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        scrubbing = true
                        select(offset.y)
                    },
                    onDragEnd = { scrubbing = false },
                    onDragCancel = { scrubbing = false },
                ) { change, _ -> select(change.position.y) }
            },
        ) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 18.dp, bottom = 18.dp).width(3.dp).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
            Box(
                Modifier.align(Alignment.TopEnd).offset { IntOffset(0, position.roundToInt()) }.padding(end = 4.dp)
                    .size(width = 7.dp, height = 36.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            )
        }
        if (scrubbing) {
            Text(
                titles[guests.getOrNull(index)?.id] ?: "—",
                Modifier
                    .align(Alignment.TopEnd)
                    // The scrubber column is 44 dp wide; the bubble keeps its own width and
                    // grows to the left of the thumb, like `fixedSize` on iOS.
                    .wrapContentWidth(Alignment.End, unbounded = true)
                    .offset { IntOffset(-with(density) { 52.dp.roundToPx() }, position.coerceAtMost((height - with(density) { 36.dp.toPx() }).coerceAtLeast(0f)).roundToInt()) }
                    .shadow(5.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.card, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun GuestDateRangeSheet(
    initialFrom: LocalDate,
    initialTo: LocalDate,
    defaultDays: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onApply: (LocalDate, LocalDate) -> Unit,
) {
    var from by remember { mutableStateOf(initialFrom) }
    var to by remember { mutableStateOf(initialTo) }
    val rangeLength = maxOf(0, from.daysUntil(to))
    CaseroSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(Res.string.date_range), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.default_guest_range_last_x_days_change_this_in, defaultDays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.card, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                DateRow(stringResource(Res.string.from), from, { from = it })
                DateRow(stringResource(Res.string.to), to, { to = it })
            }
            if (rangeLength > 180) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.large_date_ranges_may_take_longer_to_load), style = MaterialTheme.typography.bodySmall, color = Orange)
                }
            }
            Button(onClick = { onApply(from, to) }, enabled = from <= to && !isLoading, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(stringResource(Res.string.apply_dates))
            }
        }
    }
}
