package com.casero.cu

import com.casero.cu.feature.sms.SmsReportViewModel
import com.casero.cu.platform.MessageResult
import com.casero.cu.reporting.SmsReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SmsFormatTest {
    @Test
    fun foreignGuestUsesPrefixPassportAndStayDays() {
        assertEquals("HCTEST0001 5", SmsReporter.reportBody(" test0001 ", "05"))
    }

    @Test
    fun companionUsesMainPassportIdentityAndDays() {
        assertEquals("HCTEST0001 00000000000 5", SmsReporter.reportBody("TEST0001", "5", "00000000000"))
    }

    @Test
    fun confirmationUsesHCAndCode() {
        assertEquals("HC2568", SmsReporter.confirmationBody("2568"))
        assertFailsWith<SmsReporter.InvalidReport> { SmsReporter.confirmationBody("HC2568") }
        assertFailsWith<SmsReporter.InvalidReport> { SmsReporter.confirmationBody("") }
    }

    @Test
    fun invalidFieldsCannotProduceReport() {
        for (days in listOf("0", "-1", "1.5", "abc", "", "٥")) {
            assertFailsWith<SmsReporter.InvalidReport> { SmsReporter.reportBody("TEST0001", days) }
        }
        for (passport in listOf("", "TEST 0001", "TEST\n0001", "PÄSS", "TEST*0001")) {
            assertFailsWith<SmsReporter.InvalidReport> { SmsReporter.reportBody(passport, "5") }
        }
        assertFailsWith<SmsReporter.InvalidReport> { SmsReporter.reportBody("TEST0001", "5", "123") }
        assertFailsWith<SmsReporter.InvalidReport> { SmsReporter.reportBody("T".repeat(160), "5") }
    }
}

/**
 * The iOS suite also expected the model to switch to the confirmation step after
 * a sent request; the model no longer does (AGENTS.md, known issues), so those
 * expectations are not ported.
 */
class SmsFlowTest {
    @Test
    fun mainGuestMustBeRegisteredBeforeCompanion() {
        val model = SmsReportViewModel()
        model.kind = SmsReportViewModel.Kind.CubanCompanion
        model.passport = "TEST0001"
        model.days = "5"
        model.companionIdentity = "00000000000"
        assertNull(model.preview)
        assertNotNull(model.validationMessage)
        model.mainGuestIsRegistered = true
        assertEquals("HCTEST0001 00000000000 5", model.preview)
    }

    @Test
    fun cancellingKeepsInputsAndSaysNothingWasConfirmed() {
        val model = SmsReportViewModel()
        model.passport = "TEST0001"
        model.days = "5"
        model.prepare(SmsReportViewModel.Kind.Guest)
        model.finished(MessageResult.Cancelled, SmsReportViewModel.Kind.Guest)
        assertNull(model.preparedMessage)
        assertEquals("TEST0001", model.passport)
        assertNotNull(model.resultMessage)
    }

    @Test
    fun preparedMessageIsAnImmutableSnapshot() {
        val model = SmsReportViewModel()
        model.passport = "TEST0001"
        model.days = "1"
        model.prepare(SmsReportViewModel.Kind.Guest)
        val message = model.preparedMessage!!
        model.passport = "TEST0002"
        model.days = "7"
        assertEquals("HCTEST0001 1", message.body)
    }

    @Test
    fun sentConfirmationClearsTheCode() {
        val model = SmsReportViewModel()
        model.confirmationCode = "2568"
        model.prepare(SmsReportViewModel.Kind.Confirmation)
        model.finished(MessageResult.Sent, SmsReportViewModel.Kind.Confirmation)
        assertEquals("", model.confirmationCode)
    }
}
