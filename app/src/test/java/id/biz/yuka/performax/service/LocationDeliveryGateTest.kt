package id.biz.yuka.performax.service
import org.junit.Assert.*
import org.junit.Test
class LocationDeliveryGateTest {
    @Test fun cachedOrFutureFixNeverBecomesNewUpload() {
        val gate = LocationDeliveryGate()
        assertFalse(gate.shouldUpload(100_000, 69_999, -7.0, 110.0))
        assertFalse(gate.shouldUpload(100_000, 100_001, -7.0, 110.0))
        assertTrue(gate.shouldUpload(100_000, 90_000, -7.0, 110.0))
    }
    @Test fun successLimitsRateButFailedUploadCanRetry() {
        val gate = LocationDeliveryGate()
        assertTrue(gate.shouldUpload(100_000, 100_000, -7.0, 110.0))
        assertTrue(gate.shouldUpload(101_000, 101_000, -7.0, 110.0))
        gate.markSuccess(101_000)
        assertFalse(gate.shouldUpload(115_999, 115_999, -7.0, 110.0))
        assertTrue(gate.shouldUpload(116_000, 116_000, -7.0, 110.0))
    }
    @Test fun stationaryDeviceStillSendsFreshHeartbeat() {
        val gate = LocationDeliveryGate()
        gate.markSuccess(100_000)
        assertTrue(gate.shouldUpload(115_000, 115_000, -7.0, 110.0))
    }
    @Test fun invalidCoordinatesNeverUpload() {
        val gate = LocationDeliveryGate()
        assertFalse(gate.shouldUpload(100, 100, Double.NaN, 110.0))
        assertFalse(gate.shouldUpload(100, 100, 91.0, 110.0))
        assertFalse(gate.shouldUpload(100, 100, -7.0, -181.0))
    }
}
