package com.casero.cu.export

import com.casero.cu.resources.Res
import com.casero.cu.resources.attempted_from_caserocu_on_x_cuba_time_error_x
import com.casero.cu.resources.born_x
import com.casero.cu.resources.check_in
import com.casero.cu.resources.check_in_x_check_out_x
import com.casero.cu.resources.check_in_x_check_out_x_x_nights
import com.casero.cu.resources.check_out
import com.casero.cu.resources.guest_registration_not_completed
import com.casero.cu.resources.guest_registration_partly_registered
import com.casero.cu.resources.guest_registration_prepared
import com.casero.cu.resources.guest_registration_registered
import com.casero.cu.resources.guest_report_x_x
import com.casero.cu.resources.if_the_registration_cannot_be_completed_report
import com.casero.cu.resources.name
import com.casero.cu.resources.nationality
import com.casero.cu.resources.not_confirmed
import com.casero.cu.resources.not_registered
import com.casero.cu.resources.not_registered_x
import com.casero.cu.resources.passport_id
import com.casero.cu.resources.passport_id_x_x
import com.casero.cu.resources.people_marked_not_registered_were_not_accepted
import com.casero.cu.resources.prepared_in_caserocu_on_x_cuba_time_before
import com.casero.cu.resources.registered
import com.casero.cu.resources.sent_from_caserocu_to_the_official_portal_on_x
import com.casero.cu.resources.stay_id_x
import com.casero.cu.resources.the_answer_was_lost_after_sending_these_people
import com.casero.cu.resources.unconfirmed
import org.jetbrains.compose.resources.getString

object ResourceExportLabels : ExportLabels {
    override suspend fun name() = getString(Res.string.name)
    override suspend fun passportId() = getString(Res.string.passport_id)
    override suspend fun nationality() = getString(Res.string.nationality)
    override suspend fun checkIn() = getString(Res.string.check_in)
    override suspend fun checkOut() = getString(Res.string.check_out)
    override suspend fun guestReport(from: String, to: String) = getString(Res.string.guest_report_x_x, from, to)
    override suspend fun passportIdWithNationality(id: String, nationality: String) =
        getString(Res.string.passport_id_x_x, id, nationality)
    override suspend fun checkInCheckOut(checkIn: String, checkOut: String) =
        getString(Res.string.check_in_x_check_out_x, checkIn, checkOut)
    override suspend fun checkInCheckOutNights(checkIn: String, checkOut: String, nights: Int) =
        getString(Res.string.check_in_x_check_out_x_x_nights, checkIn, checkOut, nights)
    override suspend fun born(date: String) = getString(Res.string.born_x, date)
    override suspend fun stayId(id: String) = getString(Res.string.stay_id_x, id)
    override suspend fun inPersonFallback() = getString(Res.string.if_the_registration_cannot_be_completed_report)
    override suspend fun registrationPreparedTitle() = getString(Res.string.guest_registration_prepared)
    override suspend fun registrationPreparedNote(time: String) =
        getString(Res.string.prepared_in_caserocu_on_x_cuba_time_before, time)
    override suspend fun registrationRegisteredTitle() = getString(Res.string.guest_registration_registered)
    override suspend fun registrationPartlyTitle() = getString(Res.string.guest_registration_partly_registered)
    override suspend fun registrationSentNote(time: String) =
        getString(Res.string.sent_from_caserocu_to_the_official_portal_on_x, time)
    override suspend fun notRegisteredNote() = getString(Res.string.people_marked_not_registered_were_not_accepted)
    override suspend fun registered() = getString(Res.string.registered)
    override suspend fun notRegisteredReason(reason: String) = getString(Res.string.not_registered_x, reason)
    override suspend fun unconfirmed() = getString(Res.string.unconfirmed)
    override suspend fun registrationNotCompletedTitle() = getString(Res.string.guest_registration_not_completed)
    override suspend fun registrationAttemptedNote(time: String, reason: String) =
        getString(Res.string.attempted_from_caserocu_on_x_cuba_time_error_x, time, reason)
    override suspend fun answerLostNote() = getString(Res.string.the_answer_was_lost_after_sending_these_people)
    override suspend fun notConfirmed() = getString(Res.string.not_confirmed)
    override suspend fun notRegistered() = getString(Res.string.not_registered)
}
