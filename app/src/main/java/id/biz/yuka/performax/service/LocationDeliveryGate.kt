package id.biz.yuka.performax.service

import id.biz.yuka.performax.utils.AttendancePolicy

class LocationDeliveryGate {
    private var lastSent: Long? = null
    fun shouldUpload(now: Long, fixTime: Long, latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            AttendancePolicy.isFreshLocation(now, fixTime) &&
            (lastSent == null || now - lastSent!! >= INTERVAL_MS)
    fun markSuccess(now: Long) { lastSent = now }
    companion object { const val INTERVAL_MS = 15_000L }
}
