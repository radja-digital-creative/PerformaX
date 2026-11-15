package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class ManualAttendanceResponse(
    val id: String,
    val user_id: String,
    val schedule_id: String? = null,
    val attendance_date: String,
    val check_in_time: String? = null,
    val check_in_latitude: Double? = null,
    val check_in_longitude: Double? = null,
    val check_in_image_url: String? = null,
    val check_in_status: String? = null,
    val check_out_time: String? = null,
    val check_out_latitude: Double? = null,
    val check_out_longitude: Double? = null,
    val check_out_image_url: String? = null,
    val check_out_status: String? = null,
    val earned_points: Int? = 0,
    val approval_status: String = "pending",
    val approved_by: String? = null,
    val approval_reason: String? = null,
    val approved_at: String? = null,
    val created_at: String? = null,

    // Nested/Joined Data (Dari view atau rpc)
    val user_name: String? = null,
    val profile_url: String? = null,
    val store_name: String? = null
)
