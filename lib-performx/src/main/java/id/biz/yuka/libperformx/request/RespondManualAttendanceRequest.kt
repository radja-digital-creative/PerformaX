package id.biz.yuka.libperformx.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RespondManualAttendanceRequest(
    @SerialName("approval_status")
    val approval_status: String, // 'approved' atau 'rejected'
    @SerialName("approved_by")
    val approved_by: String,
    @SerialName("approval_reason")
    val approval_reason: String? = null,
    @SerialName("approved_at")
    val approved_at: String? = null
)
