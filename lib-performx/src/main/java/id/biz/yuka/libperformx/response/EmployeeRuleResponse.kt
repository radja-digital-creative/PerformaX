package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

// --- 1. RESPONSE ATURAN KARYAWAN ---
@Serializable
data class EmployeeRuleResponse(
    val id: String,
    val rule_name: String,
    val description: String? = null,
    val trigger_type: String,
    val trigger_mode: String? = null,
    val trigger_threshold: Int? = null,
    val penalty_type: String? = null,
    val penalty_multiplier: Double? = 0.0,
    val penalty_fixed_amount: Double? = 0.0,
    val penalty_status_action: String? = null,
    val rule_type: String? = "violation",
    val reward_type: String? = null,
    val reward_multiplier: Double? = 0.0,
    val reward_fixed_amount: Double? = 0.0,
    val reward_status_action: String? = null,
    val is_active: Boolean? = true,
    val created_at: String
)

// --- 2. RESPONSE LOG PELANGGARAN ---
@Serializable
data class ViolationLogResponse(
    val id: String,
    val user_id: String,
    val user_name: String,
    val store_id: String,
    val rule_id: String,
    val reported_by: String? = null,
    val violation_notes: String? = null,
    val evidence_attachment: String? = null,
    val violation_date: String,
    val applied_penalty: String,
    val is_processed_to_payroll: Boolean? = false,
    val status: String? = "PENDING", // <--- KOLOM BARU
    val created_at: String
)

// --- 3. RESPONSE RELASI TOKO & ATURAN ---
@Serializable
data class StoreRuleResponse(
    val id: String,
    val store_id: String,
    val rule_id: String,
    val is_active: Boolean? = true,
    val created_at: String
)