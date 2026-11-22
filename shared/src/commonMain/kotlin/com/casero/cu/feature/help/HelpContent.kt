// Generated from the iOS help screens (PortalHelpView, CubatramiteGuideView,
// AppUserManualView) by a one-off script. Edit by hand from now on.
package com.casero.cu.feature.help

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.casero.cu.resources.*

val portalHelpTopics: List<HelpSection> = listOf(
    HelpSection(
        id = "home",
        title = Res.string.help_home,
        icon = Icons.Default.Home,
        color = Color(0.36f, 0.42f, 0.5f),
        path = "",
        image = null,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.help_this_section_shows_every_operation_available_in),
            HelpBlock.Heading(Res.string.help_top_menu_bar),
            HelpBlock.Terms(listOf(Res.string.help_home to Res.string.help_shows_the_main_screen_with_every_operation, Res.string.help_guest to Res.string.help_shows_a_menu_with_the_register_guest_active, Res.string.help_communication_channels to Res.string.help_configures_the_alternative_communication, Res.string.help_my_profile to Res.string.help_shows_the_signed_in_user_s_details_and_lets_them, Res.string.help_help to Res.string.help_shows_the_help, Res.string.help_about to Res.string.help_shows_information_about_the_application, Res.string.help_exit to Res.string.help_closes_the_application)),
            HelpBlock.Heading(Res.string.help_work_area),
            HelpBlock.Terms(listOf(Res.string.help_register_guest to Res.string.help_registers_one_or_more_guests_with_their, Res.string.help_active_guests to Res.string.help_lists_the_guests_who_are_currently_registered, Res.string.help_registered_guests to Res.string.help_keeps_a_history_of_every_guest_the_host_has)),
        ),
    ),
    HelpSection(
        id = "profile",
        title = Res.string.help_my_profile,
        icon = Icons.Default.AccountCircle,
        color = Color(0.42f, 0.36f, 0.75f),
        path = "Cuenta/Perfil",
        image = null,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.help_this_section_changes_the_signed_in_user_s),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_in_the_clave_anterior_previous_password_field, listOf()),
                HelpStep(Res.string.help_in_the_nueva_clave_new_password_and_confirmar_la, listOf()),
                HelpStep(Res.string.help_tap_the_cambiar_change_button_to_change_the, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.help_the_password_must_have_at_least_one_uppercase)),
        ),
    ),
    HelpSection(
        id = "register",
        title = Res.string.help_register_guest,
        icon = Icons.Default.PersonAdd,
        color = Color(1.0f, 0.55f, 0.16f),
        path = "Huespedes",
        image = Res.drawable.portalregisterguest,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.help_this_section_registers_one_or_more_guests_follow),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_from_the_drop_down_list_select_pasaporte, listOf()),
                HelpStep(Res.string.help_when_the_search_finishes_the_guest_s_information, listOf()),
                HelpStep(Res.string.help_the_detalles_del_huesped_guest_details_window_2, listOf()),
                HelpStep(Res.string.help_to_edit_a_guest_s_details_select_the_guest_and, listOf(Res.string.help_the_detalles_del_huesped_guest_details_window)),
                HelpStep(Res.string.help_to_remove_a_guest_tap_the_delete_icon, listOf()),
                HelpStep(Res.string.help_tap_the_registrar_register_button_to_finish_the, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.help_to_register_several_guests_repeat_steps_1_2_and, Res.string.help_the_first_guest_added_must_be_a_foreigner_or_a, Res.string.help_cuban_nationals_added_after_a_guest_are_treated, Res.string.help_the_numero_de_identidad_identity_number_field_is)),
        ),
    ),
    HelpSection(
        id = "active",
        title = Res.string.help_active_guests,
        icon = Icons.Default.HowToReg,
        color = Color(1.0f, 0.34f, 0.16f),
        path = "Huespedes/HuespedesActivos",
        image = Res.drawable.portalactiveguests,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.help_this_section_lists_the_guests_who_are_currently),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_first_select_a_guest, listOf()),
                HelpStep(Res.string.help_the_guest_s_area_turns_blue_and_two_icons_appear, listOf()),
            )),
            HelpBlock.Paragraph(Res.string.help_what_to_do_for_each_option_is_described_below),
            HelpBlock.Heading(Res.string.help_to_add_companions_to_a_guest),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_tap_the_add_companions_icon_a_new_window_opens, listOf()),
                HelpStep(Res.string.help_to_see_the_selected_guest_s_companions_tap_the, listOf()),
                HelpStep(Res.string.help_to_add_a_new_companion_to_the_guest_tap_the, listOf()),
                HelpStep(Res.string.help_type_the_companion_s_identity_card_number_in_the, listOf()),
                HelpStep(Res.string.help_when_the_companion_s_details_are_returned_tap, listOf()),
                HelpStep(Res.string.help_the_detalles_del_acompanante_companion_details_2, listOf()),
                HelpStep(Res.string.help_to_edit_a_companion_s_details_select_them_and, listOf(Res.string.help_the_detalles_del_acompanante_companion_details)),
                HelpStep(Res.string.help_to_remove_a_companion_tap_the_delete_icon, listOf()),
                HelpStep(Res.string.help_tap_the_registrar_register_button_to_finish, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.help_to_register_several_companions_repeat_the_steps, Res.string.help_to_go_back_to_the_active_guests_list_at_any)),
            HelpBlock.Heading(Res.string.help_to_change_a_guest_s_check_out_date),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_with_the_guest_selected_tap_the_change_check_out, listOf()),
                HelpStep(Res.string.help_in_the_window_that_opens_select_the_fecha_salida, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.help_use_mostrar_show_to_choose_how_many_guests)),
        ),
    ),
    HelpSection(
        id = "registered",
        title = Res.string.help_registered_guests,
        icon = Icons.Default.Person,
        color = Color(0.06f, 0.6f, 0.28f),
        path = "Huespedes/HuespedesRegistrados",
        image = Res.drawable.portalregisteredguests,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.help_this_section_lists_the_registered_guests_follow),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_optionally_select_a_desde_from_hasta_to_date, listOf(Res.string.help_from_every_guest_registered_from_that_date, Res.string.help_to_every_guest_registered_up_to_that_date)),
                HelpStep(Res.string.help_tap_the_buscar_search_button, listOf()),
                HelpStep(Res.string.help_when_the_search_finishes_the_list_of_registered, listOf()),
            )),
            HelpBlock.Heading(Res.string.help_to_show_a_guest_s_companions),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_first_select_a_guest, listOf()),
                HelpStep(Res.string.help_the_guest_s_area_turns_blue_and_the_companions, listOf()),
                HelpStep(Res.string.help_tap_the_companions_icon_a_new_window_shows_the, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.help_to_register_several_companions_repeat_the_steps, Res.string.help_to_go_back_to_the_active_guests_list_at_any, Res.string.help_use_mostrar_show_to_choose_how_many_guests)),
        ),
    ),
    HelpSection(
        id = "channels",
        title = Res.string.help_communication_channels,
        icon = Icons.Default.Email,
        color = Color(0.24f, 0.5f, 0.76f),
        path = "ViasComunicacion",
        image = Res.drawable.portalchannels,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.help_this_section_configures_the_alternative),
            HelpBlock.Heading(Res.string.help_to_add_new_channels),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_add_an_email_type_the_email_address_in_the, listOf()),
                HelpStep(Res.string.help_add_a_mobile_phone_type_the_mobile_number_in_the, listOf()),
                HelpStep(Res.string.help_tap_the_confirmar_confirm_button_to_finish_the, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.help_to_add_several_channels_repeat_steps_1_and_or_2)),
            HelpBlock.Heading(Res.string.help_to_remove_existing_channels),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.help_select_the_channel_you_want_to_remove, listOf()),
                HelpStep(Res.string.help_the_channel_s_area_turns_blue_and_the_delete, listOf()),
                HelpStep(Res.string.help_tap_the_delete_icon_and_the_channel_is_removed, listOf()),
                HelpStep(Res.string.help_tap_the_confirmar_confirm_button_to_finish_the, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.help_to_remove_several_channels_repeat_steps_1_2_and)),
        ),
    ),
)

val cubatramiteGuideSections: List<HelpSection> = listOf(
    HelpSection(
        id = "who",
        title = Res.string.guide_who_must_report,
        icon = Icons.Default.Group,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.guide_non_state_economic_actors_authorized_to_rent_out),
            HelpBlock.Bullets(listOf(Res.string.guide_private_companies_and_msmes_mipymes, Res.string.guide_non_agricultural_cooperatives, Res.string.guide_self_employed_workers_trabajadores_por_cuenta)),
            HelpBlock.Paragraph(Res.string.guide_it_covers_foreign_tourists_and_also_cubans_who),
            HelpBlock.Notes(listOf(Res.string.guide_self_employed_hosts_must_also_keep_a_register_of, Res.string.guide_under_decree_137_of_2025_accommodations_covered)),
        ),
    ),
    HelpSection(
        id = "how",
        title = Res.string.guide_how_to_report,
        icon = Icons.Default.FormatListNumbered,
        blocks = listOf(
            HelpBlock.Heading(Res.string.guide_step_1_record_the_guest_s_details),
            HelpBlock.Bullets(listOf(Res.string.guide_name_surnames_citizenship_and_identity_or, Res.string.guide_the_same_details_for_each_companion_if_any, Res.string.guide_check_in_and_check_out_dates)),
            HelpBlock.Paragraph(Res.string.guide_self_employed_hosts_record_this_in_the_register),
            HelpBlock.Heading(Res.string.guide_step_2_report_to_minint),
            HelpBlock.Paragraph(Res.string.guide_there_are_two_remote_channels),
            HelpBlock.Terms(listOf(Res.string.guide_portal_caseros to Res.string.guide_sign_in_at_https_casero_rem_cu_with_your_host, Res.string.guide_sms to Res.string.guide_send_the_report_to_7344_from_the_mobile_number)),
            HelpBlock.Heading(Res.string.guide_step_3_report_the_departure),
            HelpBlock.Paragraph(Res.string.guide_article_69_of_law_173_of_2024_immigration),
        ),
    ),
    HelpSection(
        id = "sms",
        title = Res.string.guide_reporting_by_sms,
        icon = Icons.AutoMirrored.Filled.Message,
        blocks = listOf(
            HelpBlock.Heading(Res.string.guide_before_the_first_report),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.guide_call_18808_from_your_mobile_to_ask_for_the, listOf()),
                HelpStep(Res.string.guide_wait_for_the_confirmation_the_association_is_not, listOf()),
                HelpStep(Res.string.guide_once_enabled_use_that_number_for_sms_reports, listOf()),
            )),
            HelpBlock.Heading(Res.string.guide_foreign_guest),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.guide_send_hc_passport_number_stay_days_to_7344, listOf()),
                HelpStep(Res.string.guide_the_reply_contains_the_name_citizenship_and_a, listOf()),
                HelpStep(Res.string.guide_send_the_code_back_preceded_by_hc, listOf()),
                HelpStep(Res.string.guide_the_system_then_sends_a_registration_code_which, listOf()),
            )),
            HelpBlock.Example("HCJ1459863 5\nHC2568"),
            HelpBlock.Heading(Res.string.guide_companion_living_in_cuba),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.guide_confirm_the_main_guest_s_registration_first, listOf()),
                HelpStep(Res.string.guide_send_hc_guest_passport_companion_identity_card, listOf()),
                HelpStep(Res.string.guide_send_the_confirmation_code_back_preceded_by_hc, listOf()),
                HelpStep(Res.string.guide_wait_for_the_registration_code, listOf()),
            )),
            HelpBlock.Example("HCJ1459863 89092120806 5"),
            HelpBlock.Notes(listOf(Res.string.guide_each_person_is_a_separate_report_sent_one_at_a, Res.string.guide_register_the_main_guest_before_their_companions, Res.string.guide_hc_always_goes_before_the_data, Res.string.guide_confirmation_requests_expire_after_six_hours, Res.string.guide_if_the_sms_does_not_follow_the_format_there_may, Res.string.guide_the_sms_procedure_was_designed_for_foreign)),
        ),
    ),
    HelpSection(
        id = "costs",
        title = Res.string.guide_cost_hours_and_deadlines,
        icon = Icons.Default.Schedule,
        blocks = listOf(
            HelpBlock.Terms(listOf(Res.string.guide_cost to Res.string.guide_there_is_no_administrative_fee_the_portal_needs, Res.string.guide_hours to Res.string.guide_report_at_any_time_of_day_through_the_portal_or, Res.string.guide_deadline to Res.string.guide_article_38_1_c_of_decree_137_of_2025_requires, Res.string.guide_confirmation to Res.string.guide_the_portal_registers_the_report_immediately_the)),
        ),
    ),
    HelpSection(
        id = "faq",
        title = Res.string.guide_frequently_asked_questions,
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        blocks = listOf(
            HelpBlock.Heading(Res.string.guide_must_cubans_living_abroad_be_reported),
            HelpBlock.Paragraph(Res.string.guide_yes_the_rule_covers_anyone_who_lives_abroad_plus),
            HelpBlock.Heading(Res.string.guide_does_reporting_a_guest_authorize_their_stay),
            HelpBlock.Paragraph(Res.string.guide_no_the_report_communicates_their_presence_it),
            HelpBlock.Heading(Res.string.guide_can_a_tourist_stay_at_a_friend_s_home),
            HelpBlock.Paragraph(Res.string.guide_not_if_the_home_is_not_authorized_for_it),
            HelpBlock.Heading(Res.string.guide_must_a_stay_shorter_than_one_day_be_reported),
            HelpBlock.Paragraph(Res.string.guide_yes_both_the_check_in_and_the_check_out_must_be),
            HelpBlock.Heading(Res.string.guide_can_i_host_foreigners_coming_for_religious),
            HelpBlock.Paragraph(Res.string.guide_a_tourist_visa_does_not_allow_religious),
            HelpBlock.Heading(Res.string.guide_what_other_obligations_do_self_employed_hosts),
            HelpBlock.Paragraph(Res.string.guide_be_responsible_for_what_happens_in_the_property),
        ),
    ),
    HelpSection(
        id = "penalties",
        title = Res.string.guide_penalties,
        icon = Icons.Default.Warning,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.guide_decree_law_91_of_2024_sets_three_levels_of),
            HelpBlock.Heading(Res.string.guide_less_serious),
            HelpBlock.Paragraph(Res.string.guide_not_providing_required_information_or_not),
            HelpBlock.Bullets(listOf(Res.string.guide_self_employed_20_to_40_units, Res.string.guide_msmes_and_cooperatives_40_to_60_units, Res.string.guide_each_unit_100_to_300_cup)),
            HelpBlock.Heading(Res.string.guide_serious),
            HelpBlock.Paragraph(Res.string.guide_working_without_the_required_licenses_using),
            HelpBlock.Bullets(listOf(Res.string.guide_self_employed_40_to_60_units, Res.string.guide_msmes_and_cooperatives_60_to_100_units, Res.string.guide_each_unit_400_to_600_cup)),
            HelpBlock.Heading(Res.string.guide_very_serious),
            HelpBlock.Paragraph(Res.string.guide_renting_to_people_who_live_abroad_without),
            HelpBlock.Bullets(listOf(Res.string.guide_self_employed_60_to_100_units, Res.string.guide_msmes_and_cooperatives_100_to_200_units, Res.string.guide_each_unit_700_to_1_000_cup)),
            HelpBlock.Notes(listOf(Res.string.guide_the_fine_depends_on_the_circumstances_of_each)),
        ),
    ),
    HelpSection(
        id = "contact",
        title = Res.string.guide_contact_and_legal_basis,
        icon = Icons.Default.Phone,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.guide_for_questions_about_dimec_minint_services),
            HelpBlock.Terms(listOf(Res.string.guide_phone to Res.string.guide_18808, Res.string.guide_email to Res.string.guide_diieminintcuba_rem_cu)),
            HelpBlock.Heading(Res.string.guide_legal_basis),
            HelpBlock.Bullets(listOf(Res.string.guide_decree_law_171_of_1997_rental_of_homes_rooms_or, Res.string.guide_decree_law_90_of_2024_self_employment_article_27, Res.string.guide_decree_law_91_of_2024_contraventions_and_fines, Res.string.guide_decree_160_of_2026_reporting_obligation_for_non, Res.string.guide_decree_law_133_of_2026_amends_article_27_of)),
        ),
    ),
)

val userManualSections: List<HelpSection> = listOf(
    HelpSection(
        id = "start",
        title = Res.string.manual_getting_started,
        icon = Icons.Default.Flag,
        blocks = listOf(
            HelpBlock.Terms(listOf(Res.string.manual_sms_reports to Res.string.manual_work_without_an_account_or_internet_you_need, Res.string.manual_portal_features to Res.string.manual_register_guests_and_your_account_need_your)),
            HelpBlock.Heading(Res.string.manual_sign_in),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.manual_tap_the_yellow_sign_in_to_the_app_session_banner, listOf()),
                HelpStep(Res.string.manual_enter_your_portal_username_and_password, listOf()),
                HelpStep(Res.string.manual_keep_remember_credentials_on_to_save_them_in, listOf()),
            )),
            HelpBlock.Heading(Res.string.manual_banners_at_the_top),
            HelpBlock.Bullets(listOf(Res.string.manual_yellow_you_are_not_signed_in, Res.string.manual_orange_the_session_expired_sign_in_again, Res.string.manual_red_no_internet_connection_saved_guests_are)),
        ),
    ),
    HelpSection(
        id = "sms",
        title = Res.string.manual_sms_reports,
        icon = Icons.AutoMirrored.Filled.Message,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.manual_report_a_guest_by_sms_to_7344_each_person_is_a),
            HelpBlock.Heading(Res.string.manual_report_a_foreign_guest),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.manual_choose_guest, listOf()),
                HelpStep(Res.string.manual_enter_the_passport_and_the_stay_days, listOf()),
                HelpStep(Res.string.manual_tap_preview_sms_messages_opens_with_the_prepared, listOf()),
            )),
            HelpBlock.Heading(Res.string.manual_report_a_cuban_companion),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.manual_register_the_main_guest_first_and_wait_for_its, listOf()),
                HelpStep(Res.string.manual_choose_companion_enter_the_main_guest_s_passport, listOf()),
                HelpStep(Res.string.manual_turn_on_the_main_guest_s_registration_is, listOf()),
            )),
            HelpBlock.Heading(Res.string.manual_confirm_the_received_code),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.manual_the_reply_brings_the_person_s_name_nationality, listOf()),
                HelpStep(Res.string.manual_under_confirm_received_code_enter_the_code, listOf()),
                HelpStep(Res.string.manual_wait_for_the_final_registration_code_that_is, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.manual_confirmation_requests_expire_after_six_hours, Res.string.manual_the_app_never_sends_anything_by_itself_and_does, Res.string.manual_the_i_button_explains_the_procedure_and_opens)),
        ),
    ),
    HelpSection(
        id = "register",
        title = Res.string.manual_register_guests,
        icon = Icons.Default.PersonAdd,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.manual_registers_guests_and_companions_in_the_official),
            HelpBlock.Steps(listOf(
                HelpStep(Res.string.manual_choose_how_to_look_up_passport_id_card_name_or, listOf()),
                HelpStep(Res.string.manual_pick_the_right_person_set_the_check_in_up_to_7, listOf()),
                HelpStep(Res.string.manual_repeat_for_each_person_you_can_remove_someone, listOf()),
                HelpStep(Res.string.manual_tap_register_people_review_the_summary_and, listOf()),
                HelpStep(Res.string.manual_the_result_shows_each_person_s_stay_id_which_you, listOf()),
            )),
            HelpBlock.Notes(listOf(Res.string.manual_add_the_main_guest_a_foreigner_or_a_cuban_living, Res.string.manual_you_can_save_a_pdf_before_sending_after_sending, Res.string.manual_if_the_connection_drops_after_sending_the_people, Res.string.manual_if_the_portal_refuses_it_gives_no_reason_try_sms)),
        ),
    ),
    HelpSection(
        id = "guests",
        title = Res.string.manual_guests,
        icon = Icons.Default.Group,
        blocks = listOf(
            HelpBlock.Paragraph(Res.string.manual_your_guests_from_the_official_portal_needs_sign),
            HelpBlock.Terms(listOf(Res.string.manual_active to Res.string.manual_guests_currently_staying_grouped_by_departure, Res.string.manual_history to Res.string.manual_every_registered_guest_grouped_by_check_in_month)),
            HelpBlock.Bullets(listOf(Res.string.manual_search_by_name_or_passport, Res.string.manual_change_the_date_range_with_the_calendar_button, Res.string.manual_reload_with_the_arrow_button, Res.string.manual_drag_the_side_bar_to_jump_between_groups)),
            HelpBlock.Heading(Res.string.manual_guest_details),
            HelpBlock.Bullets(listOf(Res.string.manual_see_the_stay_the_photo_and_the_companions, Res.string.manual_add_companion_active_guests_only_look_up_the, Res.string.manual_update_check_out_active_guests_and_companions)),
        ),
    ),
    HelpSection(
        id = "settings",
        title = Res.string.manual_settings,
        icon = Icons.Default.Settings,
        blocks = listOf(
            HelpBlock.Terms(listOf(Res.string.manual_portal_profile_account to Res.string.manual_your_name_id_card_username_and_address_from_the, Res.string.manual_communication_channels to Res.string.manual_your_registered_phones_and_emails_add_or_remove, Res.string.manual_remember_credentials to Res.string.manual_saves_your_password_in_this_device_s_keychain, Res.string.manual_auto_re_authenticate_on_expiry to Res.string.manual_signs_in_again_by_itself_when_the_app_opens_or, Res.string.manual_cache to Res.string.manual_how_long_guests_are_kept_on_the_device_1_hour_to, Res.string.manual_guests to Res.string.manual_the_default_date_range_and_export_guest_data)),
            HelpBlock.Notes(listOf(Res.string.manual_signing_out_deletes_the_saved_password_the)),
        ),
    ),
    HelpSection(
        id = "help",
        title = Res.string.manual_help_and_references,
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        blocks = listOf(
            HelpBlock.Bullets(listOf(Res.string.manual_the_i_button_on_sms_register_and_guests_explains, Res.string.manual_official_portal_help_the_portal_s_own, Res.string.manual_cubatramite_source_an_offline_summary_of_the, Res.string.manual_privacy_policy_terms_on_the_casero_website)),
        ),
    ),
    HelpSection(
        id = "troubleshooting",
        title = Res.string.manual_troubleshooting,
        icon = Icons.Default.Build,
        blocks = listOf(
            HelpBlock.Terms(listOf(Res.string.manual_the_portal_does_not_load to Res.string.manual_it_only_works_from_inside_cuba_use_sms_reports, Res.string.manual_the_session_keeps_expiring to Res.string.manual_sign_in_again_or_turn_on_auto_re_authenticate_on, Res.string.manual_no_sms_reply to Res.string.manual_check_that_you_sent_it_from_the_associated, Res.string.manual_this_device_cannot_send_sms to Res.string.manual_use_the_preview_on_your_registered_phone_or_type)),
        ),
    ),
)
