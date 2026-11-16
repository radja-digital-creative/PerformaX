package id.biz.yuka.performax.utils

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class AttendancePolicyTest {
    @Test fun unrestrictedAttendanceAcceptsTimeAfterCutoff() {
        assertTrue(AttendancePolicy.canAttend(LocalTime.NOON, LocalTime.of(7,0), LocalTime.of(8,15), true))
        assertFalse(AttendancePolicy.canAttend(LocalTime.NOON, LocalTime.of(7,0), LocalTime.of(8,15), false))
    }
    @Test fun overnightWindowIncludesBothEnds() {
        assertTrue(AttendancePolicy.canAttend(LocalTime.of(0,10), LocalTime.of(23,0), LocalTime.of(1,0), false))
        assertFalse(AttendancePolicy.canAttend(LocalTime.NOON, LocalTime.of(23,0), LocalTime.of(1,0), false))
    }
    @Test fun staleAndFutureFixesAreRejected() {
        assertTrue(AttendancePolicy.isFreshLocation(100000, 85000))
        assertFalse(AttendancePolicy.isFreshLocation(100000, 69999))
        assertFalse(AttendancePolicy.isFreshLocation(100000, 100001))
    }
}
