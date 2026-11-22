package com.casero.cu

import com.casero.cu.feature.guests.GuestsViewModel
import com.casero.cu.feature.register.RegisterGuestViewModel
import com.casero.cu.model.DotNetDate
import com.casero.cu.model.Guest
import com.casero.cu.model.GuestListResponse
import com.casero.cu.model.GuestRegistration
import com.casero.cu.model.PortalDates
import com.casero.cu.model.PortalJson
import com.casero.cu.model.PortalProfile
import com.casero.cu.network.AntiForgery
import com.casero.cu.network.PortalError
import com.casero.cu.reporting.SmsReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ModelTest {
    /**
     * Mirrors a browser capture of `RegistrarHuesped` (synthetic names/IDs, real
     * shape): four `*`-joined name parts, `**` when there is no second name.
     */
    @Test
    fun registrationWireFormatMatchesPortal() {
        val checkIn = ms(1_794_978_000_000)
        val checkOut = ms(1_795_237_200_000)
        val guest = GuestRegistration(
            firstName = "TESTA", lastName1 = "UNO", lastName2 = "DOS", identificador = "X0000001",
            nationalityCode = "201", sex = "M", birthDate = ms(122_533_200_000),
            checkIn = checkIn, checkOut = checkOut, procedenciaCode = "201",
        )
        val companion = GuestRegistration(
            firstName = "TESTB", middleName = "MEDIO", lastName1 = "TRES", lastName2 = "CUATRO",
            identificador = "00000000001", nationalityCode = "247", sex = "F", birthDate = ms(481_611_600_000),
            checkIn = checkIn, checkOut = checkOut, procedenciaCode = "247",
        )
        assertEquals(
            "TESTA**UNO*DOS,X0000001,201,M,19/11/73,18/11/26,21/11/26,201|" +
                "TESTB*MEDIO*TRES*CUATRO,00000000001,247,F,06/04/85,18/11/26,21/11/26,247",
            listOf(guest, companion).joinToString("|") { it.wireFormat },
        )
    }

    @Test
    fun checkInGoesBackAtMostSevenDays() {
        val earliest = RegisterGuestViewModel.earliestCheckIn(cubaAfternoon)
        assertEquals(14, earliest.day)
        assertEquals(11, earliest.month.ordinal + 1)
    }

    @Test
    fun daysUntilCheckOutCountsCubaCalendarDays() {
        fun guest(checkOut: Instant) = Guest(fullName = "Test", identificador = "X1", checkOut = checkOut, stayId = "1")
        val todayMidnight = PortalDates.startOfDay(cubaAfternoon)
        val day = 86_400_000L
        assertEquals(0, GuestsViewModel.daysUntilCheckOut(guest(todayMidnight), cubaAfternoon))
        assertEquals(1, GuestsViewModel.daysUntilCheckOut(guest(ms(todayMidnight.toEpochMilliseconds() + day)), cubaAfternoon))
        assertEquals(3, GuestsViewModel.daysUntilCheckOut(guest(ms(todayMidnight.toEpochMilliseconds() + 3 * day)), cubaAfternoon))
    }

    @Test
    fun portalDatesKeepBirthDatesBefore1970() {
        assertNotNull(DotNetDate.parse("/Date(-315619200000)/"))
        assertEquals(Instant.fromEpochMilliseconds(0), DotNetDate.parse("/Date(0)/"))
        assertNull(DotNetDate.parse("/Date(-62135578800000)/"))
        assertNull(DotNetDate.parse("invalid"))
    }

    @Test
    fun guestJsonRoundTripRetainsAllDates() {
        val guest = fixtureGuest()
        val restored = Guest.fromJson(guest.toJson())
        assertEquals(guest, restored)
    }

    @Test
    fun listDoesNotHideServerErrorsOrMalformedPayloads() {
        assertFailsWith<PortalError.Rejected> {
            GuestListResponse.fromJson(PortalJson.parseToJsonElement("""{"Error":"Rejected"}"""))
        }
        assertFailsWith<PortalError.InvalidResponse> {
            GuestListResponse.fromJson(PortalJson.parseToJsonElement("""{"unexpected":[]}"""))
        }
    }

    @Test
    fun smsIsConfiguredAndUnsupportedDialCodesAreRejected() {
        assertEquals("7344", SmsReporter.SMS_DESTINATION)
        assertNull(SmsReporter.dialUri(""))
        assertNull(SmsReporter.dialUri("*123#"))
        assertEquals("tel:18808", SmsReporter.dialUri(SmsReporter.ASSOCIATION_PHONE))
    }

    /**
     * Mirrors the shape of `GET /Cuenta/Perfil` (synthetic data): `td.titulo` /
     * `td.texto` pairs inside `div.perfil`, entities in labels, an empty second name,
     * and the password form after the block.
     */
    @Test
    fun portalProfileParsesProfileTable() {
        val html = """
            <table class="tabla"><tr><td class="titulo">Registrado:</td><td class="texto">Ignored</td></tr></table>
            <h2>Mi Perfil</h2>
            <div class="row well perfil">
                <table class="tabla" >
                    <tr>
                        <td class="titulo">Primer Nombre</td><td class="texto">TESTA</td>
                        <td class="titulo">Segundo Nombre</td><td class="texto"></td>
                    </tr>
                    <tr>
                        <td class="titulo">Primer Apellido</td><td class="texto">UNO</td>
                        <td class="titulo">Segundo Apellido</td><td class="texto">DOS</td>
                    </tr>
                    <tr>
                        <td class="titulo">CI</td><td class="texto">00000000001</td>
                        <td class="titulo">Provincia</td><td class="texto">LA HABANA</td>
                    </tr>
                    <tr>
                        <td class="titulo">Usuario</td><td class="texto">X000Y</td>
                        <td class="titulo">Municipio</td><td class="texto">PLAYA</td>
                    </tr>
                    <tr>
                        <td class="titulo">Direcci&oacute;n</td>
                        <td class="texto" colspan="3">CALLE A, Nro. 1, Entre: B y C, Municipio: PLAYA</td>
                    </tr>
                </table>
            </div>
            <form class="well"><td class="titulo">Primer Nombre</td><td class="texto">FORM</td></form>
        """.trimIndent()
        val profile = PortalProfile.parse(html)
        assertEquals("TESTA UNO DOS", profile.fullName)
        assertEquals("", profile.secondName)
        assertEquals("00000000001", profile.identityNumber)
        assertEquals("X000Y", profile.username)
        assertEquals("LA HABANA", profile.province)
        assertEquals("PLAYA", profile.municipality)
        assertEquals("CALLE A, Nro. 1, Entre: B y C, Municipio: PLAYA", profile.address)
        assertFailsWith<PortalError.InvalidResponse> { PortalProfile.parse("<html><form>login</form></html>") }
    }

    @Test
    fun antiForgeryInputAttributeOrderAndQuotes() {
        assertEquals("a&b", AntiForgery.extract("<input value='a&amp;b' type='hidden' name='__RequestVerificationToken'>"))
        assertFailsWith<PortalError.AntiForgeryMissing> { AntiForgery.extract("<input name='other' value='x'>") }
    }

    @Test
    fun cubanIdIsElevenDigits() {
        assertTrue(com.casero.cu.model.CubanId.isValid("00000000001"))
        for (bad in listOf("", "0000000000", "000000000012", "0000000000A", "000 0000000")) {
            assertFalse(com.casero.cu.model.CubanId.isValid(bad), bad)
        }
        assertEquals("00000000001", com.casero.cu.model.CubanId.sanitize(" 000-000-000-01 99"))
    }
}
