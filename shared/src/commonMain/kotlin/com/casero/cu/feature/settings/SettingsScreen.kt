package com.casero.cu.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.casero.cu.ui.CaseroSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.casero.cu.app.AppState
import com.casero.cu.export.ExportService
import com.casero.cu.model.CommunicationChannel
import com.casero.cu.model.PortalProfile
import com.casero.cu.network.CaseroClient
import com.casero.cu.network.PortalError
import com.casero.cu.persistence.AppSettings
import com.casero.cu.resources.Res
import com.casero.cu.resources.about
import com.casero.cu.resources.account
import com.casero.cu.resources.address
import com.casero.cu.resources.adding_or_removing_channels_is_done_in_the
import com.casero.cu.resources.app_disclaimer
import com.casero.cu.resources.app_user_manual
import com.casero.cu.resources.auto_re_authenticate_on_expiry
import com.casero.cu.resources.built_from_the_guests_already_cached_on_this
import com.casero.cu.resources.by_albertolicea00
import com.casero.cu.resources.cache
import com.casero.cu.resources.casero_web_portal
import com.casero.cu.resources.change_password_in_the_portal
import com.casero.cu.resources.check_it_on_the_official_website
import com.casero.cu.resources.clear_cache
import com.casero.cu.resources.communication_channels
import com.casero.cu.resources.create_export
import com.casero.cu.resources.csv_excel
import com.casero.cu.resources.cubatramite_source
import com.casero.cu.resources.date_range
import com.casero.cu.resources.default_date_range
import com.casero.cu.resources.device_check_failed
import com.casero.cu.resources.disclaimer
import com.casero.cu.resources.done
import com.casero.cu.resources.enable_remember_credentials_at_sign_in_to
import com.casero.cu.resources.export
import com.casero.cu.resources.export_guest_data
import com.casero.cu.resources.format
import com.casero.cu.resources.from
import com.casero.cu.resources.guests
import com.casero.cu.resources.identity_card_ci
import com.casero.cu.resources.include_photos
import com.casero.cu.resources.keep_guest_data_for
import com.casero.cu.resources.manage_phones_and_emails
import com.casero.cu.resources.municipality
import com.casero.cu.resources.n_12_months
import com.casero.cu.resources.n_15_days
import com.casero.cu.resources.n_180_days
import com.casero.cu.resources.n_1_day
import com.casero.cu.resources.n_1_hour
import com.casero.cu.resources.n_1_month
import com.casero.cu.resources.n_1_week
import com.casero.cu.resources.n_30_days
import com.casero.cu.resources.n_365_days
import com.casero.cu.resources.n_3_days
import com.casero.cu.resources.n_3_months
import com.casero.cu.resources.n_60_days
import com.casero.cu.resources.n_6_hours
import com.casero.cu.resources.n_6_months
import com.casero.cu.resources.n_7_days
import com.casero.cu.resources.n_90_days
import com.casero.cu.resources.name
import com.casero.cu.resources.never_expire
import com.casero.cu.resources.no_cached_data
import com.casero.cu.resources.no_communication_channels_registered
import com.casero.cu.resources.official_portal_help
import com.casero.cu.resources.open_source_project
import com.casero.cu.resources.pdf
import com.casero.cu.resources.phones_and_emails_registered_in_the_official
import com.casero.cu.resources.portal_profile_account
import com.casero.cu.resources.privacy_policy_terms
import com.casero.cu.resources.province
import com.casero.cu.resources.remember_credentials
import com.casero.cu.resources.retry
import com.casero.cu.resources.save_guest_photos
import com.casero.cu.resources.session
import com.casero.cu.resources.settings
import com.casero.cu.resources.sign_in_to_see_your_portal_profile
import com.casero.cu.resources.sign_in_to_see_your_registered_channels
import com.casero.cu.resources.sign_in_to_the_app_session
import com.casero.cu.resources.sign_out_of_the_app_session
import com.casero.cu.resources.signs_in_with_your_saved_credentials_when_the
import com.casero.cu.resources.stored_only_on_this_device_in_the_ios_keychain
import com.casero.cu.resources.this_range_is_used_by_default_when_loading
import com.casero.cu.resources.to
import com.casero.cu.resources.username
import com.casero.cu.resources.version_x_x
import com.casero.cu.resources.x_guests_cached_last_updated_x
import com.casero.cu.resources.your_account_in_the_official_portal
import com.casero.cu.ui.Caption
import com.casero.cu.ui.Card
import com.casero.cu.ui.CardDivider
import com.casero.cu.ui.DateRow
import com.casero.cu.ui.InputCard
import com.casero.cu.ui.PrimaryButton
import com.casero.cu.ui.Red
import com.casero.cu.ui.SecondaryButton
import com.casero.cu.ui.SectionHeading
import com.casero.cu.ui.ToggleRow
import com.casero.cu.ui.UiText
import com.casero.cu.ui.Yellow
import com.casero.cu.ui.formatElapsed
import com.casero.cu.ui.toUiText
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Screens Settings pushes on its own stack. */
enum class SettingsDestination { UserManual, PortalHelp, CubatramiteGuide }

const val PRIVACY_URL = "https://casero-cu.vercel.app/privacy.html"
const val SOURCE_URL = "https://github.com/albertolicea00/casero.cu"
const val AUTHOR_URL = "https://github.com/albertolicea00"
const val PORTAL_HOME_URL = "https://casero.rem.cu/"

fun AppSettings.CacheTTL.label(): StringResource = when (this) {
    AppSettings.CacheTTL.OneHour -> Res.string.n_1_hour
    AppSettings.CacheTTL.SixHours -> Res.string.n_6_hours
    AppSettings.CacheTTL.OneDay -> Res.string.n_1_day
    AppSettings.CacheTTL.ThreeDays -> Res.string.n_3_days
    AppSettings.CacheTTL.OneWeek -> Res.string.n_1_week
    AppSettings.CacheTTL.OneMonth -> Res.string.n_1_month
    AppSettings.CacheTTL.ThreeMonths -> Res.string.n_3_months
    AppSettings.CacheTTL.SixMonths -> Res.string.n_6_months
    AppSettings.CacheTTL.TwelveMonths -> Res.string.n_12_months
    AppSettings.CacheTTL.Forever -> Res.string.never_expire
}

private fun rangeLabel(days: Int): StringResource = when (days) {
    7 -> Res.string.n_7_days
    15 -> Res.string.n_15_days
    30 -> Res.string.n_30_days
    60 -> Res.string.n_60_days
    180 -> Res.string.n_180_days
    365 -> Res.string.n_365_days
    else -> Res.string.n_90_days
}

@Composable
fun SettingsScreen(app: AppState, model: SettingsViewModel, onOpen: (SettingsDestination) -> Unit, onSignIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val settings = app.settings
    val rememberCredentials by settings.rememberCredentials.collectAsState()
    val autoReauth by settings.autoReauth.collectAsState()
    val cacheTTL by settings.cacheTTL.collectAsState()
    val saveImages by settings.saveImages.collectAsState()
    val defaultDays by settings.defaultGuestRangeDays.collectAsState()
    var isExportFormPresented by remember { mutableStateOf(false) }
    var autoReauthFailed by remember { mutableStateOf(false) }
    var isProfilePresented by remember { mutableStateOf(false) }
    var isChannelsPresented by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { model.refreshCacheSummary() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.settings)) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeading(stringResource(Res.string.account))
            Card(spacing = 4) {
                NavRow(stringResource(Res.string.portal_profile_account), Icons.Default.AccountCircle) { isProfilePresented = true }
                CardDivider()
                NavRow(stringResource(Res.string.communication_channels), Icons.Default.Email) { isChannelsPresented = true }
                CardDivider()
                ToggleRow(stringResource(Res.string.remember_credentials), rememberCredentials, settings::setRememberCredentials)
                Caption(stringResource(Res.string.stored_only_on_this_device_in_the_ios_keychain))
                CardDivider()
                if (app.isSignedIn) {
                    TextButton(onClick = { app.tasks.launch { app.signOutCompletely() } }) {
                        Text(stringResource(Res.string.sign_out_of_the_app_session), color = Red)
                    }
                } else {
                    NavRow(stringResource(Res.string.sign_in_to_the_app_session), Icons.Default.PersonAdd, onClick = onSignIn)
                }
            }

            SectionHeading(stringResource(Res.string.session), Modifier.padding(top = 12.dp))
            Card(spacing = 4) {
                if (!rememberCredentials) Caption(stringResource(Res.string.enable_remember_credentials_at_sign_in_to))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        ToggleRow(stringResource(Res.string.auto_re_authenticate_on_expiry), autoReauth && rememberCredentials, { on ->
                            when {
                                !rememberCredentials -> Unit
                                // Turning it on proves the phone can be unlocked first.
                                on -> app.tasks.launch { autoReauthFailed = !app.enableAutoReauth() }
                                else -> {
                                    autoReauthFailed = false
                                    settings.setAutoReauth(false)
                                }
                            }
                        })
                    }
                }
                if (autoReauthFailed) {
                    Text(stringResource(Res.string.device_check_failed), style = MaterialTheme.typography.bodySmall, color = Red)
                }
                Caption(stringResource(Res.string.signs_in_with_your_saved_credentials_when_the))
            }

            SectionHeading(stringResource(Res.string.cache), Modifier.padding(top = 12.dp))
            Card(spacing = 4) {
                PickerRow(
                    stringResource(Res.string.keep_guest_data_for),
                    stringResource(cacheTTL.label()),
                    AppSettings.CacheTTL.entries.map { it to it.label() },
                    onSelect = settings::setCacheTTL,
                )
                CardDivider()
                ToggleRow(stringResource(Res.string.save_guest_photos), saveImages, { enabled ->
                    settings.setSaveImages(enabled)
                    if (!enabled) scope.launch { app.photoCache.clear() }
                })
                CardDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable {
                        scope.launch {
                            model.clearCache()
                            app.hasCachedGuests = false
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.clear_cache), color = Red)
                }
                Caption(cacheSummaryText(model))
            }

            SectionHeading(stringResource(Res.string.guests), Modifier.padding(top = 12.dp))
            Card(spacing = 4) {
                PickerRow(
                    stringResource(Res.string.default_date_range),
                    stringResource(rangeLabel(defaultDays)),
                    AppSettings.GUEST_RANGE_OPTIONS.map { it to rangeLabel(it) },
                    onSelect = settings::setDefaultGuestRangeDays,
                )
                CardDivider()
                NavRow(stringResource(Res.string.export_guest_data), Icons.Default.IosShare) { isExportFormPresented = true }
                Caption(stringResource(Res.string.this_range_is_used_by_default_when_loading))
            }

            SectionHeading(stringResource(Res.string.about), Modifier.padding(top = 12.dp))
            Card(spacing = 4) {
                Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Yellow, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(Res.string.disclaimer), fontWeight = FontWeight.SemiBold)
                    }
                    Text(stringResource(Res.string.app_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CardDivider()
                NavRow(stringResource(Res.string.app_user_manual), Icons.Outlined.Book) { onOpen(SettingsDestination.UserManual) }
                CardDivider()
                NavRow(stringResource(Res.string.official_portal_help), Icons.Default.Help) { onOpen(SettingsDestination.PortalHelp) }
                CardDivider()
                NavRow(stringResource(Res.string.cubatramite_source), Icons.AutoMirrored.Filled.MenuBook) { onOpen(SettingsDestination.CubatramiteGuide) }
                CardDivider()
                NavRow(stringResource(Res.string.privacy_policy_terms), Icons.Default.PrivacyTip, chevron = false) { uriHandler.openUri(PRIVACY_URL) }
                CardDivider()
                NavRow(stringResource(Res.string.open_source_project), Icons.Default.Code, chevron = false) { uriHandler.openUri(SOURCE_URL) }
                CardDivider()
                NavRow(stringResource(Res.string.casero_web_portal), Icons.Default.Public, chevron = false) { uriHandler.openUri(PORTAL_HOME_URL) }
            }

            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.version_x_x, app.platform.appVersion, app.platform.buildNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.by_albertolicea00),
                    Modifier.clickable { uriHandler.openUri(AUTHOR_URL) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (isProfilePresented) PortalProfileSheet(app) { isProfilePresented = false }
    if (isChannelsPresented) CommunicationChannelsSheet(app) { isChannelsPresented = false }
    if (isExportFormPresented) ExportFormSheet(app, model) { isExportFormPresented = false }
}

@Composable
private fun cacheSummaryText(model: SettingsViewModel): String {
    val summary = model.cacheSummary ?: return stringResource(Res.string.no_cached_data)
    return stringResource(Res.string.x_guests_cached_last_updated_x, summary.count, formatElapsed(summary.fetchedAt, com.casero.cu.model.PortalDates.now()))
}

@Composable
private fun NavRow(text: String, icon: ImageVector, chevron: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        // Rows without a chevron open a web page: shown as links, in blue like iOS `Link`.
        Text(text, Modifier.weight(1f), color = if (chevron) Color.Unspecified else MaterialTheme.colorScheme.primary)
        if (chevron) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun <T> PickerRow(label: String, value: String, options: List<Pair<T, StringResource>>, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { open = true }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Box {
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                for ((option, title) in options) {
                    DropdownMenuItem(text = { Text(stringResource(title)) }, onClick = {
                        open = false
                        onSelect(option)
                    })
                }
            }
        }
    }
}

@Composable
private fun SheetScaffold(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    CaseroSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.done)) }
            }
            content()
        }
    }
}

/** A read-only result loaded from the portal while signed in. */
private sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Loaded<T>(val value: T) : Load<T>
    data class Failed(val error: UiText) : Load<Nothing>
}

@Composable
private fun <T> rememberPortalLoad(app: AppState, fetch: suspend () -> T): Pair<Load<T>, () -> Unit> {
    var state by remember { mutableStateOf<Load<T>>(Load.Loading) }
    var attempt by remember { mutableStateOf(0) }
    LaunchedEffect(app.isSignedIn, attempt) {
        if (!app.isSignedIn) return@LaunchedEffect
        state = Load.Loading
        state = try {
            Load.Loaded(fetch())
        } catch (error: CancellationException) {
            throw error
        } catch (error: PortalError.SessionExpired) {
            app.reportSessionExpired()
            Load.Failed(error.toUiText())
        } catch (error: Exception) {
            Load.Failed(error.toUiText())
        }
    }
    return state to { attempt += 1 }
}

@Composable
private fun MessageCard(text: String, icon: ImageVector) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FailureCard(error: UiText, onRetry: () -> Unit, extra: @Composable () -> Unit = {}) {
    Card(spacing = 10) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(error.resolve(), color = Red)
        }
        OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
        extra()
    }
}

/** The host's portal account, read-only. Password changes happen on the official profile page. */
@Composable
private fun PortalProfileSheet(app: AppState, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val (state, retry) = rememberPortalLoad(app) { app.client.fetchProfile() }
    SheetScaffold(stringResource(Res.string.portal_profile_account), onDismiss) {
        Caption(stringResource(Res.string.your_account_in_the_official_portal))
        when {
            !app.isSignedIn -> MessageCard(stringResource(Res.string.sign_in_to_see_your_portal_profile), Icons.Default.AccountCircle)
            state is Load.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state is Load.Failed -> FailureCard(state.error, retry) {
                TextButton(onClick = { uriHandler.openUri(CaseroClient.portalUrl("Cuenta/Perfil")) }) {
                    Text(stringResource(Res.string.check_it_on_the_official_website))
                }
            }
            state is Load.Loaded -> ProfileRows(state.value)
        }
        SecondaryButton(stringResource(Res.string.change_password_in_the_portal), { uriHandler.openUri(CaseroClient.portalUrl("Cuenta/Perfil")) }, icon = Icons.Default.Public)
    }
}

@Composable
private fun ProfileRows(profile: PortalProfile) {
    val rows = listOf(
        Res.string.name to profile.fullName,
        Res.string.identity_card_ci to profile.identityNumber,
        Res.string.username to profile.username,
        Res.string.province to profile.province,
        Res.string.municipality to profile.municipality,
        Res.string.address to profile.address,
    ).filter { it.second.isNotEmpty() }
    InputCard {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) CardDivider()
            Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer { Text(value) }
            }
        }
    }
}

/** Lists the host's registered phones and e-mails. Changes happen on the official page. */
@Composable
private fun CommunicationChannelsSheet(app: AppState, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val (state, retry) = rememberPortalLoad(app) { app.client.listCommunicationChannels() }
    SheetScaffold(stringResource(Res.string.communication_channels), onDismiss) {
        Caption(stringResource(Res.string.phones_and_emails_registered_in_the_official))
        when {
            !app.isSignedIn -> MessageCard(stringResource(Res.string.sign_in_to_see_your_registered_channels), Icons.Default.AccountCircle)
            state is Load.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state is Load.Failed -> FailureCard(state.error, retry)
            state is Load.Loaded && state.value.isEmpty() ->
                MessageCard(stringResource(Res.string.no_communication_channels_registered), Icons.Default.Inbox)
            state is Load.Loaded -> ChannelRows(state.value)
        }
        SecondaryButton(stringResource(Res.string.manage_phones_and_emails), { uriHandler.openUri(CaseroClient.portalUrl("ViasComunicacion")) }, icon = Icons.Default.Public)
        Caption(stringResource(Res.string.adding_or_removing_channels_is_done_in_the))
    }
}

@Composable
private fun ChannelRows(channels: List<CommunicationChannel>) {
    InputCard {
        channels.forEachIndexed { index, channel ->
            if (index > 0) CardDivider()
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (channel.isPhone) Icons.Default.Phone else Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                SelectionContainer { Text(channel.value) }
            }
        }
    }
}

@Composable
private fun ExportFormSheet(app: AppState, model: SettingsViewModel, onDismiss: () -> Unit) {
    LaunchedEffect(model.exportedFile) {
        model.exportedFile?.let { file ->
            app.platform.files.share(file.path, file.mimeType)
        }
    }
    SheetScaffold(stringResource(Res.string.export), {
        model.removeExport()
        onDismiss()
    }) {
        SectionHeading(stringResource(Res.string.date_range))
        InputCard {
            DateRow(stringResource(Res.string.from), model.exportFrom, { model.exportFrom = it })
            CardDivider()
            DateRow(stringResource(Res.string.to), model.exportTo, { model.exportTo = it })
        }
        SectionHeading(stringResource(Res.string.format))
        Card(spacing = 8) {
            val formats = ExportService.Format.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                formats.forEachIndexed { index, format ->
                    SegmentedButton(
                        selected = model.exportFormat == format,
                        onClick = { model.exportFormat = format },
                        shape = SegmentedButtonDefaults.itemShape(index, formats.size),
                    ) { Text(stringResource(if (format == ExportService.Format.Csv) Res.string.csv_excel else Res.string.pdf)) }
                }
            }
            if (model.exportFormat == ExportService.Format.Pdf) {
                ToggleRow(stringResource(Res.string.include_photos), model.includePhotosInPDF, { model.includePhotosInPDF = it })
            }
        }
        Card(spacing = 8) {
            Caption(cacheSummaryText(model))
            Caption(stringResource(Res.string.built_from_the_guests_already_cached_on_this))
            PrimaryButton(stringResource(Res.string.create_export), { app.tasks.launch { model.export() } }, loading = model.isExporting)
            model.noticeMessage?.let { Caption(it.resolve()) }
            model.errorMessage?.let { Text(it.resolve(), style = MaterialTheme.typography.bodySmall, color = Red) }
        }
    }
}
