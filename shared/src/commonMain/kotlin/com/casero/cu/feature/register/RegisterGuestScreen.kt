package com.casero.cu.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.casero.cu.ui.CaseroSheet
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.casero.cu.app.AppState
import com.casero.cu.export.ExportService
import com.casero.cu.model.Guest
import com.casero.cu.model.GuestRegistration
import com.casero.cu.model.PersonaValidation
import com.casero.cu.network.CaseroClient
import com.casero.cu.resources.Res
import com.casero.cu.resources.add_to_registration
import com.casero.cu.resources.cancel
import com.casero.cu.resources.check_in
import com.casero.cu.resources.check_names_documents_and_dates_this_app_cannot
import com.casero.cu.resources.check_out
import com.casero.cu.resources.choose_person
import com.casero.cu.resources.complete_in_official_portal
import com.casero.cu.resources.confirm_person
import com.casero.cu.resources.copied
import com.casero.cu.resources.copy_stay_id
import com.casero.cu.resources.fill_in_the_fields_above_to_look_up
import com.casero.cu.resources.first_name_only_the_first_one
import com.casero.cu.resources.first_surname
import com.casero.cu.resources.if_it_keeps_failing_report_these_people_by_sms
import com.casero.cu.resources.last_result
import com.casero.cu.resources.look_up
import com.casero.cu.resources.look_up_each_person_set_the_stay_dates_and_add
import com.casero.cu.resources.look_up_guests_and_companions_in_the_official
import com.casero.cu.resources.n_1_person_will_be_reported_to_the_official_portal
import com.casero.cu.resources.ready_to_register_x
import com.casero.cu.resources.register_1_person
import com.casero.cu.resources.register_guest
import com.casero.cu.resources.register_guest_information
import com.casero.cu.resources.register_guests_in_the_portal
import com.casero.cu.resources.register_people
import com.casero.cu.resources.register_x_people
import com.casero.cu.resources.registered
import com.casero.cu.resources.remove
import com.casero.cu.resources.remove_from_registration
import com.casero.cu.resources.review_registration
import com.casero.cu.resources.save_pdf
import com.casero.cu.resources.second_surname
import com.casero.cu.resources.select_a_person_above_to_add_them
import com.casero.cu.resources.sent_without_confirmation_check_guests_before
import com.casero.cu.resources.stay_details
import com.casero.cu.resources.stay_id
import com.casero.cu.resources.stay_x
import com.casero.cu.resources.try_without_the_second_given_name_example_juan
import com.casero.cu.resources.unconfirmed
import com.casero.cu.resources.x_people_match_this_search
import com.casero.cu.resources.x_people_will_be_reported_to_the_official_portal
import com.casero.cu.ui.CardDivider
import com.casero.cu.ui.CardTextField
import com.casero.cu.ui.Caption
import com.casero.cu.ui.DateRow
import com.casero.cu.ui.GuestStayText
import com.casero.cu.ui.Green
import com.casero.cu.ui.Hint
import com.casero.cu.ui.InputCard
import com.casero.cu.ui.NationalityFlag
import com.casero.cu.ui.Orange
import com.casero.cu.ui.PersonDetails
import com.casero.cu.ui.PortalLink
import com.casero.cu.ui.PrimaryButton
import com.casero.cu.ui.Red
import com.casero.cu.ui.SecondaryButton
import com.casero.cu.ui.SectionInfoSheet
import com.casero.cu.ui.StayRange
import com.casero.cu.ui.Yellow
import com.casero.cu.ui.toImageBitmapOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** State of the Register tab: the queue must survive switching tabs. */
class RegisterTab(app: AppState) {
    val model = RegisterGuestViewModel(app.client)

    /** Photos seen in the result sheet, reused on the queue cards and in the PDFs. */
    val queuePhotos = mutableStateMapOf<String, ImageBitmap?>()
    val photoBytes = mutableStateMapOf<String, ByteArray>()
}

@Composable
fun RegisterGuestScreen(app: AppState, tab: RegisterTab) {
    val model = tab.model
    val uriHandler = LocalUriHandler.current
    var isInfoPresented by remember { mutableStateOf(false) }
    val queuePhotos = tab.queuePhotos
    val photoBytes = tab.photoBytes
    var pendingRemoval by remember { mutableStateOf<GuestRegistration?>(null) }
    var isConfirmingSubmit by remember { mutableStateOf(false) }

    var seenQuery by remember { mutableStateOf(model.lookupKind to model.lookupQuery) }
    LaunchedEffect(model.lookupKind, model.lookupQuery) {
        if (seenQuery != model.lookupKind to model.lookupQuery) {
            seenQuery = model.lookupKind to model.lookupQuery
            model.clearLookup()
        }
    }
    LaunchedEffect(model.isSessionExpired) {
        if (model.isSessionExpired) {
            app.reportSessionExpired()
            model.acknowledgeSessionExpired()
        }
    }

    fun savePdf(record: ExportService.RegistrationRecord) {
        app.tasks.launch {
            val data = app.exports.registrationPdf(record, photoBytes.toMap())
            val file = app.platform.temporaryDirectory / ExportService.registrationFileName()
            app.platform.fileSystem.write(file) { write(data) }
            app.platform.files.share(file, "application/pdf")
        }
    }

    /**
     * Same photo endpoints as Guests: Cuban nationals (CI lookup) by carné, everyone
     * else through the foreign registry. A missing photo just keeps the initials.
     */
    suspend fun photo(person: PersonaValidation): ByteArray? = try {
        if (model.lookupKind == RegisterGuestViewModel.LookupKind.CubanCompanion) {
            app.client.fetchCompanionPhoto(person.identificador)
        } else {
            app.client.fetchForeignGuestPhoto(person.identificador, person.fullName, person.nationalityCode ?: "", person.sex ?: "")
        }
    } catch (_: Exception) {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.register_guest)) },
                actions = {
                    IconButton(onClick = { isInfoPresented = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(Res.string.register_guest_information))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Caption(stringResource(Res.string.look_up_each_person_set_the_stay_dates_and_add))
            TextButton(onClick = { uriHandler.openUri(CaseroClient.portalUrl("Huespedes")) }) {
                Icon(Icons.Default.Public, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.complete_in_official_portal))
            }

            val kinds = RegisterGuestViewModel.LookupKind.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                kinds.forEachIndexed { index, kind ->
                    SegmentedButton(
                        selected = model.lookupKind == kind,
                        onClick = { model.lookupKind = kind },
                        shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                        icon = {},
                    ) { Text(stringResource(kind.segmentLabel), maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                }
            }

            LookupInput(model)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PrimaryButton(
                    stringResource(Res.string.look_up),
                    onClick = { app.tasks.launch { model.lookUp() } },
                    enabled = model.canLookUp,
                    loading = model.isLookingUp,
                )
                val lookupError = model.lookupErrorMessage
                if (lookupError != null) {
                    Hint(lookupError.resolve(), icon = Icons.Default.Warning, color = Red)
                } else if (!model.canLookUp && !model.isLookingUp) {
                    Hint(stringResource(Res.string.fill_in_the_fields_above_to_look_up), icon = Icons.Default.Lock)
                }
            }

            if (model.queue.isNotEmpty()) {
                Section(stringResource(Res.string.ready_to_register_x, model.queue.size)) {
                    for (entry in model.queue.toList()) {
                        RegistrationCard(
                            entry,
                            queuePhotos[entry.identificador],
                            model.isUncertain(entry),
                            onRemove = if (model.isSubmitting) null else ({ pendingRemoval = entry }),
                        )
                    }
                    model.companionWarning?.let { warning ->
                        // Warning only, sending stays enabled: same yellow as the sign-in banner.
                        Row(
                            Modifier.fillMaxWidth().background(Yellow.copy(alpha = 0.25f), RoundedCornerShape(10.dp)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(warning.resolve(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                    PrimaryButton(
                        stringResource(Res.string.register_people),
                        onClick = { isConfirmingSubmit = true },
                        enabled = model.canSubmit,
                        loading = model.isSubmitting,
                    )
                }
            }

            model.lastSubmission?.let { results ->
                Section(stringResource(Res.string.last_result)) {
                    for (guest in results) {
                        RegistrationResultCard(guest, displayName(guest.fullName), queuePhotos[guest.identificador])
                    }
                    SecondaryButton(stringResource(Res.string.save_pdf), { savePdf(ExportService.RegistrationRecord.Submitted(results)) }, icon = Icons.Default.Download)
                }
            }

            model.errorMessage?.let { message ->
                val reason = message.resolve()
                Text(reason, Modifier.padding(horizontal = 4.dp), color = Red, style = MaterialTheme.typography.bodyMedium)
                if (model.queue.isNotEmpty()) {
                    // What to do next and a PDF of the pending people with the error, to finish elsewhere.
                    Hint(stringResource(Res.string.if_it_keeps_failing_report_these_people_by_sms), icon = Icons.Default.AccountBalance)
                    SecondaryButton(
                        stringResource(Res.string.save_pdf),
                        {
                            savePdf(
                                ExportService.RegistrationRecord.Failed(model.queue.toList(), reason, model.uncertainIDs.isNotEmpty()),
                            )
                        },
                        icon = Icons.Default.Download,
                    )
                }
            }
        }
    }

    if (isInfoPresented) {
        SectionInfoSheet(
            title = stringResource(Res.string.register_guest),
            summary = stringResource(Res.string.look_up_guests_and_companions_in_the_official),
            links = listOf(PortalLink(Res.string.register_guests_in_the_portal, "Huespedes")),
            onDismiss = { isInfoPresented = false },
        )
    }

    if (model.isShowingResult) {
        LookupResultSheet(
            model = model,
            loadPhoto = { person -> photo(person) },
            onAdded = { id, bytes ->
                queuePhotos[id] = bytes?.toImageBitmapOrNull()
                if (bytes != null) photoBytes[id] = bytes
            },
        )
    }

    if (isConfirmingSubmit) {
        SubmitSummarySheet(
            entries = model.queue.toList(),
            photos = queuePhotos,
            isUncertain = model::isUncertain,
            onCancel = { isConfirmingSubmit = false },
            onConfirm = {
                isConfirmingSubmit = false
                // Not tied to this screen: leaving the tab must not cut off a sent registration.
                app.tasks.launch { if (model.submit()) app.guestsDidChange() }
            },
            onSavePdf = { savePdf(ExportService.RegistrationRecord.Prepared(model.queue.toList())) },
        )
    }

    pendingRemoval?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(Res.string.remove_from_registration)) },
            text = { Text(listOfNotNull(fullName(entry), NationalityFlag.emoji(entry.nationalityName)).joinToString(" ")) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRemoval = null
                    queuePhotos.remove(entry.identificador)
                    photoBytes.remove(entry.identificador)
                    model.removeFromQueue(entry)
                }) { Text(stringResource(Res.string.remove), color = Red) }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun LookupInput(model: RegisterGuestViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InputCard {
            if (model.lookupKind == RegisterGuestViewModel.LookupKind.ForeignGuestByName) {
                CardTextField(model.firstName, { model.firstName = it }, stringResource(Res.string.first_name_only_the_first_one), capitalization = KeyboardCapitalization.Characters)
                CardDivider()
                CardTextField(model.lastName1, { model.lastName1 = it }, stringResource(Res.string.first_surname), capitalization = KeyboardCapitalization.Characters)
                CardDivider()
                CardTextField(model.lastName2, { model.lastName2 = it }, stringResource(Res.string.second_surname), capitalization = KeyboardCapitalization.Characters)
            } else {
                CardTextField(
                    model.identificador,
                    { model.identificador = it },
                    stringResource(model.lookupKind.fieldLabel),
                    keyboardType = if (model.lookupKind == RegisterGuestViewModel.LookupKind.CubanCompanion) KeyboardType.Number else KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Characters,
                )
            }
        }
        Hint(stringResource(model.lookupKind.help))
        if (model.lookupKind == RegisterGuestViewModel.LookupKind.ForeignGuestByName) {
            // The portal matches on the first given name only; a second name finds nobody.
            Hint(stringResource(Res.string.try_without_the_second_given_name_example_juan), icon = Icons.Default.Lightbulb)
        }
    }
}

/** Headline + content block, matching the SMS confirmation section. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

/** `RegistrarHuesped` echoes the wire name (`NOMBRE**APELLIDO1*APELLIDO2`) as `Biografico`. */
private fun displayName(wireName: String): String = wireName.split('*', ' ').filter { it.isNotEmpty() }.joinToString(" ")

private fun fullName(entry: GuestRegistration): String =
    "${entry.firstName} ${entry.middleName} ${entry.lastName1} ${entry.lastName2}".split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

/**
 * A queued person: the same card as the result sheet plus the stay range. Shown
 * in Ready to register (with the trash) and in the review sheet (without it).
 */
@Composable
private fun RegistrationCard(entry: GuestRegistration, photo: ImageBitmap?, isUncertain: Boolean, onRemove: (() -> Unit)?) {
    InputCard {
        if (isUncertain) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(Res.string.sent_without_confirmation_check_guests_before),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Orange,
                )
            }
        }
        PersonDetails(fullName(entry), entry.identificador, entry.nationalityName, entry.birthDate, photo)
        CardDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            StayRange(entry.checkIn, entry.checkOut, Modifier.weight(1f))
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.remove), tint = Red)
                }
            }
        }
    }
}

/**
 * One person of a `RegistrarHuesped` answer: status, the person card and the
 * stay ID the portal assigned (companions get `<guest stay>.1`, `.2`, …), which
 * can be copied.
 */
@Composable
private fun RegistrationResultCard(guest: Guest, name: String, photo: ImageBitmap?) {
    val accepted = guest.response == "OK"
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }
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
                if (accepted) stringResource(Res.string.registered) else guest.response ?: stringResource(Res.string.unconfirmed),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (accepted) Green else Red,
            )
        }
        PersonDetails(name, guest.identificador, guest.nationality, guest.birthDate, photo)
        if (guest.stayId.isNotEmpty()) {
            CardDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${stringResource(Res.string.stay_id)}: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer(Modifier.weight(1f)) { Text(guest.stayId, fontWeight = FontWeight.Bold) }
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(guest.stayId))
                    copied = true
                }) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = stringResource(if (copied) Res.string.copied else Res.string.copy_stay_id),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        val checkIn = guest.checkIn
        val checkOut = guest.checkOut
        if (checkIn != null && checkOut != null) {
            CardDivider()
            StayRange(checkIn, checkOut, Modifier.padding(vertical = 10.dp))
        }
    }
}

/**
 * Last check before reporting: every person with document, origin and stay,
 * so the host reviews exactly what is about to reach the portal.
 */
@Composable
private fun SubmitSummarySheet(
    entries: List<GuestRegistration>,
    photos: Map<String, ImageBitmap?>,
    isUncertain: (GuestRegistration) -> Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onSavePdf: () -> Unit,
) {
    CaseroSheet(onDismissRequest = onCancel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
                Text(stringResource(Res.string.review_registration), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            }
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Caption(
                    if (entries.size == 1) {
                        stringResource(Res.string.n_1_person_will_be_reported_to_the_official_portal)
                    } else {
                        stringResource(Res.string.x_people_will_be_reported_to_the_official_portal, entries.size)
                    },
                )
                for (entry in entries) RegistrationCard(entry, photos[entry.identificador], isUncertain(entry), onRemove = null)
                Hint(stringResource(Res.string.check_names_documents_and_dates_this_app_cannot), icon = Icons.Default.Warning)
            }
            Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    if (entries.size == 1) stringResource(Res.string.register_1_person) else stringResource(Res.string.register_x_people, entries.size),
                    onClick = onConfirm,
                )
                SecondaryButton(stringResource(Res.string.save_pdf), onSavePdf, icon = Icons.Default.Download)
            }
        }
    }
}

/**
 * Lookup results: the people on top (a single match is already active, several
 * are tapped to pick one) and, apart below, the stay dates and Add to registration.
 */
@Composable
private fun LookupResultSheet(
    model: RegisterGuestViewModel,
    loadPhoto: suspend (PersonaValidation) -> ByteArray?,
    onAdded: (String, ByteArray?) -> Unit,
) {
    val photos = remember { mutableStateMapOf<String, ByteArray?>() }
    val hasSeveral = model.candidates.size > 1
    CaseroSheet(onDismissRequest = model::dismissResult, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = model::dismissResult) { Text(stringResource(Res.string.cancel)) }
                Text(
                    stringResource(if (hasSeveral) Res.string.choose_person else Res.string.confirm_person),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (hasSeveral) Caption(stringResource(Res.string.x_people_match_this_search, model.candidates.size))
            for (person in model.candidates) {
                val found = model.foundPerson
                val active = found != null && found.identificador == person.identificador && found.fullName == person.fullName
                LaunchedEffect(person.identificador) {
                    if (!photos.containsKey(person.identificador)) photos[person.identificador] = loadPhoto(person)
                }
                Box(
                    Modifier
                        .clickable { model.select(person) }
                        .border(if (active && hasSeveral) 2.dp else 0.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (active && hasSeveral) 1f else 0f), RoundedCornerShape(16.dp)),
                ) {
                    InputCard {
                        PersonDetails(
                            person.fullName,
                            person.identificador,
                            person.nationality,
                            person.birthDate,
                            photos[person.identificador]?.toImageBitmapOrNull(),
                        )
                    }
                    if (hasSeveral) {
                        Icon(
                            if (active) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        )
                    }
                }
            }

            Text(stringResource(Res.string.stay_details), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
            InputCard {
                DateRow(stringResource(Res.string.check_in), model.checkIn, { model.checkIn = it }, min = model.earliestCheckIn)
                CardDivider()
                DateRow(stringResource(Res.string.check_out), model.checkOut, { model.checkOut = it }, min = model.checkIn)
                CardDivider()
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.stay_x, GuestStayText.nights(model.checkIn, model.checkOut).resolve()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            model.validationMessage?.let { Text(it.resolve(), Modifier.padding(horizontal = 4.dp), color = Red, style = MaterialTheme.typography.bodySmall) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    stringResource(Res.string.add_to_registration),
                    onClick = {
                        val person = model.foundPerson ?: return@PrimaryButton
                        val photo = photos[person.identificador]
                        model.addToQueue()
                        onAdded(person.identificador, photo)
                    },
                    enabled = model.canAdd,
                )
                if (model.foundPerson == null) Hint(stringResource(Res.string.select_a_person_above_to_add_them), icon = Icons.Default.TouchApp)
            }
        }
    }
}

