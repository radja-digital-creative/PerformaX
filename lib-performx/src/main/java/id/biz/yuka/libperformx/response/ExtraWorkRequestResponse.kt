package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class ExtraWorkRequestResponse(
    val id: String,
    val user_id: String,
    val store_id: String,
    val shift_id: String,
    val work_date: String,
    val check_in_time: String,
    val check_in_latitude: Double,
    val check_in_longitude: Double,
    val check_in_image_url: String,
    val reason: String,
    val status: String = "pending",
    val approved_by: String? = null,
    val approval_notes: String? = null,
    val approved_at: String? = null,
    val created_at: String? = null,

    // Joined Relasi
    val user_name: String? = null,
    val profile_url: String? = null,
    val store_name: String? = null,
    val shift_name: String? = null,
    val shift_start_time: String? = null,
    val shift_end_time: String? = null
)
