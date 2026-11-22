package com.casero.cu.ui

import androidx.compose.runtime.Composable
import com.casero.cu.network.PortalError
import com.casero.cu.network.RejectionContext
import com.casero.cu.reporting.SmsReporter
import com.casero.cu.resources.Res
import com.casero.cu.resources.app_session_expired_please_sign_in_again
import com.casero.cu.resources.could_not_connect_to_the_portal_check_your
import com.casero.cu.resources.could_not_validate_the_portal_form_please_try
import com.casero.cu.resources.enter_a_passport_id_using_only_letters_and
import com.casero.cu.resources.enter_a_positive_whole_number_of_stay_days
import com.casero.cu.resources.enter_only_the_received_confirmation_code
import com.casero.cu.resources.enter_the_companion_s_11_digit_identity_number
import com.casero.cu.resources.invalid_username_or_password
import com.casero.cu.resources.person_not_found
import com.casero.cu.resources.portal_returned_http_x
import com.casero.cu.resources.the_connection_was_lost_after_sending_the_portal
import com.casero.cu.resources.the_https_connection_to_the_portal_ended_before
import com.casero.cu.resources.the_portal_did_not_change_the_check_out_date
import com.casero.cu.resources.the_portal_rejected_the_registration
import com.casero.cu.resources.the_portal_rejected_the_request
import com.casero.cu.resources.the_portal_returned_an_unexpected_response
import com.casero.cu.resources.the_portal_security_certificate_is_missing_from
import com.casero.cu.resources.the_report_is_too_long_for_one_sms_check_the
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Text for the UI that is either the portal's own words or one of the app's localized strings. */
sealed interface UiText {
    data class Raw(val text: String) : UiText

    class Resource(val res: StringResource, vararg val args: Any) : UiText

    /** Several texts separated by a space. */
    data class Joined(val parts: List<UiText>) : UiText

    /** A one-argument format whose argument is several texts joined by commas. */
    data class Listed(val format: StringResource, val parts: List<UiText>) : UiText

    @Composable
    fun resolve(): String = when (this) {
        is Raw -> text
        is Resource -> stringResource(res, *args)
        is Joined -> parts.map { it.resolve() }.joinToString(" ")
        is Listed -> stringResource(format, parts.map { it.resolve() }.joinToString(", "))
    }

    suspend fun load(): String = when (this) {
        is Raw -> text
        is Resource -> getString(res, *args)
        is Joined -> parts.map { it.load() }.joinToString(" ")
        is Listed -> getString(format, parts.map { it.load() }.joinToString(", "))
    }

    companion object {
        fun of(res: StringResource, vararg args: Any): UiText = Resource(res, *args)
    }
}

/** User-facing text for any error a feature catches. */
fun Throwable.toUiText(): UiText = when (this) {
    is PortalError -> toUiText()
    is SmsReporter.InvalidReport -> reason.toUiText()
    else -> UiText.Raw(message ?: toString())
}

fun PortalError.toUiText(): UiText = when (this) {
    is PortalError.Rejected -> portalMessage?.takeIf { it.isNotBlank() }?.let(UiText::Raw) ?: UiText.of(
        when (context) {
            RejectionContext.Request -> Res.string.the_portal_rejected_the_request
            RejectionContext.Lookup -> Res.string.person_not_found
            RejectionContext.Registration -> Res.string.the_portal_rejected_the_registration
            RejectionContext.CheckOut -> Res.string.the_portal_did_not_change_the_check_out_date
        },
    )
    PortalError.RegistrationUncertain -> UiText.of(Res.string.the_connection_was_lost_after_sending_the_portal)
    PortalError.InvalidResponse -> UiText.of(Res.string.the_portal_returned_an_unexpected_response)
    PortalError.SessionExpired -> UiText.of(Res.string.app_session_expired_please_sign_in_again)
    PortalError.InvalidCredentials -> UiText.of(Res.string.invalid_username_or_password)
    PortalError.AntiForgeryMissing -> UiText.of(Res.string.could_not_validate_the_portal_form_please_try)
    PortalError.CertificateNotBundled -> UiText.of(Res.string.the_portal_security_certificate_is_missing_from)
    is PortalError.Http -> UiText.of(Res.string.portal_returned_http_x, code)
    PortalError.Unreachable -> UiText.of(Res.string.could_not_connect_to_the_portal_check_your)
    PortalError.SecureConnection -> UiText.of(Res.string.the_https_connection_to_the_portal_ended_before)
}

fun SmsReporter.ValidationError.toUiText(): UiText = UiText.of(
    when (this) {
        SmsReporter.ValidationError.Passport -> Res.string.enter_a_passport_id_using_only_letters_and
        SmsReporter.ValidationError.CompanionIdentity -> Res.string.enter_the_companion_s_11_digit_identity_number
        SmsReporter.ValidationError.Days -> Res.string.enter_a_positive_whole_number_of_stay_days
        SmsReporter.ValidationError.ConfirmationCode -> Res.string.enter_only_the_received_confirmation_code
        SmsReporter.ValidationError.MessageTooLong -> Res.string.the_report_is_too_long_for_one_sms_check_the
    },
)

