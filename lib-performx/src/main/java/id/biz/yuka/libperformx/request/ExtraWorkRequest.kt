package id.biz.yuka.libperformx.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateExtraWorkRequest(
    @SerialName("user_id")
    val user_id: String,
    @SerialName("store_id")
    val store_id: String,
    @SerialName("shift_id")
    val shift_id: String,
    @SerialName("work_date")
    val work_date: String,
    @SerialName("check_in_time")
    val check_in_time: String,
    @SerialName("check_in_latitude")
    val check_in_latitude: Double,
    @SerialName("check_in_longitude")
    val check_in_longitude: Double,
    @SerialName("check_in_image_url")
    val check_in_image_url: String,
    @SerialName("reason")
    val reason: String
)

@Serializable
data class RespondExtraWorkRequest(
    @SerialName("status")
    val status: String, // 'approved' atau 'rejected'
    @SerialName("approved_by")
    val approved_by: String,
    @SerialName("approval_notes")
    val approval_notes: String? = null,
    @SerialName("approved_at")
    val approved_at: String? = null
)
