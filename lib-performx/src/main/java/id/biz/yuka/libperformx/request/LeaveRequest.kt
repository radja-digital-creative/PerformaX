package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateLeaveRequest(
    val user_id: String,
    val leave_type_id: String,
    val start_date: String,
    val end_date: String,
    val reason: String,
    val attachment_url: String? = null
)

@Serializable
data class RespondLeaveRequest(
    val status: String, // 'approved' atau 'rejected'
    val approved_by: String,
    val approval_notes: String? = null
)


@Serializable
data class CreateLeaveTypeRequest(
    val name: String,
    val description: String? = null,
    val is_paid_leave: Boolean? = false,
    val rejection_penalty_multiplier: Double? = 0.0,
    val quota_period: String? = null,
    val quota_limit: Int? = null,
    val quota_min_months: Int? = 0
)

@Serializable
data class UpdateLeaveTypeRequest(
    val name: String? = null,
    val description: String? = null,
    val is_paid_leave: Boolean? = null,
    val rejection_penalty_multiplier: Double? = null,
    val quota_period: String? = null,
    val quota_limit: Int? = null,
    val quota_min_months: Int? = null
)