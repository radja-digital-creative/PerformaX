package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

data class ScheduleIdResponse(val id: String)

@Serializable
data class ScheduleResponse(
    val id: String,
    val user_id: String,
    val store_id: String,
    val shift_id: String,
    val work_date: String,
    val notes: String? = null,
    val shifts: ShiftResponse? = null
)

@Serializable
data class ShiftResponse(
    val id: String,
    val store_id: String? = null,
    val shift_name: String,
    val start_time: String,
    val end_time: String,
    val attendance_radius: Int,
    val is_active: Boolean
)
@Serializable
data class ScheduleDetailResponse(
    val schedule_id: String,
    val user_id: String,
    val employee_name: String? = null,
    val store_id: String,
    val store_name: String? = null,
    val shift_id: String,
    val shift_name: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val work_date: String,
    val notes: String? = null
)

@Serializable
data class RoleRelation(val role_name: String)
@Serializable
data class ProfileRoleResponse(val roles: RoleRelation? = null)