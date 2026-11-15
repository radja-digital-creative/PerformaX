package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.EmployeeLoanEligibilityResponse
import id.biz.yuka.libperformx.response.EmployeeLoanInstallmentResponse
import id.biz.yuka.libperformx.response.EmployeeLoanResponse
import kotlinx.serialization.Serializable

data class EmployeeLoanEligibilityEntity(
    val userId: String,
    val fullName: String,
    val roleName: String,
    val canRequestLoan: Boolean
)

data class EmployeeLoanInstallmentEntity(
    val id: String,
    val installmentNumber: Int,
    val dueDate: String,
    val amount: Double,
    val status: String,
    val processedAt: String
)

data class EmployeeLoanEntity(
    val id: String,
    val userId: String,
    val requestedAmount: Double,
    val requestedTenorMonths: Int,
    val reason: String,
    val approvedAmount: Double,
    val approvedTenorMonths: Int,
    val monthlyInstallment: Double,
    val status: String,
    val createdAt: String,
    val installments: List<EmployeeLoanInstallmentEntity>
)

// MAPPERS
fun EmployeeLoanEligibilityResponse.toEntity() = EmployeeLoanEligibilityEntity(
    userId = this.user_id,
    fullName = this.full_name,
    roleName = this.role_name ?: "Employee",
    canRequestLoan = this.can_request_loan
)

fun EmployeeLoanInstallmentResponse.toEntity() = EmployeeLoanInstallmentEntity(
    id = this.id,
    installmentNumber = this.installment_number,
    dueDate = this.due_date,
    amount = this.amount,
    status = this.status,
    processedAt = this.processed_at ?: ""
)

fun EmployeeLoanResponse.toEntity() = EmployeeLoanEntity(
    id = this.id,
    userId = this.user_id,
    requestedAmount = this.requested_amount,
    requestedTenorMonths = this.requested_tenor_months,
    reason = this.reason ?: "",
    approvedAmount = this.approved_amount ?: 0.0,
    approvedTenorMonths = this.approved_tenor_months ?: 0,
    monthlyInstallment = this.monthly_installment ?: 0.0,
    status = this.status,
    createdAt = this.created_at ?: "",
    installments = this.employee_loan_installments.map { it.toEntity() }
)