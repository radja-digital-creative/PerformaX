package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

// Tambahkan Data Class untuk struktur Store
@Serializable
data class UserStoreRequest(
    val store_id: String,
    val is_primary: Boolean
)

// Tambahkan Data Class untuk struktur Schedule
@Serializable
data class UserScheduleRequest(
    val store_id: String,
    val shift_id: String,
    val work_date: String,
    val notes: String? = null
)

@Serializable
data class RegisterEmployeeRequest(
    val email: String,
    val password: String,
    val p_role_id: Int,
    val p_full_name: String,
    val p_gender: String,
    val p_address: String,
    val p_phone_number: String,
    val p_id_card_url: String,
    val p_face_reference_url: String,

    // --- PERUBAHAN: Array untuk Toko dan Jadwal ---
    val p_stores: List<UserStoreRequest> = emptyList(),
    val p_schedules: List<UserScheduleRequest> = emptyList(),
    // ----------------------------------------------

    val p_basic_salary: Double,
    val p_transport_allowance: Double,
    val p_meal_allowance: Double,
    val p_position_allowance: Double,
    val p_deduction_ids: List<String> = emptyList(),
    val p_payroll_type: String = "monthly",
    val p_pay_per_day: Double = 0.0
)

@Serializable
data class UpdateEmployeeRequest(
    val p_user_id: String, // Wajib diisi!
    val email: String? = null,
    val password: String? = null, // Jika null/kosong, tidak akan mengubah password di server
    val p_role_id: Int? = null,
    val p_full_name: String? = null,
    val p_gender: String? = null,
    val p_address: String? = null,
    val p_phone_number: String? = null,

    // Multiple Store & Schedule (Gunakan Data Class dari RegisterEmployeeRequest yang sudah kita buat)
    val p_stores: List<UserStoreRequest>? = null,
    val p_schedules: List<UserScheduleRequest>? = null,

    val p_payroll_type: String? = null,
    val p_pay_per_day: Double? = null,
    val p_basic_salary: Double? = null,
    val p_transport_allowance: Double? = null,
    val p_meal_allowance: Double? = null,
    val p_position_allowance: Double? = null,
    val p_deduction_ids: List<String>? = null,
    val p_per_minute: Double? = null,
    val p_join_date: String? = null // ✅ TAMBAHAN: Tanggal bergabung (YYYY-MM-DD)
)

@Serializable
data class UpdateKycRequest(
    val ktp_image_url: String?,
    val profile_url: String?,
    val face_embedding: String,
    val ktp_match_score: Double
)

@Serializable
data class SoftDeleteEmployeeRequest(
    val is_active: Boolean = false
)