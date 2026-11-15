package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceResponse(
    val id: String,
    val user_id: String,
    val attendance_date: String,
    val check_in_time: String? = null,
    val check_out_time: String? = null,
    val check_in_status: String,
    val check_out_status: String,
    val earned_points: Int
)

@Serializable
data class AttendanceDetailResponse(
    val attendance_id: String,
    val user_id: String,
    val user_name: String,
    val store_id: String,
    val store_name: String,
    val attendance_date: String,
    val check_in_time: String? = null,
    val check_in_status: String? = null,
    val check_in_latitude: Double? = null,
    val check_in_longitude: Double? = null,
    val check_in_image_url: String? = null,
    val check_out_time: String? = null,
    val check_out_status: String? = null,
    val check_out_latitude: Double? = null,
    val check_out_longitude: Double? = null,
    val check_out_image_url: String? = null,
    val earned_points: Int? = 0,
    val is_annulled: Boolean,
    val annul_reason: String? = null,
    val annulled_by_name: String? = null
)