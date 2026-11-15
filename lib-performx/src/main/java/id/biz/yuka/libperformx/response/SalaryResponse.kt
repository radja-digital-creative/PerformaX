package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class BonusItemResponse(
    val id: String,
    val amount: Double,
    val notes: String,
    val bonus_date: String
)

@Serializable
data class PenaltyItemResponse(
    val id: String,
    val penalty_amount: Double,
    val penalty_type: String,
    val created_at: String
)

@Serializable
data class DeductionComponentResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val calculation_type: String? = null,
    val fixed_amount: Double? = 0.0,
    val percentage_value: Double? = 0.0,
    val is_active: Boolean? = true
)

@Serializable
data class DeductionItemResponse(
    val deduction_relation_id: String,
    val component_id: String,
    val name: String,
    val calculation_type: String,
    val fixed_amount: Double,
    val percentage_value: Double
)

@Serializable
data class SalaryDetailResponse(
    val salary_id: String,
    val user_id: String,
    val user_name: String,
    val basic_salary: Double,
    val transport_allowance: Double,
    val meal_allowance: Double,
    val position_allowance: Double,
    val total_allowance: Double,

    // ✅ Tambahan Kolom Baru untuk Gaji Harian, Kehadiran, & Tipe Payroll
    val payroll_type: String? = "monthly",
    val pay_per_day: Double = 0.0,
    val total_present: Int = 0,
    val total_alfa: Int = 0,
    val start_date: String? = null,
    val end_date: String? = null,

    val total_deduction: Double = 0.0,
    val deductions: List<DeductionItemResponse>? = null,

    val total_bonus: Double = 0.0,
    val bonuses: List<BonusItemResponse>? = null,

    val total_penalty: Double = 0.0,
    val penalties: List<PenaltyItemResponse>? = null,

    val updated_at: String
)