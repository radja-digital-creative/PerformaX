package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

// Tambahkan di dalam ScheduleRequest.kt

@Serializable
data class UpdateScheduleShiftRequest(
    val shift_id: String,
    val notes: String? = null
)

@Serializable
data class BulkScheduleRequest(
    val p_user_id: String,
    val p_store_id: String,
    val p_shift_id: String,
    val p_active_days: List<Int>,
    val p_months_ahead: Int
)
// Digunakan khusus untuk UPDATE jadwal (Upsert)
@Serializable
data class UpsertScheduleRequest(
    val user_id: String,
    val store_id: String,
    val shift_id: String,
    val work_date: String,
    val notes: String? = null
)
@Serializable
data class CreateScheduleRequest(
    val user_id: String,
    val store_id: String,
    val shift_id: String,
    val work_date: String, // Format: YYYY-MM-DD
    val notes: String? = null
)

@Serializable
data class ShiftRequest(
    val store_id: String? = null, // Bisa null jika shift ini berlaku global
    val shift_name: String,
    val start_time: String, // Format: "HH:mm:ss"
    val end_time: String,   // Format: "HH:mm:ss"
    val attendance_radius: Int = 50, // Default 50 meter
    val is_active: Boolean = true
)