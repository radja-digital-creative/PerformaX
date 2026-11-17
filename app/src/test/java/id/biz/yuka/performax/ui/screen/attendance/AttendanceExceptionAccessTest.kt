package id.biz.yuka.performax.ui.screen.attendance

import id.biz.yuka.libperformx.repository.AttendanceExceptionAccess
import org.junit.Assert.*
import org.junit.Test

class AttendanceExceptionAccessTest {
    @Test fun pendingRequestsAllowRecordingWithoutChangingStatus() {
        val access = AttendanceExceptionAccess("pending", "pending", "pending")
        assertTrue(access.allowsOffsite)
        assertTrue(access.allowsLateArrival)
        assertTrue(access.allowsEarlyDeparture)
        assertTrue(access.hasPendingFor("check_in"))
        assertTrue(access.hasPendingFor("check_out"))
        assertEquals("pending", access.offsite_status)
        assertEquals("pending", access.late_status)
        assertEquals("pending", access.early_status)
    }

    @Test fun rejectedMissingAndUnknownStatusesDoNotGrantAccess() {
        for (status in listOf(null, "rejected", "cancelled", "unexpected")) {
            val access = AttendanceExceptionAccess(status, status, status)
            assertFalse(access.allowsOffsite)
            assertFalse(access.allowsLateArrival)
            assertFalse(access.allowsEarlyDeparture)
            assertFalse(access.hasPendingFor("check_in"))
        }
    }

    @Test fun arrivalAndDepartureExceptionsStaySeparate() {
        val arrival = AttendanceExceptionAccess(late_status = "pending")
        assertTrue(arrival.allowsLateArrival)
        assertFalse(arrival.allowsEarlyDeparture)
        assertTrue(arrival.hasPendingFor("check_in"))
        assertFalse(arrival.hasPendingFor("check_out"))
        val departure = AttendanceExceptionAccess(early_status = "pending")
        assertFalse(departure.allowsLateArrival)
        assertTrue(departure.allowsEarlyDeparture)
        assertFalse(departure.hasPendingFor("check_in"))
        assertTrue(departure.hasPendingFor("check_out"))
    }

    @Test fun approvedRequestsAllowAccessWithoutPendingMessage() {
        val access = AttendanceExceptionAccess("approved", "approved", "approved")
        assertTrue(access.allowsOffsite)
        assertTrue(access.allowsLateArrival)
        assertTrue(access.allowsEarlyDeparture)
        assertFalse(access.hasPendingFor("check_in"))
        assertFalse(access.hasPendingFor("check_out"))
    }
}
