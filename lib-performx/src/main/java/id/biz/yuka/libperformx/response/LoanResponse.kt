package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class EmployeeLoanEligibilityResponse(
    val user_id: String,
    val full_name: String,
    val role_name: String? = null,
    val can_request_loan: Boolean
)

@Serializable
data class EmployeeLoanInstallmentResponse(
    val id: String,
    val loan_id: String,
    val installment_number: Int,
    val due_date: String,
    val amount: Double,
    val status: String,
    val processed_at: String? = null
)

@Serializable
data class EmployeeLoanResponse(
    val id: String,
    val user_id: String,
    val requested_amount: Double,
    val requested_tenor_months: Int,
    val reason: String? = null,
    val approved_amount: Double? = null,
    val approved_tenor_months: Int? = null,
    val monthly_installment: Double? = null,
    val status: String,
    val approved_by: String? = null,
    val approved_at: String? = null,
    val rejection_reason: String? = null,
    val created_at: String? = null,
    val employee_loan_installments: List<EmployeeLoanInstallmentResponse> = emptyList()
)