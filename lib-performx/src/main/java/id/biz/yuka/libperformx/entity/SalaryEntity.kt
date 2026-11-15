package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.DeductionComponentResponse
import id.biz.yuka.libperformx.response.SalaryDetailResponse

data class DeductionComponentEntity(
    val id: String,
    val name: String,
    val description: String,
    val calculationType: String,
    val fixedAmount: Double,
    val percentageValue: Double,
    val isActive: Boolean
)

fun DeductionComponentResponse.toEntity(): DeductionComponentEntity {
    return DeductionComponentEntity(
        id = this.id,
        name = this.name,
        description = this.description ?: "",
        calculationType = this.calculation_type ?: "fixed",
        fixedAmount = this.fixed_amount ?: 0.0,
        percentageValue = this.percentage_value ?: 0.0,
        isActive = this.is_active ?: true
    )
}

data class DeductionItemEntity(
    val relationId: String,
    val componentId: String,
    val name: String,
    val type: String,
    val fixedAmount: Double,
    val percentageValue: Double
)

data class SalaryDetailEntity(
    val salaryId: String,
    val userId: String,
    val userName: String,
    val payrollType: PayrollType,
    val basicSalary: Double,
    val transportAllowance: Double,
    val mealAllowance: Double,
    val positionAllowance: Double,
    val totalAllowance: Double,

    // ✅ Tambahan Properti Gaji Harian, Kehadiran, & Tanggal Periode
    val payPerDay: Double,
    val totalPresent: Int,
    val totalAlfa: Int,
    val startDate: String,
    val endDate: String,

    val totalDeduction: Double,
    val deductions: List<DeductionItemEntity>,

    val totalBonus: Double,
    val bonuses: List<BonusItemEntity>,

    val totalPenalty: Double,
    val penalties: List<PenaltyItemEntity>,

    val netSalary: Double, // Gaji Bersih
    val updatedAt: String
)

// Extension Function Mapper
fun SalaryDetailResponse.toEntity(): SalaryDetailEntity {

    val mappedDeductions = this.deductions?.map {
        DeductionItemEntity(
            relationId = it.deduction_relation_id,
            componentId = it.component_id,
            name = it.name,
            type = it.calculation_type,
            fixedAmount = it.fixed_amount,
            percentageValue = it.percentage_value
        )
    } ?: emptyList()

    val mappedBonuses = this.bonuses?.map {
        BonusItemEntity(
            id = it.id,
            amount = it.amount,
            notes = it.notes,
            bonusDate = it.bonus_date
        )
    } ?: emptyList()

    val mappedPenalties = this.penalties?.map {
        PenaltyItemEntity(
            id = it.id,
            penaltyAmount = it.penalty_amount,
            penaltyType = it.penalty_type,
            createdAt = it.created_at
        )
    } ?: emptyList()

    val pType = PayrollType.fromString(this.payroll_type)
    val baseIncome = if (pType == PayrollType.WEEKLY && this.pay_per_day > 0) {
        this.pay_per_day * this.total_present
    } else {
        this.basic_salary
    }

    // 🎯 Kalkulasi Take Home Pay
    val takeHomePay = (baseIncome + this.total_allowance + this.total_bonus) - (this.total_deduction + this.total_penalty)

    return SalaryDetailEntity(
        salaryId = this.salary_id,
        userId = this.user_id,
        userName = this.user_name,
        payrollType = pType,
        basicSalary = this.basic_salary,
        transportAllowance = this.transport_allowance,
        mealAllowance = this.meal_allowance,
        positionAllowance = this.position_allowance,
        totalAllowance = this.total_allowance,

        // ✅ Mapping nilai baru dari database
        payPerDay = this.pay_per_day,
        totalPresent = this.total_present,
        totalAlfa = this.total_alfa,
        startDate = this.start_date ?: "",
        endDate = this.end_date ?: "",

        totalDeduction = this.total_deduction,
        deductions = mappedDeductions,

        totalBonus = this.total_bonus,
        bonuses = mappedBonuses,

        totalPenalty = this.total_penalty,
        penalties = mappedPenalties,

        netSalary = takeHomePay,
        updatedAt = this.updated_at
    )
}