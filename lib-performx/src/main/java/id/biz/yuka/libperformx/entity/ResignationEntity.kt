package id.biz.yuka.libperformx.entity

import kotlinx.serialization.Serializable

@Serializable
data class ResignationEntity(
    val id: String = "",
    val userId: String = "",
    val userName: String? = null,
    val reason: String = "",
    val proposedLastDay: String = "",
    val status: String = "pending", // pending, approved, rejected
    val approvedBy: String? = null,
    val rejectionReason: String? = null,
    val processedAt: String? = null,
    val createdAt: String? = null
)
