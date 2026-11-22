package com.casero.cu.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.casero.cu.resources.Res
import com.casero.cu.resources.continue_text
import com.casero.cu.resources.device_check_body
import com.casero.cu.resources.device_check_private
import com.casero.cu.resources.device_check_title
import com.casero.cu.resources.device_check_when
import com.casero.cu.resources.not_now
import com.casero.cu.ui.CaseroSheet
import com.casero.cu.ui.PrimaryButton
import org.jetbrains.compose.resources.stringResource

/**
 * Shown once, right before the first face, fingerprint or screen-lock prompt, so the
 * system dialog does not come out of nowhere. It uses whatever unlocks the phone.
 */
@Composable
fun DeviceCheckSheet(onAnswer: (Boolean) -> Unit) {
    CaseroSheet(onDismissRequest = { onAnswer(false) }) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
            Text(stringResource(Res.string.device_check_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.device_check_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Text(
                stringResource(Res.string.device_check_when),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.device_check_private),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(stringResource(Res.string.continue_text), { onAnswer(true) })
            TextButton(onClick = { onAnswer(false) }) { Text(stringResource(Res.string.not_now)) }
        }
    }
}
