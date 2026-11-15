package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.EmployeeResponse

// Store sederhana khusus Employee
data class EmployeeStoreEntity(
    val id: String,
    val name: String,
    val isPrimary: Boolean = false
)

// ✅ ENTITY BARU UNTUK POTONGAN
data class EmployeeDeductionEntity(
    val id: String,
    val name: String,
    val fixedAmount: Double,
    val percentageValue: Double
)

data class EmployeeEntity(
    val id: String,
    val fullName: String,
    val gender: String,
    val phoneNumber: String,
    val address: String,
    val isActive: Boolean,
    val roleName: String,
    val profileUrl: String,
    val joinDate: String?, // ✅ TAMBAHAN: Tanggal bergabung
    val faceEmbedding: String,
    val ktpMatchScore: Double,
    val stores: List<EmployeeStoreEntity>,
    val canRequestLoan: Boolean,
    // Finansial
    val payrollType: PayrollType,
    val payPerDay: Double,
    val basicSalary: Double,
    val transportAllowance: Double,
    val mealAllowance: Double,
    val positionAllowance: Double,
    val totalAllowances: Double,
    val perMinute: Double,

    // ✅ UBAH TIPE DATA INI
    val deductions: List<EmployeeDeductionEntity>
)

// Standar Mapper
fun EmployeeResponse.toEntity(): EmployeeEntity {
    val tnjTransport = this.transport_allowance ?: 0.0
    val tnjMeal = this.meal_allowance ?: 0.0
    val tnjPosition = this.position_allowance ?: 0.0

    return EmployeeEntity(
        id = this.user_id,
        fullName = this.full_name,
        gender = this.gender ?: "-",
        phoneNumber = this.phone_number ?: "-",
        address = this.address ?: "-",
        isActive = this.profile_active,
        roleName = this.role_name ?: "Unknown",
        profileUrl = this.profile_url ?: "",
        joinDate = this.join_date, // ✅ MAPPING BARU
        faceEmbedding = this.face_embedding ?: "",
        ktpMatchScore = this.ktp_match_score ?: 0.0,
        stores = this.assigned_stores.map { EmployeeStoreEntity(it.id, it.name, it.is_primary) },
        canRequestLoan = this.can_request_loan ?: false,
        payrollType = PayrollType.fromString(this.payroll_type),
        payPerDay = this.pay_per_day ?: 0.0,
        basicSalary = this.basic_salary ?: 0.0,
        transportAllowance = tnjTransport,
        mealAllowance = tnjMeal,
        positionAllowance = tnjPosition,
        totalAllowances = (tnjTransport + tnjMeal + tnjPosition),
        perMinute = this.per_minute ?: 0.0,

        // ✅ MAPPING POTONGAN
        deductions = this.active_deductions.map {
            EmployeeDeductionEntity(
                id = it.id,
                name = it.name,
                fixedAmount = it.fixed_amount ?: 0.0,
                percentageValue = it.percentage_value ?: 0.0
            )
        }
    )
}