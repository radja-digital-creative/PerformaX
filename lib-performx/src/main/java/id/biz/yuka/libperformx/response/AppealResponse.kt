package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable


@Serializable
data class AppealResponse(
    val id: String,
    val violation_log_id: String,
    val employee_id: String,
    val status: String,
    val is_data_sufficient: Boolean,
    val created_at: String,
    val updated_at: String? = null,
    val profiles: ProfileNameResponse? = null
)

@Serializable
data class AppealCommentResponse(
    val id: String,
    val appeal_id: String,
    val sender_type: String, // 'employee', 'ai', 'owner'
    val sender_id: String? = null,
    val message: String,
    val created_at: String
)

@Serializable
data class AppealMediaResponse(
    val id: String,
    val appeal_id: String,
    val comment_id: String? = null, // Tambahan untuk relasi ke bubble chat
    val file_url: String,
    val file_type: String? = "image",
    val created_at: String
)
