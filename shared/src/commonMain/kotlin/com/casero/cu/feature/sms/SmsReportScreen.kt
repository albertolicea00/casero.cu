package com.casero.cu.feature.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.casero.cu.network.CaseroClient
import com.casero.cu.platform.MessageComposer
import com.casero.cu.reporting.SmsReporter
import com.casero.cu.resources.Res
import com.casero.cu.resources.after_sending
import com.casero.cu.resources.associate_your_host_phone
import com.casero.cu.resources.before_the_first_report
import com.casero.cu.resources.call_18808
import com.casero.cu.resources.call_18808_from_the_phone_associated_with_your
import com.casero.cu.resources.check_the_name_and_nationality_in_the_service
import com.casero.cu.resources.check_the_reply_and_confirm
import com.casero.cu.resources.companion
import com.casero.cu.resources.companion_identity_number
import com.casero.cu.resources.confirm_received_code
import com.casero.cu.resources.confirmation_code_without_hc
import com.casero.cu.resources.done
import com.casero.cu.resources.each_guest_and_companion_needs_a_separate_report
import com.casero.cu.resources.fill_in_the_fields_above_to_send_the_sms
import com.casero.cu.resources.for_a_guest_enter_the_passport_and_stay_days_for
import com.casero.cu.resources.guest
import com.casero.cu.resources.guest_passport
import com.casero.cu.resources.how_to_report_by_sms
import com.casero.cu.resources.if_you_already_have_an_account_associate_or
import com.casero.cu.resources.instructions_and_portal
import com.casero.cu.resources.manage_phone_in_settings
import com.casero.cu.resources.open_official_portal
import com.casero.cu.resources.prepare_the_first_message
import com.casero.cu.resources.preview_sms
import com.casero.cu.resources.sms_does_not_require_internet_but_needs_cellular
import com.casero.cu.resources.sms_procedure_reference
import com.casero.cu.resources.sms_report_information
import com.casero.cu.resources.sms_reports
import com.casero.cu.resources.stay_days
import com.casero.cu.resources.tap_preview_sms_to_open_the_prepared_message_in
import com.casero.cu.resources.the_main_guest_s_registration_is_confirmed
import com.casero.cu.resources.the_report_is_complete_only_after_the_service
import com.casero.cu.resources.this_device_cannot_send_sms_use_the_preview_on
import com.casero.cu.resources.use_the_phone_number_associated_with_your_host
import com.casero.cu.resources.wait_for_the_registration_code
import com.casero.cu.resources.wait_for_the_service_reply_verify_the_guest_s
import com.casero.cu.ui.Card
import com.casero.cu.ui.CardDivider
import com.casero.cu.ui.CardTextField
import com.casero.cu.ui.Caption
import com.casero.cu.ui.Hint
import com.casero.cu.ui.InputCard
import com.casero.cu.ui.LinkRow
import com.casero.cu.ui.NumberedStep
import com.casero.cu.ui.PrimaryButton
import com.casero.cu.ui.SecondaryButton
import com.casero.cu.ui.SectionHeading
import com.casero.cu.ui.ToggleRow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Carrier SMS reports remain accessible without a portal account or mobile data. */
@Composable
fun SmsReportScreen(
    model: SmsReportViewModel,
    messages: MessageComposer,
    onOpenSettings: () -> Unit,
    onOpenCubatramiteGuide: () -> Unit,
) {
    var selectedKind by remember {
        mutableStateOf(if (model.kind == SmsReportViewModel.Kind.Companion) model.kind else SmsReportViewModel.Kind.Guest)
    }
    var isInfoPresented by remember { mutableStateOf(false) }
    val canSendText = messages.canSendText

    var seenPassport by remember { mutableStateOf(model.passport) }
    LaunchedEffect(model.passport) {
        if (model.passport != seenPassport) {
            seenPassport = model.passport
            model.mainGuestIsRegistered = false
        }
    }
    val prepared = model.preparedMessage
    LaunchedEffect(prepared) {
        if (prepared != null) {
            val result = messages.compose(SmsReporter.SMS_DESTINATION, prepared.body)
            model.finished(result, prepared.kind)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.sms_reports)) },
                actions = {
                    IconButton(onClick = { isInfoPresented = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(Res.string.sms_report_information))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Caption(stringResource(Res.string.tap_preview_sms_to_open_the_prepared_message_in))

            val kinds = listOf(SmsReportViewModel.Kind.Guest, SmsReportViewModel.Kind.Companion)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                kinds.forEachIndexed { index, kind ->
                    SegmentedButton(
                        selected = selectedKind == kind,
                        onClick = {
                            selectedKind = kind
                            model.kind = kind
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                    ) {
                        Text(stringResource(if (kind == SmsReportViewModel.Kind.Guest) Res.string.guest else Res.string.companion))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InputCard {
                    CardTextField(
                        model.passport,
                        { model.passport = it },
                        stringResource(Res.string.guest_passport),
                        capitalization = KeyboardCapitalization.Characters,
                    )
                    if (selectedKind == SmsReportViewModel.Kind.Companion) {
                        CardDivider()
                        CardTextField(
                            model.companionIdentity,
                            { model.companionIdentity = it },
                            stringResource(Res.string.companion_identity_number),
                            keyboardType = KeyboardType.Number,
                        )
                        CardDivider()
                        ToggleRow(
                            stringResource(Res.string.the_main_guest_s_registration_is_confirmed),
                            model.mainGuestIsRegistered,
                            { model.mainGuestIsRegistered = it },
                        )
                    }
                    CardDivider()
                    CardTextField(
                        model.days,
                        { model.days = it },
                        stringResource(Res.string.stay_days),
                        keyboardType = KeyboardType.Number,
                    )
                }
                model.validationMessage(selectedKind)?.let { Hint(it.resolve()) }
            }
            SendButton(model, selectedKind, canSendText)

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.confirm_received_code), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.check_the_name_and_nationality_in_the_service),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InputCard {
                        CardTextField(
                            model.confirmationCode,
                            { model.confirmationCode = it },
                            stringResource(Res.string.confirmation_code_without_hc),
                            capitalization = KeyboardCapitalization.Characters,
                        )
                    }
                    model.validationMessage(SmsReportViewModel.Kind.Confirmation)?.let { Hint(it.resolve()) }
                }
                SendButton(model, SmsReportViewModel.Kind.Confirmation, canSendText)
            }

            if (!canSendText) {
                Hint(stringResource(Res.string.this_device_cannot_send_sms_use_the_preview_on), icon = Icons.Default.Warning)
            }

            model.resultMessage?.let { result ->
                Card { Text(result.resolve(), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }

    if (isInfoPresented) {
        SmsReportInfoSheet(
            onDismiss = { isInfoPresented = false },
            onOpenSettings = onOpenSettings,
            onOpenCubatramiteGuide = onOpenCubatramiteGuide,
        )
    }
}

@Composable
private fun SendButton(model: SmsReportViewModel, kind: SmsReportViewModel.Kind, canSendText: Boolean) {
    val preview = model.preview(kind)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryButton(
            stringResource(Res.string.preview_sms),
            onClick = { model.prepare(kind) },
            enabled = preview != null && canSendText,
        )
        if (preview == null) Hint(stringResource(Res.string.fill_in_the_fields_above_to_send_the_sms), icon = Icons.Default.Lock)
    }
}

@Composable
private fun SmsReportInfoSheet(onDismiss: () -> Unit, onOpenSettings: () -> Unit, onOpenCubatramiteGuide: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            then()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.sms_report_information),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { close() }) { Text(stringResource(Res.string.done)) }
            }

            SectionHeading(stringResource(Res.string.before_the_first_report))
            Card {
                Text(stringResource(Res.string.use_the_phone_number_associated_with_your_host))
                SmsReporter.dialUri(SmsReporter.ASSOCIATION_PHONE)?.let { uri ->
                    CardDivider()
                    LinkRow(stringResource(Res.string.call_18808), Icons.Default.Phone, { uriHandler.openUri(uri) }, Icons.AutoMirrored.Filled.OpenInNew)
                    CardDivider()
                }
                Text(
                    stringResource(Res.string.sms_does_not_require_internet_but_needs_cellular),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionHeading(stringResource(Res.string.after_sending))
            Card {
                Text(stringResource(Res.string.each_guest_and_companion_needs_a_separate_report))
                CardDivider()
                Text(
                    stringResource(Res.string.the_report_is_complete_only_after_the_service),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionHeading(stringResource(Res.string.how_to_report_by_sms))
            Card(spacing = 14) {
                NumberedStep(1, stringResource(Res.string.associate_your_host_phone), stringResource(Res.string.call_18808_from_the_phone_associated_with_your))
                NumberedStep(2, stringResource(Res.string.prepare_the_first_message), stringResource(Res.string.for_a_guest_enter_the_passport_and_stay_days_for))
                NumberedStep(3, stringResource(Res.string.check_the_reply_and_confirm), stringResource(Res.string.wait_for_the_service_reply_verify_the_guest_s))
                NumberedStep(4, stringResource(Res.string.wait_for_the_registration_code), stringResource(Res.string.the_report_is_complete_only_after_the_service))
                CardDivider()
                SelectionContainer {
                    Text(
                        "Guest: HC<PASSPORT> <STAY_DAYS>\nCompanion: HC<MAIN_PASSPORT> <ID_NUMBER> <STAY_DAYS>\nConfirm: HC<CONFIRMATION_CODE>",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionHeading(stringResource(Res.string.instructions_and_portal))
            Card(spacing = 8) {
                Text(stringResource(Res.string.if_you_already_have_an_account_associate_or), style = MaterialTheme.typography.bodyMedium)
                CardDivider()
                LinkRow(
                    stringResource(Res.string.manage_phone_in_settings),
                    Icons.Default.Settings,
                    { close(onOpenSettings) },
                    Icons.AutoMirrored.Filled.OpenInNew,
                )
            }

            SecondaryButton(stringResource(Res.string.open_official_portal), { uriHandler.openUri(CaseroClient.BASE_URL) }, icon = Icons.Default.Public)
            SecondaryButton(stringResource(Res.string.sms_procedure_reference), { close(onOpenCubatramiteGuide) }, icon = Icons.AutoMirrored.Filled.MenuBook)
        }
    }
}

