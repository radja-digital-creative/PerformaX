package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateAppealRequest(
    val violation_log_id: String,
    val employee_id: String
)

@Serializable
data class CreateAppealCommentRequest(
    val appeal_id: String,
    val sender_type: String, // 'employee', 'ai', 'owner'
    val sender_id: String? = null,
    val message: String
)

// Request Baru untuk Tabel Media
@Serializable
data class CreateAppealMediaRequest(
    val appeal_id: String,
    val comment_id: String,
    val file_url: String,
    val file_type: String = "image"
)

@Serializable
data class UpdateAppealStatusRequest(
    val status: String? = null,
    val is_data_sufficient: Boolean? = null
)