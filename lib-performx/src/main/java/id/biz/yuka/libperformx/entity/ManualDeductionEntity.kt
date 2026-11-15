package id.biz.yuka.libperformx.entity

import kotlinx.serialization.Serializable

@Serializable
data class ManualDeductionEntity(
    val id: String = "",
    val userSalaryId: String? = null,
    val title: String = "",
    val amount: Double = 0.0,
    val notes: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val userName: String? = null
)
