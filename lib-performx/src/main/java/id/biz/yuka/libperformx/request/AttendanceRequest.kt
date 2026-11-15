package id.biz.yuka.libperformx.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessAttendanceRequest(
    val p_user_id: String,
    val p_schedule_id: String,
    val p_latitude: Double,
    val p_longitude: Double,
    val p_image_url: String,
    val p_attendance_type: String,
    val p_is_manual_flag: Boolean = false
)

@Serializable
data class AnnulAttendanceRequest(
    @SerialName("is_annulled")
    val is_annulled: Boolean = true,
    val annulled_by: String, // ID Admin/Owner yang menganulir
    val annul_reason: String
)