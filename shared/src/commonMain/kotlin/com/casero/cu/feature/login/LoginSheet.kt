package com.casero.cu.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.casero.cu.app.AppState
import com.casero.cu.network.CaseroClient
import com.casero.cu.resources.Res
import com.casero.cu.resources.hide_password
import com.casero.cu.resources.open_official_portal
import com.casero.cu.resources.password
import com.casero.cu.resources.remember_credentials
import com.casero.cu.resources.show_password
import com.casero.cu.resources.sign_in_to_the_app_session
import com.casero.cu.resources.use_your_account_to_register_guests_and_manage
import com.casero.cu.resources.username
import com.casero.cu.ui.CardDivider
import com.casero.cu.ui.CardTextField
import com.casero.cu.ui.InputCard
import com.casero.cu.ui.PrimaryButton
import com.casero.cu.ui.Red
import com.casero.cu.ui.ToggleRow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginSheet(app: AppState, onDismiss: () -> Unit) {
    val model = remember { LoginViewModel(app) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val rememberCredentials by app.settings.rememberCredentials.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Text(stringResource(Res.string.sign_in_to_the_app_session), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    stringResource(Res.string.use_your_account_to_register_guests_and_manage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            InputCard {
                CardTextField(model.user, { model.user = it }, stringResource(Res.string.username))
                CardDivider()
                CardTextField(
                    model.password,
                    { model.password = it },
                    stringResource(Res.string.password),
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (isPasswordVisible) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailing = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(if (isPasswordVisible) Res.string.hide_password else Res.string.show_password),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }

            ToggleRow(stringResource(Res.string.remember_credentials), rememberCredentials, app.settings::setRememberCredentials)

            model.validationMessage?.let {
                Text(it.resolve(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            model.errorMessage?.let { Text(it.resolve(), style = MaterialTheme.typography.bodySmall, color = Red) }

            PrimaryButton(
                stringResource(Res.string.sign_in_to_the_app_session),
                onClick = {
                    scope.launch {
                        if (model.submit()) {
                            app.didSignIn()
                            onDismiss()
                        }
                    }
                },
                enabled = model.canSubmit,
                loading = model.isLoading,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { uriHandler.openUri(CaseroClient.BASE_URL) }) { Text(stringResource(Res.string.open_official_portal)) }
            }
        }
    }
}
