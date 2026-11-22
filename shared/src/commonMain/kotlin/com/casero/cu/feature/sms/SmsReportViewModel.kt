package com.casero.cu.feature.sms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.casero.cu.platform.MessageResult
import com.casero.cu.reporting.SmsReporter
import com.casero.cu.resources.Res
import com.casero.cu.resources.confirm_received_code
import com.casero.cu.resources.confirm_the_main_guest_s_registration_before
import com.casero.cu.resources.confirmation_sms_sent_the_report_is_complete
import com.casero.cu.resources.cuban_companion
import com.casero.cu.resources.foreign_guest
import com.casero.cu.resources.message_cancelled_no_report_was_confirmed
import com.casero.cu.resources.message_could_not_be_sent_check_cellular_service
import com.casero.cu.resources.request_sms_sent_wait_for_the_reply_check_the
import com.casero.cu.resources.the_sms_result_is_unknown_check_messages_before
import com.casero.cu.ui.UiText
import com.casero.cu.ui.toUiText
import org.jetbrains.compose.resources.StringResource

class SmsReportViewModel {
    enum class Kind(val label: StringResource) {
        Guest(Res.string.foreign_guest),
        Companion(Res.string.cuban_companion),
        Confirmation(Res.string.confirm_received_code),
    }

    /** An immutable snapshot of the message handed to the SMS app. */
    data class PreparedMessage(val body: String, val kind: Kind)

    var kind by mutableStateOf(Kind.Guest)
    var passport by mutableStateOf("")
    var companionIdentity by mutableStateOf("")
    var days by mutableStateOf("")
    var confirmationCode by mutableStateOf("")
    var mainGuestIsRegistered by mutableStateOf(false)
    var preparedMessage by mutableStateOf<PreparedMessage?>(null)
    var resultMessage by mutableStateOf<UiText?>(null)
        private set

    val preview: String? get() = preview(kind)

    fun preview(kind: Kind): String? = try {
        messageBody(kind)
    } catch (_: Exception) {
        null
    }

    val validationMessage: UiText? get() = validationMessage(kind)

    fun validationMessage(kind: Kind): UiText? = try {
        messageBody(kind)
        null
    } catch (error: MainGuestNotConfirmed) {
        UiText.of(Res.string.confirm_the_main_guest_s_registration_before)
    } catch (error: Exception) {
        error.toUiText()
    }

    private class MainGuestNotConfirmed : Exception()

    private fun messageBody(kind: Kind): String = when (kind) {
        Kind.Guest -> SmsReporter.reportBody(passport, days)
        Kind.Companion -> {
            val body = SmsReporter.reportBody(passport, days, companionIdentity)
            if (!mainGuestIsRegistered) throw MainGuestNotConfirmed()
            body
        }
        Kind.Confirmation -> SmsReporter.confirmationBody(confirmationCode)
    }

    fun prepare(kind: Kind) {
        val body = preview(kind) ?: return
        resultMessage = null
        preparedMessage = PreparedMessage(body, kind)
    }

    fun finished(result: MessageResult, sentKind: Kind) {
        preparedMessage = null
        resultMessage = UiText.of(
            when (result) {
                MessageResult.Sent -> if (sentKind == Kind.Confirmation) {
                    confirmationCode = ""
                    Res.string.confirmation_sms_sent_the_report_is_complete
                } else {
                    Res.string.request_sms_sent_wait_for_the_reply_check_the
                }
                MessageResult.Cancelled -> Res.string.message_cancelled_no_report_was_confirmed
                MessageResult.Failed -> Res.string.message_could_not_be_sent_check_cellular_service
                MessageResult.Unknown -> Res.string.the_sms_result_is_unknown_check_messages_before
            },
        )
    }
}
