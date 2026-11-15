package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class AssignedStoreResponse(
    val id: String,
    val name: String,
    val is_primary: Boolean = false
)

// ✅ DTO BARU UNTUK MENANGKAP JSON DARI VIEW
@Serializable
data class ActiveDeductionResponse(
    val id: String,
    val name: String,
    val fixed_amount: Double? = 0.0,
    val percentage_value: Double? = 0.0
)

@Serializable
data class EmployeeResponse(
    val user_id: String,
    val full_name: String,
    val gender: String? = null,
    val phone_number: String? = null,
    val address: String? = null,
    val id_card_url: String? = null,
    val face_reference_url: String? = null,
    val profile_active: Boolean,
    val joined_at: String? = null,
    val join_date: String? = null, // ✅ TAMBAHAN: Tanggal bergabung (YYYY-MM-DD)

    val profile_url: String? = null,
    val face_embedding: String? = null,
    val ktp_match_score: Double? = null,
    val can_request_loan: Boolean? = false,
    // Role
    val role_name: String? = null,
    val assigned_stores: List<AssignedStoreResponse> = emptyList(),

    // Gaji & Potongan (Khusus HR/Admin)
    val payroll_type: String? = "monthly",
    val pay_per_day: Double? = 0.0,
    val basic_salary: Double? = null,
    val transport_allowance: Double? = null,
    val meal_allowance: Double? = null,
    val position_allowance: Double? = null,
    val per_minute: Double? = 0.0,

    // ✅ UBAH TIPE DATA INI
    val active_deductions: List<ActiveDeductionResponse> = emptyList()
)