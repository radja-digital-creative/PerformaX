package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateDeductionComponentRequest(
    val name: String,
    val description: String? = null,
    val calculation_type: String = "fixed", // Bisa 'fixed' atau 'percentage'
    val fixed_amount: Double = 0.0,
    val percentage_value: Double = 0.0,
    val is_active: Boolean = true
)

@Serializable
data class AddDeductionToEmployeeRequest(
    val user_salary_id: String,
    val deduction_id: String
)

@Serializable
data class HistoricalSalaryRequest(
    val p_user_id: String? = null,
    val p_month: Int? = null,
    val p_year: Int? = null,
    val p_start_date: String? = null,
    val p_end_date: String? = null
)