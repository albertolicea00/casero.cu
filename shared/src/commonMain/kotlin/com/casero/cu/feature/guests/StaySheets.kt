package com.casero.cu.feature.guests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.casero.cu.ui.CaseroSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.casero.cu.app.AppState
import com.casero.cu.feature.register.RegisterGuestViewModel
import com.casero.cu.model.CompanionRegistration
import com.casero.cu.model.CubanId
import com.casero.cu.model.Guest
import com.casero.cu.model.PersonaValidation
import com.casero.cu.model.PortalDates
import com.casero.cu.network.PortalError
import com.casero.cu.resources.Res
import com.casero.cu.resources.add
import com.casero.cu.resources.add_companion
import com.casero.cu.resources.add_companion_2
import com.casero.cu.resources.adds_x_day_to_x_s_original_check_out_x_new_check
import com.casero.cu.resources.adds_x_days_to_x_s_original_check_out_x_new
import com.casero.cu.resources.cancel
import com.casero.cu.resources.carne_de_identidad
import com.casero.cu.resources.check_in
import com.casero.cu.resources.check_in_can_be_at_most_7_days_ago
import com.casero.cu.resources.check_out
import com.casero.cu.resources.check_out_must_be_on_or_after_check_in
import com.casero.cu.resources.check_out_updated
import com.casero.cu.resources.ci_has_11_digits
import com.casero.cu.resources.choose_a_new_check_out_within_the_stay_from_the
import com.casero.cu.resources.companion_of_x_stay_id_x
import com.casero.cu.resources.current_check_out
import com.casero.cu.resources.done
import com.casero.cu.resources.id
import com.casero.cu.resources.look_up
import com.casero.cu.resources.new_check_out
import com.casero.cu.resources.person_not_found
import com.casero.cu.resources.personal_data_contains_unsupported_characters
import com.casero.cu.resources.registered
import com.casero.cu.resources.removes_x_day_from_x_s_original_check_out_x_new
import com.casero.cu.resources.removes_x_days_from_x_s_original_check_out_x_new
import com.casero.cu.resources.stay_details
import com.casero.cu.resources.stay_id
import com.casero.cu.resources.stay_x
import com.casero.cu.resources.the_portal_returned_incomplete_personal_data_use
import com.casero.cu.resources.this_person_is_already_a_companion_in_this_stay
import com.casero.cu.resources.this_search_option_is_only_valid_for_cuban
import com.casero.cu.resources.unconfirmed
import com.casero.cu.resources.update
import com.casero.cu.resources.update_check_out
import com.casero.cu.resources.update_check_out_2
import com.casero.cu.resources.x_keeps_the_original_check_out_x
import com.casero.cu.resources.x_stay_id_x
import com.casero.cu.resources.x_will_leave_on_x
import com.casero.cu.ui.CardDivider
import com.casero.cu.ui.CardTextField
import com.casero.cu.ui.Caption
import com.casero.cu.ui.DateRow
import com.casero.cu.ui.Green
import com.casero.cu.ui.GuestStayText
import com.casero.cu.ui.Hint
import com.casero.cu.ui.InputCard
import com.casero.cu.ui.Orange
import com.casero.cu.ui.PersonDetails
import com.casero.cu.ui.PrimaryButton
import com.casero.cu.ui.Red
import com.casero.cu.ui.StayRange
import com.casero.cu.ui.UiText
import com.casero.cu.ui.formatMediumDate
import com.casero.cu.ui.toImageBitmapOrNull
import com.casero.cu.ui.toUiText
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.jetbrains.compose.resources.stringResource

@Composable
private fun SheetHeader(title: String, dismissLabel: String, dismissEnabled: Boolean, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDismiss, enabled = dismissEnabled) { Text(dismissLabel) }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Changes the check-out date of an active guest or companion, as the portal's
 * active-guests page does (`ActualizarFechaSalidaHuesped` /
 * `ActualizarFechaSalidaAcompannante`). The new date can't be before the
 * person's own check-in.
 */
@Composable
fun UpdateCheckOutSheet(
    app: AppState,
    person: Guest,
    companionId: String?,
    onDismiss: () -> Unit,
    onUpdated: (Instant) -> Unit,
) {
    /** Portal rule: the new check-out can be as early as the person's check-in day. */
    val earliest = PortalDates.day(person.checkIn ?: PortalDates.now())
    val current = person.checkOut?.let(PortalDates::day)
    var checkOut by remember { mutableStateOf(maxOf(current ?: earliest, earliest)) }
    var isConfirming by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<UiText?>(null) }
    var successMessage by remember { mutableStateOf<UiText?>(null) }
    val displayName = person.fullName.split('*', ' ').filter { it.isNotEmpty() }.joinToString(" ")

    // Writes run in app.tasks so the portal's answer is never dropped halfway.
    fun submit() = app.tasks.launch {
        isSubmitting = true
        errorMessage = null
        try {
            val instant = PortalDates.startOfDay(checkOut)
            val message = app.client.updateCheckOut(person.stayId, companionId, instant)
            successMessage = if (message.isEmpty()) UiText.of(Res.string.check_out_updated) else UiText.Raw(message)
            onUpdated(instant)
        } catch (error: CancellationException) {
            throw error
        } catch (_: PortalError.SessionExpired) {
            app.reportSessionExpired()
        } catch (error: Exception) {
            errorMessage = error.toUiText()
        } finally {
            isSubmitting = false
        }
    }

    CaseroSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !isSubmitting }),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(
                stringResource(Res.string.update_check_out),
                stringResource(if (successMessage == null) Res.string.cancel else Res.string.done),
                !isSubmitting,
                onDismiss,
            )
            Caption(stringResource(Res.string.x_stay_id_x, displayName, person.stayId))
            InputCard {
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StayDate(stringResource(Res.string.check_in), person.checkIn?.let { formatMediumDate(PortalDates.day(it)) }, Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 8.dp).size(16.dp))
                    StayDate(stringResource(Res.string.current_check_out), current?.let(::formatMediumDate), Modifier.weight(1f))
                }
                CardDivider()
                DateRow(stringResource(Res.string.new_check_out), checkOut, { checkOut = it }, min = earliest, enabled = successMessage == null)
                person.checkIn?.let { checkIn ->
                    CardDivider()
                    StayNights(PortalDates.day(checkIn), checkOut)
                }
            }
            val success = successMessage
            if (success != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(success.resolve(), color = Green, fontWeight = FontWeight.SemiBold)
                }
            } else {
                PrimaryButton(stringResource(Res.string.update_check_out), { isConfirming = true }, loading = isSubmitting)
                // The same date is allowed on purpose: resending it changes nothing and lets the
                // host check the request against the portal without touching the stay.
                Hint(stringResource(Res.string.choose_a_new_check_out_within_the_stay_from_the, formatMediumDate(earliest)), icon = Icons.Default.Lock)
            }
            errorMessage?.let { Hint(it.resolve(), icon = Icons.Default.Warning, color = Red) }
        }
    }

    if (isConfirming) {
        AlertDialog(
            onDismissRequest = { isConfirming = false },
            title = { Text(stringResource(Res.string.update_check_out_2)) },
            text = { Text(confirmationMessage(displayName, current, checkOut)) },
            confirmButton = {
                TextButton(onClick = {
                    isConfirming = false
                    submit()
                }) { Text(stringResource(Res.string.update)) }
            },
            dismissButton = { TextButton(onClick = { isConfirming = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

/** "Removes 2 days from ANA's original check-out (Nov 24, 2026). New check-out: Nov 22, 2026." */
@Composable
private fun confirmationMessage(name: String, current: LocalDate?, checkOut: LocalDate): String {
    val newDate = formatMediumDate(checkOut)
    if (current == null) return stringResource(Res.string.x_will_leave_on_x, name, newDate)
    val days = current.daysUntil(checkOut)
    val original = formatMediumDate(current)
    if (days == 0) return stringResource(Res.string.x_keeps_the_original_check_out_x, name, original)
    val format = when {
        days < -1 -> Res.string.removes_x_days_from_x_s_original_check_out_x_new
        days == -1 -> Res.string.removes_x_day_from_x_s_original_check_out_x_new
        days == 1 -> Res.string.adds_x_day_to_x_s_original_check_out_x_new_check
        else -> Res.string.adds_x_days_to_x_s_original_check_out_x_new
    }
    return stringResource(format, abs(days), name, original, newDate)
}

/** Same look as the dates of a row in Guests: title above, date below. */
@Composable
private fun StayDate(title: String, date: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            date ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (date == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StayNights(checkIn: LocalDate, checkOut: LocalDate) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Bedtime, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(Res.string.stay_x, GuestStayText.nights(checkIn, checkOut).resolve()),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Adds a Cuban companion (found by CI) to an active stay, as the portal's
 * active-guests page does: CI lookup, stay dates, confirmation, then
 * `RegistrarAcompannantes` with the guest's stay ID.
 */
@Composable
fun AddCompanionSheet(app: AppState, guest: Guest, existingIDs: Set<String>, onDismiss: () -> Unit, onAdded: () -> Unit) {
    var carne by remember { mutableStateOf("") }
    var person by remember { mutableStateOf<PersonaValidation?>(null) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLookingUp by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf<UiText?>(null) }
    var submitError by remember { mutableStateOf<UiText?>(null) }
    var isConfirming by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<List<Guest>?>(null) }

    /** Check-in can go back at most 7 days and never before the guest's own check-in. */
    val earliestCheckIn = maxOf(
        RegisterGuestViewModel.earliestCheckIn(),
        guest.checkIn?.let(PortalDates::day) ?: RegisterGuestViewModel.earliestCheckIn(),
    )
    // Default stay: from today (never before the guest's check-in) to the guest's check-out.
    var checkIn by remember { mutableStateOf(maxOf(PortalDates.today(), earliestCheckIn)) }
    var checkOut by remember { mutableStateOf(maxOf(guest.checkOut?.let(PortalDates::day) ?: checkIn, checkIn)) }
    val trimmedCarne = carne.trim()

    LaunchedEffect(carne) {
        person = null
        photo = null
        lookupError = null
        submitError = null
    }

    val validationMessage: UiText? = person?.let { found ->
        val response = found.response
        val fields = listOf(found.fullName, found.identificador, found.nationalityCode ?: "", found.sex ?: "")
        when {
            !response.isNullOrEmpty() && response != "OK" -> UiText.Raw(response)
            fields.any { it.isBlank() } || found.birthDate == null -> UiText.of(Res.string.the_portal_returned_incomplete_personal_data_use)
            fields.any { field -> field.any { it in ",|\n\r" } } -> UiText.of(Res.string.personal_data_contains_unsupported_characters)
            found.identificador.uppercase() in existingIDs -> UiText.of(Res.string.this_person_is_already_a_companion_in_this_stay)
            checkOut < checkIn -> UiText.of(Res.string.check_out_must_be_on_or_after_check_in)
            checkIn < earliestCheckIn -> UiText.of(Res.string.check_in_can_be_at_most_7_days_ago)
            else -> null
        }
    }

    fun lookUp() = app.tasks.launch {
        val query = trimmedCarne
        isLookingUp = true
        lookupError = null
        person = null
        photo = null
        try {
            val people = app.client.validateCompanionForStay(query)
            if (query != carne.trim()) return@launch
            val found = people.firstOrNull()
            if (found == null) {
                lookupError = UiText.of(Res.string.person_not_found)
                return@launch
            }
            person = found
            photo = try {
                app.client.fetchCompanionPhoto(found.identificador).toImageBitmapOrNull()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: PortalError.SessionExpired) {
            app.reportSessionExpired()
        } catch (error: Exception) {
            lookupError = error.toUiText()
        } finally {
            isLookingUp = false
        }
    }

    fun submit(found: PersonaValidation) = app.tasks.launch {
        val birthDate = found.birthDate ?: return@launch
        if (validationMessage != null) return@launch
        isSubmitting = true
        submitError = null
        val entry = CompanionRegistration(
            fullName = found.fullName,
            identificador = found.identificador,
            nationalityCode = found.nationalityCode ?: "",
            sex = found.sex ?: "",
            birthDate = birthDate,
            checkIn = PortalDates.startOfDay(checkIn),
            checkOut = PortalDates.startOfDay(checkOut),
        )
        try {
            val answer = app.client.addCompanions(listOf(entry), guest.stayId)
            result = answer
            if (answer.any { it.response == "OK" }) onAdded()
        } catch (error: CancellationException) {
            throw error
        } catch (_: PortalError.SessionExpired) {
            app.reportSessionExpired()
        } catch (error: Exception) {
            submitError = error.toUiText()
        } finally {
            isSubmitting = false
        }
    }

    CaseroSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !isSubmitting }),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(
                stringResource(Res.string.add_companion),
                stringResource(if (result == null) Res.string.cancel else Res.string.done),
                !isSubmitting,
                onDismiss,
            )
            Caption(stringResource(Res.string.companion_of_x_stay_id_x, guest.fullName, guest.stayId))

            val answer = result
            if (answer != null) {
                for (item in answer) {
                    val accepted = item.response == "OK"
                    InputCard {
                        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (accepted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (accepted) Green else Red,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (accepted) stringResource(Res.string.registered) else item.response ?: stringResource(Res.string.unconfirmed),
                                fontWeight = FontWeight.SemiBold,
                                color = if (accepted) Green else Red,
                            )
                        }
                        PersonDetails(item.fullName, item.identificador, person?.nationality, item.birthDate, photo)
                        CardDivider()
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Fact(stringResource(Res.string.stay_id), item.stayId, Modifier.weight(1f))
                            item.companionId?.let { Fact(stringResource(Res.string.id), it) }
                        }
                        val itemCheckIn = item.checkIn
                        val itemCheckOut = item.checkOut
                        if (itemCheckIn != null && itemCheckOut != null) {
                            CardDivider()
                            StayRange(itemCheckIn, itemCheckOut, Modifier.padding(vertical = 10.dp))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputCard {
                        CardTextField(carne, { carne = CubanId.sanitize(it) }, stringResource(Res.string.carne_de_identidad), keyboardType = KeyboardType.Number)
                    }
                    if (trimmedCarne.isNotEmpty() && !CubanId.isValid(trimmedCarne)) {
                        Hint(stringResource(Res.string.ci_has_11_digits), icon = Icons.Default.Warning, color = Orange)
                    }
                    Hint(stringResource(Res.string.this_search_option_is_only_valid_for_cuban))
                    PrimaryButton(
                        stringResource(Res.string.look_up),
                        ::lookUp,
                        enabled = CubanId.isValid(trimmedCarne) && !isSubmitting,
                        loading = isLookingUp,
                    )
                    lookupError?.let { Hint(it.resolve(), icon = Icons.Default.Warning, color = Red) }
                }
                person?.let { found ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        InputCard { PersonDetails(found.fullName, found.identificador, found.nationality, found.birthDate, photo) }
                        Text(stringResource(Res.string.stay_details), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium)
                        InputCard {
                            DateRow(stringResource(Res.string.check_in), checkIn, { checkIn = it }, min = earliestCheckIn)
                            CardDivider()
                            DateRow(stringResource(Res.string.check_out), checkOut, { checkOut = it }, min = checkIn)
                            CardDivider()
                            StayNights(checkIn, checkOut)
                        }
                        validationMessage?.let { Text(it.resolve(), Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall, color = Red) }
                        PrimaryButton(
                            stringResource(Res.string.add_companion),
                            { isConfirming = true },
                            enabled = validationMessage == null,
                            loading = isSubmitting,
                        )
                        submitError?.let { Hint(it.resolve(), icon = Icons.Default.Warning, color = Red) }
                    }
                }
            }
        }
    }

    val confirming = person
    if (isConfirming && confirming != null) {
        AlertDialog(
            onDismissRequest = { isConfirming = false },
            title = { Text(stringResource(Res.string.add_companion_2)) },
            text = {
                Text(
                    "${confirming.fullName}\n${confirming.identificador} · ${formatMediumDate(checkIn)} → ${formatMediumDate(checkOut)} · " +
                        GuestStayText.nights(checkIn, checkOut).resolve(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    isConfirming = false
                    submit(confirming)
                }) { Text(stringResource(Res.string.add)) }
            },
            dismissButton = { TextButton(onClick = { isConfirming = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun Fact(title: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text("$title: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(value, fontWeight = FontWeight.Bold) }
    }
}
