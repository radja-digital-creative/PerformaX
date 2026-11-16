package id.biz.yuka.performax.ui.screen.location

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class LocationFreshnessTest {
    private val now = Instant.parse("2026-11-16T12:00:00Z")

    @Test fun twoMinuteBoundaryStillCountsAsLatestLocation() {
        val status = locationFreshness(now.minusSeconds(120).toString(), now)
        assertTrue(status.isFresh)
        assertEquals("Lokasi terbaru • 2 menit lalu", status.label)
    }

    @Test fun olderFixIsLastLocationWithoutClaimingOffline() {
        val status = locationFreshness(now.minusSeconds(121).toString(), now)
        assertFalse(status.isFresh)
        assertEquals("Lokasi terakhir • 2 menit lalu", status.label)
    }

    @Test fun futureTimestampBeyondToleranceIsClockMismatch() {
        val status = locationFreshness(now.plusSeconds(61).toString(), now)
        assertFalse(status.isFresh)
        assertEquals("Waktu perangkat tidak sesuai", status.label)
    }

    @Test fun smallClockSkewIsTolerated() {
        val status = locationFreshness(now.plusSeconds(60).toString(), now)
        assertTrue(status.isFresh)
        assertEquals("Lokasi terbaru • Baru saja", status.label)
    }

    @Test fun missingAndMalformedTimestampsAreUnknown() {
        for (timestamp in listOf(null, "", "bad timestamp")) {
            val status = locationFreshness(timestamp, now)
            assertFalse(status.isFresh)
            assertEquals("Waktu tidak diketahui", status.label)
        }
    }

    @Test fun offsetTimestampRefersToSameInstant() {
        assertEquals("Lokasi terbaru • Baru saja", locationFreshness("2026-11-16T19:00:00+07:00", now).label)
    }
}
