package com.casero.cu.feature.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.casero.cu.model.CubanId
import com.casero.cu.model.Guest
import com.casero.cu.model.GuestRegistration
import com.casero.cu.model.PersonaValidation
import com.casero.cu.model.PortalDates
import com.casero.cu.model.PortalDates.plusDays
import com.casero.cu.network.PersonaLookup
import com.casero.cu.network.PortalError
import com.casero.cu.network.PortalServing
import com.casero.cu.resources.Res
import com.casero.cu.resources.add_the_main_guest_to_the_queue_before
import com.casero.cu.resources.carne_de_identidad
import com.casero.cu.resources.check_in_can_be_at_most_7_days_ago
import com.casero.cu.resources.ci_has_11_digits
import com.casero.cu.resources.check_out_must_be_on_or_after_check_in
import com.casero.cu.resources.first_name
import com.casero.cu.resources.id_card
import com.casero.cu.resources.if_the_person_has_a_second_given_name_try
import com.casero.cu.resources.name
import com.casero.cu.resources.not_found_if_the_person_has_a_second_given_name
import com.casero.cu.resources.passport
import com.casero.cu.resources.passport_number
import com.casero.cu.resources.person_not_found
import com.casero.cu.resources.personal_data_contains_unsupported_characters
import com.casero.cu.resources.some_registrations_were_not_confirmed_they
import com.casero.cu.resources.temp_id
import com.casero.cu.resources.temporary_carne_number
import com.casero.cu.resources.the_portal_returned_incomplete_personal_data_use
import com.casero.cu.resources.this_person_is_already_in_the_registration_queue
import com.casero.cu.resources.this_search_option_is_only_valid_for_cuban
import com.casero.cu.resources.this_search_option_is_only_valid_for_foreigners
import com.casero.cu.resources.this_search_option_is_only_valid_for_foreigners_2
import com.casero.cu.ui.UiText
import com.casero.cu.ui.toUiText
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

class RegisterGuestViewModel(
    private val client: PortalServing,
    private val now: () -> Instant = PortalDates::now,
    /** Shows the expired-session banner at once, even if the Register tab is not on screen. */
    private val onSessionExpired: () -> Unit = {},
) {

    /** Order matches the segmented selector: passport, CI, names, temporary CI. */
    enum class LookupKind(val segmentLabel: StringResource, val fieldLabel: StringResource, val help: StringResource) {
        ForeignGuest(Res.string.passport, Res.string.passport_number, Res.string.this_search_option_is_only_valid_for_foreigners),
        CubanCompanion(Res.string.id_card, Res.string.carne_de_identidad, Res.string.this_search_option_is_only_valid_for_cuban),
        ForeignGuestByName(Res.string.name, Res.string.first_name, Res.string.this_search_option_is_only_valid_for_foreigners),
        TemporaryCarne(Res.string.temp_id, Res.string.temporary_carne_number, Res.string.this_search_option_is_only_valid_for_foreigners_2),
    }

    var lookupKind by mutableStateOf(LookupKind.ForeignGuest)
    var identificador by mutableStateOf("")

    /** Name lookup fields, mirroring the portal form: first name, first and second surname. */
    var firstName by mutableStateOf("")
    var lastName1 by mutableStateOf("")
    var lastName2 by mutableStateOf("")
    var checkIn by mutableStateOf(PortalDates.today(now()))
    var checkOut by mutableStateOf(PortalDates.today(now()).plusDays(1))

    /** The person being reviewed in the result sheet (dates + Add to registration). */
    var foundPerson by mutableStateOf<PersonaValidation?>(null)
        private set

    /** Every lookup result. Several are listed in the sheet to pick from first. */
    var candidates by mutableStateOf<List<PersonaValidation>>(emptyList())
        private set

    /** How the current results were found (the selector can change afterwards). */
    private var resultKind: LookupKind? = null

    /** The result sheet is open while there is something to pick or review. */
    val isShowingResult: Boolean get() = foundPerson != null || candidates.isNotEmpty()

    val queue = mutableStateListOf<GuestRegistration>()
    var isLookingUp by mutableStateOf(false)
        private set
    var isSubmitting by mutableStateOf(false)
        private set

    /** Shown under the Look up button. */
    var lookupErrorMessage by mutableStateOf<UiText?>(null)
        private set

    /** Registration errors, shown with the queue. */
    var errorMessage by mutableStateOf<UiText?>(null)
        private set
    var lastSubmission by mutableStateOf<List<Guest>?>(null)
        private set

    /** Queued people whose registration was sent but never confirmed (answer lost). */
    var uncertainIDs by mutableStateOf<Set<String>>(emptySet())
        private set
    var isSessionExpired by mutableStateOf(false)
        private set

    /**
     * What is sent as `identificador`. Name lookups are joined with underscores,
     * as captured from the portal: `NOMBRE_APELLIDO1_APELLIDO2`. An empty second
     * surname keeps the trailing underscore (`NOMBRE_APELLIDO1_`), which the portal
     * also matches.
     */
    val lookupQuery: String
        get() = when (lookupKind) {
            LookupKind.ForeignGuestByName -> listOf(firstName, lastName1, lastName2).joinToString("_") { it.trim().uppercase() }
            else -> identificador.trim()
        }

    val canLookUp: Boolean
        get() {
            val hasQuery = when (lookupKind) {
                LookupKind.ForeignGuestByName -> firstName.isNotBlank() && lastName1.isNotBlank()
                LookupKind.CubanCompanion -> CubanId.isValid(lookupQuery)
                else -> lookupQuery.isNotEmpty()
            }
            return hasQuery && !isLookingUp && !isSubmitting
        }

    /** Shown under a CI that is typed but not yet 11 digits. */
    val identityHint: UiText?
        get() = if (lookupKind == LookupKind.CubanCompanion && lookupQuery.isNotEmpty() && !CubanId.isValid(lookupQuery)) {
            UiText.of(Res.string.ci_has_11_digits)
        } else {
            null
        }

    val earliestCheckIn: LocalDate get() = earliestCheckIn(now())

    val validationMessage: UiText?
        get() {
            val person = foundPerson ?: return null
            val response = person.response
            if (!response.isNullOrEmpty() && response != "OK") return UiText.Raw(response)
            val fields = listOf(person.firstName, person.identificador, person.nationalityCode ?: "", person.sex ?: "")
            if (fields.any { it.isBlank() } || person.birthDate == null) {
                return UiText.of(Res.string.the_portal_returned_incomplete_personal_data_use)
            }
            val wireFields = fields + listOf(person.lastName1 ?: "", person.lastName2 ?: "", person.middleName ?: "")
            if (wireFields.any { field -> field.any { it in ",|*\n\r" } }) {
                return UiText.of(Res.string.personal_data_contains_unsupported_characters)
            }
            if (checkOut < checkIn) return UiText.of(Res.string.check_out_must_be_on_or_after_check_in)
            if (checkIn < earliestCheckIn) return UiText.of(Res.string.check_in_can_be_at_most_7_days_ago)
            if (queue.any { it.identificador.equals(person.identificador, ignoreCase = true) }) {
                return UiText.of(Res.string.this_person_is_already_in_the_registration_queue)
            }
            return null
        }

    val canAdd: Boolean get() = foundPerson != null && validationMessage == null && !isSubmitting

    /**
     * Captured traffic only shows companions registered in the same call as a
     * main guest (the companion's `EstanciaId` is `<guest stay>.1`). A queue of
     * companions alone gets a warning but is still sent: the portal decides and
     * its answer is shown. Companions are people found by CI, not by nationality.
     */
    val companionWarning: UiText?
        get() = if (queue.isNotEmpty() && queue.all { it.isCompanion }) {
            UiText.of(Res.string.add_the_main_guest_to_the_queue_before)
        } else {
            null
        }

    val canSubmit: Boolean get() = queue.isNotEmpty() && !isSubmitting

    suspend fun lookUp() {
        if (!canLookUp) return
        lookupErrorMessage = null
        foundPerson = null
        candidates = emptyList()
        isLookingUp = true
        val query = lookupQuery
        val kind = lookupKind
        try {
            val people = when (kind) {
                LookupKind.ForeignGuest -> client.validatePersonaDiie(query, PersonaLookup.ByPassport)
                LookupKind.ForeignGuestByName -> client.validatePersonaDiie(query, PersonaLookup.ByName)
                LookupKind.TemporaryCarne -> client.validatePersonaDiie(query, PersonaLookup.ByTemporaryCarne)
                LookupKind.CubanCompanion -> client.validatePersonaSuin(query)
            }
            if (kind != lookupKind || query != lookupQuery) return
            // Results open in a sheet. A single match goes straight to its card;
            // several are listed first so the host picks the right person.
            if (people.isEmpty()) {
                lookupErrorMessage = UiText.of(
                    if (kind == LookupKind.ForeignGuestByName) {
                        Res.string.not_found_if_the_person_has_a_second_given_name
                    } else {
                        Res.string.person_not_found
                    },
                )
            } else {
                resultKind = kind
                candidates = people
                if (people.size == 1) foundPerson = people[0]
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: PortalError.SessionExpired) {
            isSessionExpired = true
            onSessionExpired()
        } catch (error: PortalError.Rejected) {
            // The portal matches the first given name only; suggest dropping the second.
            lookupErrorMessage = if (kind == LookupKind.ForeignGuestByName) {
                UiText.Joined(listOf(error.toUiText(), UiText.of(Res.string.if_the_person_has_a_second_given_name_try)))
            } else {
                error.toUiText()
            }
        } catch (error: Exception) {
            lookupErrorMessage = error.toUiText()
        } finally {
            isLookingUp = false
        }
    }

    /** Queues the currently found person with the selected stay dates, then clears the lookup. */
    fun addToQueue() {
        val person = foundPerson ?: return
        val birthDate = person.birthDate ?: return
        if (!canAdd) return
        queue.add(
            GuestRegistration(
                firstName = person.firstName,
                middleName = person.middleName ?: "",
                lastName1 = person.lastName1 ?: "",
                lastName2 = person.lastName2 ?: "",
                identificador = person.identificador,
                nationalityCode = person.nationalityCode ?: "",
                sex = person.sex ?: "",
                birthDate = birthDate,
                checkIn = PortalDates.startOfDay(checkIn),
                checkOut = PortalDates.startOfDay(checkOut),
                procedenciaCode = person.nationalityCode ?: "",
                nationalityName = person.nationality,
                isCompanion = resultKind == LookupKind.CubanCompanion,
            ),
        )
        foundPerson = null
        candidates = emptyList()
        identificador = ""
        firstName = ""
        lastName1 = ""
        lastName2 = ""
    }

    fun removeFromQueue(entry: GuestRegistration) {
        if (isSubmitting) return
        uncertainIDs = uncertainIDs - entry.identificador.uppercase()
        queue.remove(entry)
    }

    /** Returns `true` when the portal accepted at least one person. */
    suspend fun submit(): Boolean {
        if (!canSubmit) return false
        isSubmitting = true
        errorMessage = null
        lastSubmission = null
        try {
            val result = client.registerGuests(queue.toList())
            lastSubmission = result
            val accepted = result
                .filter { it.response == "OK" && it.identificador.isNotEmpty() }
                .map { it.identificador.uppercase() }
                .toSet()
            queue.removeAll { it.identificador.uppercase() in accepted }
            uncertainIDs = uncertainIDs - accepted
            if (queue.isNotEmpty()) errorMessage = UiText.of(Res.string.some_registrations_were_not_confirmed_they)
            return accepted.isNotEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (_: PortalError.SessionExpired) {
            isSessionExpired = true
            onSessionExpired()
        } catch (error: PortalError.RegistrationUncertain) {
            uncertainIDs = uncertainIDs + queue.map { it.identificador.uppercase() }
            errorMessage = error.toUiText()
        } catch (error: Exception) {
            errorMessage = error.toUiText()
        } finally {
            isSubmitting = false
        }
        return false
    }

    fun isUncertain(entry: GuestRegistration): Boolean = entry.identificador.uppercase() in uncertainIDs

    /** Closes the result sheet without adding anyone. */
    fun dismissResult() {
        foundPerson = null
        candidates = emptyList()
    }

    fun select(person: PersonaValidation) {
        foundPerson = person
    }

    fun clearLookup() {
        foundPerson = null
        candidates = emptyList()
        lookupErrorMessage = null
    }

    fun acknowledgeSessionExpired() {
        isSessionExpired = false
    }

    companion object {
        /** The portal accepts check-ins up to 7 days back (today the 21st → the 14th), counted in Cuba time. */
        const val MAX_CHECK_IN_DAYS_BACK = 7

        fun earliestCheckIn(now: Instant = PortalDates.now()): LocalDate =
            PortalDates.today(now).plusDays(-MAX_CHECK_IN_DAYS_BACK)
    }
}
