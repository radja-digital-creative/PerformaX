package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

// Untuk memanggil RPC create_new_store
@Serializable
data class CreateStoreRequest(
    val p_name: String,
    val p_address: String? = null,
    val p_phone_number: String? = null,
    val p_logo_url: String? = null,
    val p_latitude: Double? = null,
    val p_longitude: Double? = null,
    val p_open_before_minutes: Int? = 60,
    val p_late_tolerance_minutes: Int? = 15,
    val p_overtime_threshold_minutes: Int? = 60,
    val p_daily_close_time: String? = "22:00:00"
)

// Untuk Update tabel stores
@Serializable
data class UpdateStoreRequest(
    val name: String? = null,
    val address: String? = null,
    val phone_number: String? = null,
    val logo_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_active: Boolean? = null
)

// Untuk Update tabel attendance_settings
@Serializable
data class UpdateAttendanceSettingRequest(
    val open_before_minutes: Int? = null,
    val late_tolerance_minutes: Int? = null,
    val overtime_threshold_minutes: Int? = null,
    val daily_close_time: String? = null
)