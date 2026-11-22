package com.casero.cu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

private val Blue = Color(0xFF0A7AFF)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    surface = Color.White,
    background = Color.White,
    surfaceVariant = Color(0xFFF2F2F7),
    surfaceContainerHighest = Color(0xFFE5E5EA),
    secondaryContainer = Color(0xFFDCEBFF),
    onSecondaryContainer = Color(0xFF00296B),
    surfaceContainer = Color(0xFFF7F7FA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3B95FF),
    onPrimary = Color.White,
    surface = Color.Black,
    background = Color.Black,
    surfaceVariant = Color(0xFF1C1C1E),
    surfaceContainerHighest = Color(0xFF2C2C2E),
    secondaryContainer = Color(0xFF1F3A5F),
    onSecondaryContainer = Color(0xFFDCEBFF),
    surfaceContainer = Color(0xFF121214),
)

@Composable
fun CaseroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

/** The grouped background of iOS cards. */
val ColorScheme.card: Color get() = surfaceVariant

val Orange = Color(0xFFFF9500)
val Green = Color(0xFF34C759)
val Red = Color(0xFFFF3B30)
val Yellow = Color(0xFFFFCC00)

/** Rounded field group shared by forms. Rows supply their own padding and dividers. */
@Composable
fun InputCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.card, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        content = content,
    )
}

/** Rounded card with inner padding, for read-only content. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    spacing: Int = 12,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.card, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

/** A borderless text field inside an [InputCard]. */
@Composable
fun CardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    trailing: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            autoCorrectEnabled = false,
        ),
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        // The card already pads horizontally.
        shape = RoundedCornerShape(0.dp),
    )
}

@Composable
fun CardDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

/** A switch row inside a card. */
@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Full-width filled button shared by the SMS report and guest lookup forms. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            disabledContentColor = Color.White,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Full-width outlined button with an optional leading icon. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}

/** Small secondary label with a leading icon, like SwiftUI's `Label` in caption style. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Outlined.Info, color: Color? = null) {
    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            Modifier.size(16.dp).padding(top = 1.dp),
            tint = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Section heading above a card. */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A numbered instruction. */
@Composable
fun NumberedStep(number: Int, title: String?, description: String, details: List<String> = emptyList()) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(22.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = if (title != null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            )
            for (detail in details) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A tappable row with an icon, a title and a trailing arrow, like a SwiftUI `Link` in a list. */
@Composable
fun LinkRow(text: String, icon: ImageVector, onClick: () -> Unit, trailing: ImageVector? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
        if (trailing != null) {
            Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
        }
    }
}

/** Back arrow for screens pushed on a tab's stack. */
@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
}

/** Share of the screen height a sheet may take; the rest keeps the screen behind visible. */
private const val SHEET_MAX_HEIGHT = 0.8f

/**
 * Bottom sheet used by every modal in the app. It opens fully but never covers
 * more than [SHEET_MAX_HEIGHT] of the screen; longer content scrolls inside.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CaseroSheet(
    onDismissRequest: () -> Unit,
    sheetState: androidx.compose.material3.SheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val screenHeight = with(androidx.compose.ui.platform.LocalDensity.current) {
        androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp()
    }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        // Limit the content, not the sheet: a shorter sheet still sits on the bottom edge.
        Column(Modifier.heightIn(max = screenHeight * SHEET_MAX_HEIGHT), content = content)
    }
}
