package id.biz.yuka.libperformx.entity

import kotlinx.serialization.Serializable

@Serializable
data class EmployeeAttendancePreferences(
    val user_id: String,
    val require_photo: Boolean = true,
    val allow_any_time: Boolean = false,
    val max_check_in_time: String? = null,
    val default_store_id: String? = null,
    val default_shift_id: String? = null,
    val work_days: List<Int> = listOf(1, 2, 3, 4, 5, 6)
)
