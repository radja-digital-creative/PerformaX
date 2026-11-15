package id.biz.yuka.libperformx.response

import id.biz.yuka.libperformx.entity.ResignationEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResignationResponse(
    val id: String = "",
    val user_id: String = "",
    val reason: String = "",
    val proposed_last_day: String = "",
    val status: String = "pending",
    val approved_by: String? = null,
    val rejection_reason: String? = null,
    val processed_at: String? = null,
    val created_at: String? = null,
    val profiles: ProfileNameResponse? = null
)

fun ResignationResponse.toEntity(): ResignationEntity {
    return ResignationEntity(
        id = this.id,
        userId = this.user_id,
        userName = this.profiles?.full_name,
        reason = this.reason,
        proposedLastDay = this.proposed_last_day,
        status = this.status,
        approvedBy = this.approved_by,
        rejectionReason = this.rejection_reason,
        processedAt = this.processed_at,
        createdAt = this.created_at
    )
}
