package id.biz.yuka.libperformx.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessResignationRequest(
    @SerialName("resignation_id")
    val resignation_id: String,
    @SerialName("status")
    val status: String,
    @SerialName("approved_by")
    val approved_by: String? = null,
    @SerialName("rejection_reason")
    val rejection_reason: String? = null,
    @SerialName("processed_at")
    val processed_at: String? = null
)
