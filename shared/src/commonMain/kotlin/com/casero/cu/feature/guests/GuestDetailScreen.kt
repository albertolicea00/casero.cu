package com.casero.cu.feature.guests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.casero.cu.app.AppState
import com.casero.cu.model.Guest
import com.casero.cu.network.PortalError
import com.casero.cu.resources.Res
import com.casero.cu.resources.add_companion
import com.casero.cu.resources.companion_details
import com.casero.cu.resources.companions
import com.casero.cu.resources.guest_details
import com.casero.cu.resources.id
import com.casero.cu.resources.load_companion_photos
import com.casero.cu.resources.load_companions
import com.casero.cu.resources.load_photo
import com.casero.cu.resources.no_companions
import com.casero.cu.resources.passport_id_x
import com.casero.cu.resources.reload_companions
import com.casero.cu.resources.reload_photo
import com.casero.cu.resources.reload_photos
import com.casero.cu.resources.stay_id
import com.casero.cu.resources.stay_x
import com.casero.cu.resources.update_check_out
import com.casero.cu.ui.BackButton
import com.casero.cu.ui.CardDivider
import com.casero.cu.ui.GuestAvatar
import com.casero.cu.ui.GuestName
import com.casero.cu.ui.GuestStayText
import com.casero.cu.ui.NationalityFlag
import com.casero.cu.ui.Red
import com.casero.cu.ui.SectionHeading
import com.casero.cu.ui.StayTimeline
import com.casero.cu.ui.UiText
import com.casero.cu.ui.card
import com.casero.cu.ui.displayNationality
import com.casero.cu.ui.toImageBitmapOrNull
import com.casero.cu.ui.toUiText
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun GuestDetailScreen(
    app: AppState,
    guest: Guest,
    onBack: () -> Unit,
    isCompanion: Boolean = false,
    nationalityNamesByCode: Map<String, String> = emptyMap(),
    onCompanionsLoaded: (Int) -> Unit = {},
) {
    val cacheTTL by app.settings.cacheTTL.collectAsState()
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoError by remember { mutableStateOf<UiText?>(null) }
    var isLoadingPhoto by remember { mutableStateOf(false) }
    var companions by remember { mutableStateOf<List<Guest>>(emptyList()) }
    var companionsError by remember { mutableStateOf<UiText?>(null) }
    var isLoadingCompanions by remember { mutableStateOf(false) }
    var isLoadingCompanionPhotos by remember { mutableStateOf(false) }
    var didLoadCompanions by remember { mutableStateOf(false) }
    val companionPhotos = remember { mutableStateMapOf<String, ImageBitmap>() }
    var companionPhotoError by remember { mutableStateOf<UiText?>(null) }
    var isAddingCompanion by remember { mutableStateOf(false) }
    var isUpdatingCheckOut by remember { mutableStateOf(false) }
    var updatingCompanion by remember { mutableStateOf<Guest?>(null) }
    /** New check-out confirmed by the portal while this screen is open. */
    var updatedCheckOut by remember { mutableStateOf<Instant?>(null) }

    suspend fun loadPhoto(forceRefresh: Boolean = false) {
        if (isLoadingPhoto) return
        isLoadingPhoto = true
        photoError = null
        try {
            photo = app.photos.image(guest, isCompanion, forceRefresh).toImageBitmapOrNull()
        } catch (error: CancellationException) {
            throw error
        } catch (_: PortalError.SessionExpired) {
            app.reportSessionExpired()
        } catch (error: Exception) {
            photoError = error.toUiText()
        } finally {
            isLoadingPhoto = false
        }
    }

    suspend fun loadCompanionPhotos(onlyIfMissing: Boolean, forceRefresh: Boolean = false) {
        if (isLoadingCompanionPhotos || companions.isEmpty()) return
        isLoadingCompanionPhotos = true
        companionPhotoError = null
        try {
            for (companion in companions) {
                if (onlyIfMissing) {
                    val cached = app.photos.cachedImage(companion, isCompanion = true)?.toImageBitmapOrNull()
                    if (cached != null) {
                        companionPhotos[companion.id] = cached
                        continue
                    }
                }
                try {
                    app.photos.image(companion, isCompanion = true, forceRefresh = forceRefresh).toImageBitmapOrNull()
                        ?.let { companionPhotos[companion.id] = it }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: PortalError.SessionExpired) {
                    app.reportSessionExpired()
                    return
                } catch (error: Exception) {
                    companionPhotoError = error.toUiText()
                }
            }
        } finally {
            isLoadingCompanionPhotos = false
        }
    }

    suspend fun loadCompanions() {
        if (isLoadingCompanions) return
        isLoadingCompanions = true
        companionsError = null
        try {
            companions = app.client.listCompanions(guest.stayId, pageSize = 100)
            didLoadCompanions = true
            companionPhotos.clear()
            app.guestCache.saveCompanions(companions, guest.stayId)
            onCompanionsLoaded(companions.size)
        } catch (error: CancellationException) {
            throw error
        } catch (_: PortalError.SessionExpired) {
            app.reportSessionExpired()
            return
        } catch (error: Exception) {
            companionsError = error.toUiText()
            return
        } finally {
            isLoadingCompanions = false
        }
        loadCompanionPhotos(onlyIfMissing = true)
    }

    LaunchedEffect(guest.id, isCompanion) {
        photo = app.photos.cachedImage(guest, isCompanion)?.toImageBitmapOrNull()
        if (photo == null) launch { loadPhoto() }
        if (!isCompanion && guest.stayId.isNotEmpty()) {
            val snapshot = app.guestCache.loadCompanions(guest.stayId)
                ?.takeUnless { app.guestCache.isStale(it.fetchedAt, cacheTTL.interval) }
            if (snapshot != null) {
                companions = snapshot.companions
                didLoadCompanions = true
                onCompanionsLoaded(companions.size)
                loadCompanionPhotos(onlyIfMissing = true)
            } else {
                app.guestCache.removeCompanions(guest.stayId)
                // Like the photo: without fresh cached companions, ask the portal right away.
                if (app.isSignedIn) app.tasks.launch { loadCompanions() }
            }
        }
    }

    // Loads started here run in app.tasks: companions and photos still reach the cache
    // (and the companion count on the list) if the host goes back before they finish.
    val checkOut = updatedCheckOut ?: guest.checkOut

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isCompanion) Res.string.companion_details else Res.string.guest_details)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Photo action.
            Column(Modifier.fillMaxWidth().padding(horizontal = 5.dp), horizontalAlignment = Alignment.End) {
                Row(
                    Modifier.clickable(enabled = !isLoadingPhoto) { app.tasks.launch { loadPhoto(forceRefresh = true) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isLoadingPhoto) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(if (photo == null) Res.string.load_photo else Res.string.reload_photo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                photoError?.let { Text(it.resolve(), style = MaterialTheme.typography.labelSmall, color = Red) }
            }

            // Personal card.
            GuestCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GuestAvatar(guest.fullName, photo, 58)
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            GuestName(guest.fullName, Modifier.weight(1f), MaterialTheme.typography.titleLarge)
                            val nationality = guest.displayNationality(isCompanion)
                                ?: guest.nationalityCode?.trim()?.let(nationalityNamesByCode::get)
                            NationalityFlag.emoji(nationality)?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
                        }
                        SelectionContainer {
                            Text(
                                stringResource(Res.string.passport_id_x, guest.identificador),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                CardDivider()
                Identifiers(guest, showCompanionId = isCompanion)
            }

            // Stay card.
            GuestCard {
                StayTimeline(guest.checkIn, checkOut)
                val checkIn = guest.checkIn
                val duration = if (checkIn != null && checkOut != null) GuestStayText.nights(checkIn, checkOut) else null
                if (checkIn != null || duration != null) {
                    CardDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (guest.isActive()) {
                            Text(GuestStayText.departure(checkOut).resolve(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (checkIn != null) {
                            Text(GuestStayText.age(checkIn).resolve(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (duration != null) StayLine(duration)
                    }
                }
                if (!isCompanion && guest.isActive() && guest.stayId.isNotEmpty()) {
                    // Only active stays can change their check-out (portal: active-guests page).
                    CardDivider()
                    ActionLine(stringResource(Res.string.update_check_out), Icons.Default.EditCalendar) { isUpdatingCheckOut = true }
                }
            }

            if (!isCompanion) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeading(stringResource(Res.string.companions), Modifier.weight(1f))
                        if (didLoadCompanions) {
                            Text(companions.size.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (didLoadCompanions && companions.isEmpty() && companionsError == null) {
                        Text(stringResource(Res.string.no_companions), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (companions.isNotEmpty()) {
                        val hasAll = companions.all { companionPhotos[it.id] != null }
                        Row(
                            Modifier.align(Alignment.End).clickable(enabled = !isLoadingCompanionPhotos) {
                                app.tasks.launch { loadCompanionPhotos(onlyIfMissing = false, forceRefresh = true) }
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isLoadingCompanionPhotos) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(if (hasAll) Res.string.reload_photos else Res.string.load_companion_photos),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    for (companion in companions) {
                        CompanionSummary(
                            companion,
                            companionPhotos[companion.id],
                            onUpdateCheckOut = if (companion.isActive() && companion.companionId != null) ({ updatingCompanion = companion }) else null,
                        )
                    }
                    companionPhotoError?.let { Text(it.resolve(), Modifier.align(Alignment.End), style = MaterialTheme.typography.bodySmall, color = Red) }
                    companionsError?.let { Text(it.resolve(), style = MaterialTheme.typography.bodySmall, color = Red) }
                    if (guest.isActive() && guest.stayId.isNotEmpty()) {
                        // Only active stays take new companions (portal: active-guests page).
                        OutlinedButton(onClick = { isAddingCompanion = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.add_companion))
                        }
                    }
                    Button(
                        onClick = { app.tasks.launch { loadCompanions() } },
                        enabled = !isLoadingCompanions && guest.stayId.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        if (isLoadingCompanions) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(if (didLoadCompanions) Res.string.reload_companions else Res.string.load_companions))
                        }
                    }
                }
            }
        }
    }

    if (isUpdatingCheckOut) {
        UpdateCheckOutSheet(app, guest, companionId = null, onDismiss = { isUpdatingCheckOut = false }) { date ->
            updatedCheckOut = date
            app.guestsDidChange()
        }
    }
    updatingCompanion?.let { companion ->
        UpdateCheckOutSheet(app, companion, companionId = companion.companionId, onDismiss = { updatingCompanion = null }) {
            app.tasks.launch { loadCompanions() }
        }
    }
    if (isAddingCompanion) {
        AddCompanionSheet(
            app = app,
            guest = guest,
            existingIDs = companions.map { it.identificador.uppercase() }.toSet(),
            onDismiss = { isAddingCompanion = false },
            onAdded = { app.tasks.launch { loadCompanions() } },
        )
    }
}

@Composable
private fun GuestCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.card, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun Identifiers(guest: Guest, showCompanionId: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        LabeledValue(stringResource(Res.string.stay_id), guest.stayId, Modifier.weight(1f))
        if (showCompanionId) LabeledValue(stringResource(Res.string.id), guest.companionId ?: "--")
    }
}

@Composable
private fun LabeledValue(title: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text("$title: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun StayLine(duration: UiText) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Bedtime, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(Res.string.stay_x, duration.resolve()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionLine(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CompanionSummary(guest: Guest, photo: ImageBitmap?, onUpdateCheckOut: (() -> Unit)?) {
    GuestCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuestAvatar(guest.fullName, photo, 46)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    GuestName(guest.fullName, Modifier.weight(1f), MaterialTheme.typography.titleSmall)
                    NationalityFlag.emoji(guest.displayNationality(isCompanion = true))?.let { Text(it) }
                }
                Text(
                    stringResource(Res.string.passport_id_x, guest.identificador),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CardDivider()
        Identifiers(guest, showCompanionId = true)
        if (guest.checkIn != null || guest.checkOut != null) {
            CardDivider()
            StayTimeline(guest.checkIn, guest.checkOut)
        }
        val duration = GuestStayText.duration(guest)
        if (guest.isActive() || duration != null) {
            // Like the main stay card, without "days ago": only active companions say when they leave.
            CardDivider()
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (guest.isActive()) {
                    Text(GuestStayText.departure(guest.checkOut).resolve(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (duration != null) StayLine(duration)
            }
        }
        if (onUpdateCheckOut != null) {
            CardDivider()
            ActionLine(stringResource(Res.string.update_check_out), Icons.Default.EditCalendar, onUpdateCheckOut)
        }
    }
}
