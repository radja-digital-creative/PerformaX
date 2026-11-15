package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.LeaveResponse
import id.biz.yuka.libperformx.response.LeaveTypeResponse

data class LeaveEntity(
    val id: String,
    val userId: String,
    val employeeName: String,
    val typeName: String,
    val period: String, // Gabungan start - end date
    val reason: String,
    val status: String,
    val attachment: String,
    val adminNotes: String
)

fun LeaveResponse.toEntity() = LeaveEntity(
    id = this.leave_id,
    userId = this.user_id,
    employeeName = this.user_name,
    typeName = this.leave_type_name,
    period = "${this.start_date} s/d ${this.end_date}",
    reason = this.reason,
    status = this.status,
    attachment = this.attachment_url ?: "",
    adminNotes = this.approval_notes ?: ""
)

data class LeaveTypeEntity(
    val id: String,
    val name: String,
    val description: String,
    val isPaidLeave: Boolean,
    val rejectionPenaltyMultiplier: Double,
    val quotaPeriod: String?,
    val quotaLimit: Int?,
    val quotaMinMonths: Int
)

fun LeaveTypeResponse.toEntity() = LeaveTypeEntity(
    id = this.id,
    name = this.name,
    description = this.description ?: "Tidak ada deskripsi",
    isPaidLeave = this.is_paid_leave ?: false,
    rejectionPenaltyMultiplier = this.rejection_penalty_multiplier ?: 0.0,
    quotaPeriod = this.quota_period,
    quotaLimit = this.quota_limit,
    quotaMinMonths = this.quota_min_months ?: 0
)