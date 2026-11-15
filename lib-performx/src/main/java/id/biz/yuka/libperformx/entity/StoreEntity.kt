package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.StoreDetailResponse
import id.biz.yuka.libperformx.response.StoreResponse

// Entity List Sederhana
data class StoreEntity(
    val id: String,
    val name: String,
    val address: String? = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isActive: Boolean = true
)

fun StoreResponse.toEntity(): StoreEntity {
    return StoreEntity(
        id = this.id,
        name = this.name,
        address = this.address ?: "",
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        isActive = this.is_active
    )
}

// Entity Detail Lengkap
data class StoreDetailEntity(
    val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val logoUrl: String,
    val latitude: Double,
    val longitude: Double,
    val isActive: Boolean,

    // Setting Absen (Bisa kita kelompokkan agar lebih rapi)
    val settingId: String,
    val openBeforeMinutes: Int,
    val lateToleranceMinutes: Int,
    val overtimeThresholdMinutes: Int,
    val closeTime: String
)

fun StoreDetailResponse.toEntity(): StoreDetailEntity {
    return StoreDetailEntity(
        id = this.store_id,
        name = this.store_name,
        address = this.address ?: "",
        phone = this.phone_number ?: "",
        logoUrl = this.logo_url ?: "",
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        isActive = this.is_active,

        settingId = this.setting_id ?: "",
        openBeforeMinutes = this.open_before_minutes ?: 0,
        lateToleranceMinutes = this.late_tolerance_minutes ?: 0,
        overtimeThresholdMinutes = this.overtime_threshold_minutes ?: 0,
        closeTime = this.daily_close_time ?: "22:00:00"
    )
}