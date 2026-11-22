package com.casero.cu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.casero.cu.resources.Res
import com.casero.cu.resources.cancel
import com.casero.cu.resources.done
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private fun LocalDate.utcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun localDateOfUtcMillis(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

/** A labelled row showing a date; tapping it opens a calendar limited to [min]…[max]. */
@Composable
fun DateRow(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    min: LocalDate? = null,
    max: LocalDate? = null,
    enabled: Boolean = true,
) {
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.clickable(enabled = enabled) { picking = true },
        ) {
            Text(
                formatMediumDate(date),
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
    if (picking) {
        DatePickerSheet(
            initial = date,
            min = min,
            max = max,
            onDismiss = { picking = false },
            onConfirm = {
                picking = false
                onDateChange(it)
            },
        )
    }
}

@Composable
fun DatePickerSheet(
    initial: LocalDate,
    min: LocalDate?,
    max: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val minMillis = min?.utcMillis()
    val maxMillis = max?.utcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.utcMillis(),
        yearRange = (min?.year ?: 1900)..(max?.year ?: 2100),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                (minMillis == null || utcTimeMillis >= minMillis) && (maxMillis == null || utcTimeMillis <= maxMillis)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onConfirm(localDateOfUtcMillis(it)) } ?: onDismiss() }) {
                Text(stringResource(Res.string.done))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}
