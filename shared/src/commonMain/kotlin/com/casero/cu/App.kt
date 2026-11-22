package com.casero.cu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.casero.cu.app.AppState
import com.casero.cu.feature.guests.GuestDetailScreen
import com.casero.cu.feature.guests.GuestsScreen
import com.casero.cu.feature.guests.GuestsTab
import com.casero.cu.feature.help.CubatramiteGuideScreen
import com.casero.cu.feature.help.PortalHelpScreen
import com.casero.cu.feature.help.UserManualScreen
import com.casero.cu.feature.login.LoginSheet
import com.casero.cu.feature.register.RegisterGuestScreen
import com.casero.cu.feature.register.RegisterTab
import com.casero.cu.feature.settings.SettingsDestination
import com.casero.cu.feature.settings.SettingsScreen
import com.casero.cu.feature.settings.SettingsViewModel
import com.casero.cu.feature.sms.SmsReportScreen
import com.casero.cu.feature.sms.SmsReportViewModel
import com.casero.cu.model.Guest
import com.casero.cu.platform.Platform
import com.casero.cu.resources.Res
import com.casero.cu.resources.app_session_expired_tap_to_sign_in_again
import com.casero.cu.resources.guests
import com.casero.cu.resources.no_internet_connection_check_your_wi_fi_or
import com.casero.cu.resources.register
import com.casero.cu.resources.settings
import com.casero.cu.resources.sign_in_to_the_app_session
import com.casero.cu.resources.signing_out_of_the_app_session
import com.casero.cu.resources.sms
import com.casero.cu.resources.use_your_portal_account_to_look_up_and_report
import com.casero.cu.ui.CaseroTheme
import com.casero.cu.ui.Orange
import com.casero.cu.ui.PlatformBackHandler
import com.casero.cu.ui.Red
import com.casero.cu.ui.Yellow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class Tab(val label: StringResource, val icon: ImageVector) {
    Sms(Res.string.sms, Icons.AutoMirrored.Filled.Message),
    Register(Res.string.register, Icons.Default.PersonAdd),
    Guests(Res.string.guests, Icons.Default.Group),
    Settings(Res.string.settings, Icons.Default.Settings),
}

/** A screen pushed on top of a tab's root. */
private sealed interface Route {
    data class GuestDetail(val guest: Guest) : Route
    data class SettingsPage(val destination: SettingsDestination) : Route
}

@Composable
fun App(platform: Platform) {
    val app = remember { AppState(platform) }
    CaseroTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MainScreen(app)
        }
    }
}

/** Registration requires login; local settings and retained guests work offline. */
@Composable
private fun MainScreen(app: AppState) {
    var selection by remember { mutableStateOf(Tab.Sms) }
    var isLoginPresented by remember { mutableStateOf(false) }
    val stacks = remember { Tab.entries.associateWith { mutableStateListOf<Route>() } }
    val guestsTab = remember { GuestsTab(app) }
    val registerTab = remember { RegisterTab(app) }
    val smsModel = remember { SmsReportViewModel() }
    val settingsModel = remember {
        SettingsViewModel(app.platform, app.settings, app.guestCache, app.photoCache, app.photos, app.exports)
    }
    val saveableState = rememberSaveableStateHolder()
    val isConnected by app.platform.connectivity.isConnected.collectAsState()

    LaunchedEffect(Unit) {
        app.restoreLocalData()
        app.restoreSession()
    }
    LaunchedEffect(app.isSignedIn) { if (app.isSignedIn) isLoginPresented = false }

    fun select(tab: Tab) {
        selection = tab
        if (tab == Tab.Register && !app.isSignedIn) isLoginPresented = true
    }

    val stack = stacks.getValue(selection)
    PlatformBackHandler(enabled = stack.isNotEmpty()) { stack.removeAt(stack.lastIndex) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                for (tab in Tab.entries) {
                    NavigationBarItem(
                        selected = selection == tab,
                        onClick = {
                            // Tapping the current tab again goes back to its root, like iOS.
                            if (selection == tab) stacks.getValue(tab).clear()
                            select(tab)
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        // The tab bar already pads for the system bars; screens must not pad again.
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            Box(Modifier.weight(1f)) {
                saveableState.SaveableStateProvider("${selection.name}-${stack.size}") {
                    when (val route = stack.lastOrNull()) {
                        is Route.GuestDetail -> GuestDetailScreen(
                            app = app,
                            guest = route.guest,
                            onBack = { stack.removeAt(stack.lastIndex) },
                            nationalityNamesByCode = guestsTab.nationalityNamesByCode,
                            onCompanionsLoaded = { count -> guestsTab.companionCounts[route.guest.id] = count },
                        )
                        is Route.SettingsPage -> when (route.destination) {
                            SettingsDestination.UserManual -> UserManualScreen { stack.removeAt(stack.lastIndex) }
                            SettingsDestination.PortalHelp -> PortalHelpScreen { stack.removeAt(stack.lastIndex) }
                            SettingsDestination.CubatramiteGuide -> CubatramiteGuideScreen { stack.removeAt(stack.lastIndex) }
                        }
                        null -> when (selection) {
                            Tab.Sms -> SmsReportScreen(
                                model = smsModel,
                                messages = app.platform.messages,
                                onOpenSettings = { select(Tab.Settings) },
                                onOpenCubatramiteGuide = {
                                    select(Tab.Settings)
                                    stacks.getValue(Tab.Settings).apply {
                                        clear()
                                        add(Route.SettingsPage(SettingsDestination.CubatramiteGuide))
                                    }
                                },
                            )
                            Tab.Register -> AuthGate(app, onSignIn = { isLoginPresented = true }) { RegisterGuestScreen(app, registerTab) }
                            Tab.Guests -> GuestsScreen(app, guestsTab) { guest -> stack.add(Route.GuestDetail(guest)) }
                            Tab.Settings -> SettingsScreen(
                                app = app,
                                model = settingsModel,
                                onOpen = { stack.add(Route.SettingsPage(it)) },
                                onSignIn = { isLoginPresented = true },
                            )
                        }
                    }
                }
                if (app.isSigningOut) {
                    // Like `.disabled` on iOS: nothing reacts while signing out.
                    Box(Modifier.fillMaxSize().clickable(enabled = true, onClick = {}))
                }
            }
            // Status banners sit right above the tab bar.
            Column {
                if (app.isSigningOut) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.signing_out_of_the_app_session))
                    }
                }
                if (app.sessionExpiredBanner) {
                    Banner(stringResource(Res.string.app_session_expired_tap_to_sign_in_again), Icons.Default.Warning, Orange.copy(alpha = 0.2f)) {
                        isLoginPresented = true
                    }
                }
                // SMS reports work without internet; the warning would only confuse there.
                if (!isConnected && selection != Tab.Sms) {
                    Banner(stringResource(Res.string.no_internet_connection_check_your_wi_fi_or), Icons.Default.WifiOff, Red.copy(alpha = 0.15f))
                }
                if (!app.isSignedIn && !app.sessionExpiredBanner && selection != Tab.Sms) {
                    Banner(stringResource(Res.string.sign_in_to_the_app_session), Icons.Default.PersonAdd, Yellow.copy(alpha = 0.25f)) {
                        isLoginPresented = true
                    }
                }
            }
        }
    }

    if (isLoginPresented) LoginSheet(app) { isLoginPresented = false }
}

/** Keeps registration locked until the login sheet succeeds; SMS remains available without a portal account. */
@Composable
private fun AuthGate(app: AppState, onSignIn: () -> Unit, content: @Composable () -> Unit) {
    if (app.isSignedIn) {
        content()
        return
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.register)) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(38.dp))
            Text(stringResource(Res.string.sign_in_to_the_app_session), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                stringResource(Res.string.use_your_portal_account_to_look_up_and_report),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onSignIn) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.sign_in_to_the_app_session))
            }
        }
    }
}

@Composable
private fun Banner(text: String, icon: ImageVector, background: Color, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(background)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
