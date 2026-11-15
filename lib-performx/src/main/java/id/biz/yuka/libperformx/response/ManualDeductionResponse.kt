package id.biz.yuka.libperformx.response

import id.biz.yuka.libperformx.entity.ManualDeductionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ManualDeductionResponse(
    val id: String = "",
    @SerialName("user_salary_id")
    val user_salary_id: String? = null,
    val title: String = "",
    val amount: Double = 0.0,
    val notes: String? = null,
    @SerialName("created_by")
    val created_by: String? = null,
    @SerialName("created_at")
    val created_at: String? = null,
    val profiles: ProfileNameResponse? = null
)

fun ManualDeductionResponse.toEntity(): ManualDeductionEntity {
    return ManualDeductionEntity(
        id = this.id,
        userSalaryId = this.user_salary_id,
        title = this.title,
        amount = this.amount,
        notes = this.notes,
        createdBy = this.created_by,
        createdAt = this.created_at,
        userName = this.profiles?.full_name
    )
}
