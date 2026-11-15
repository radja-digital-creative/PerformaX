package id.biz.yuka.libperformx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateManualDeductionRequest(
    @SerialName("user_salary_id")
    val userSalaryId: String,
    @SerialName("title")
    val title: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("notes")
    val notes: String? = null
)


@Serializable
data class InsertManualDeductionDto(
    val title: String,
    val amount: Double,
    val notes: String? = null,
    @SerialName("user_salary_id")
    val user_salary_id: String? = null
)

@Serializable
data class InsertUserSalaryDeductionDto(
    @SerialName("user_salary_id")
    val user_salary_id: String,
    @SerialName("manual_deduction_id")
    val manual_deduction_id: String
)
