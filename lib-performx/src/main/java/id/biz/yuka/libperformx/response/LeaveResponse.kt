package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class LeaveResponse(
    val leave_id: String,
    val user_id: String,
    val user_name: String,
    val leave_type_id: String,
    val leave_type_name: String,
    val start_date: String,
    val end_date: String,
    val reason: String,
    val attachment_url: String? = null,
    val status: String,
    val approved_by: String? = null,
    val approved_by_name: String? = null,
    val approval_notes: String? = null,
    val created_at: String
)

@Serializable
data class LeaveTypeResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val is_paid_leave: Boolean? = false,
    val rejection_penalty_multiplier: Double? = 0.0,
    val quota_period: String? = null,
    val quota_limit: Int? = null,
    val quota_min_months: Int? = 0
)