package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.AppealCommentResponse
import id.biz.yuka.libperformx.response.AppealMediaResponse
import id.biz.yuka.libperformx.response.AppealResponse

data class AppealEntity(
    val id: String,
    val violationLogId: String,
    val employeeId: String,
    val employeeName: String? = null,
    val status: String, // ACTIVE, APPROVED, REJECTED
    val isDataSufficient: Boolean,
    val createdAt: String
)

fun AppealResponse.toEntity() = AppealEntity(
    id = this.id,
    violationLogId = this.violation_log_id,
    employeeId = this.employee_id,
    employeeName = this.profiles?.full_name,
    status = this.status,
    isDataSufficient = this.is_data_sufficient,
    createdAt = this.created_at
)

data class AppealCommentEntity(
    val id: String,
    val appealId: String,
    val senderType: String,
    val senderId: String?,
    val message: String,
    var images: List<String> = emptyList(), // var agar Repository bisa memasukkan media
    val createdAt: String
)

fun AppealCommentResponse.toEntity() = AppealCommentEntity(
    id = this.id,
    appealId = this.appeal_id,
    senderType = this.sender_type,
    senderId = this.sender_id,
    message = this.message,
    createdAt = this.created_at
)

data class AppealMediaEntity(
    val id: String,
    val appealId: String,
    val commentId: String?,
    val fileUrl: String,
    val fileType: String,
    val createdAt: String
)

fun AppealMediaResponse.toEntity() = AppealMediaEntity(
    id = this.id,
    appealId = this.appeal_id,
    commentId = this.comment_id,
    fileUrl = this.file_url,
    fileType = this.file_type ?: "image",
    createdAt = this.created_at
)