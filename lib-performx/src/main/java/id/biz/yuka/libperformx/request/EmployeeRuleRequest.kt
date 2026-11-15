package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

// --- 1. REQUEST ATURAN KARYAWAN ---
@Serializable
data class CreateEmployeeRuleRequest(
    val rule_name: String,
    val description: String? = null,
    val trigger_type: String,
    val trigger_mode: String? = null,
    val trigger_threshold: Int? = null,
    val penalty_type: String? = null,
    val penalty_multiplier: Double? = 0.0,
    val penalty_fixed_amount: Double? = 0.0,
    val penalty_status_action: String? = null,
    val rule_type: String = "violation",
    val reward_type: String? = null,
    val reward_multiplier: Double? = 0.0,
    val reward_fixed_amount: Double? = 0.0,
    val reward_status_action: String? = null
)

@Serializable
data class UpdateEmployeeRuleRequest(
    val rule_name: String? = null,
    val description: String? = null,
    val trigger_type: String? = null,
    val trigger_mode: String? = null,
    val trigger_threshold: Int? = null,
    val penalty_type: String? = null,
    val penalty_multiplier: Double? = null,
    val penalty_fixed_amount: Double? = null,
    val penalty_status_action: String? = null,
    val rule_type: String? = null,
    val reward_type: String? = null,
    val reward_multiplier: Double? = null,
    val reward_fixed_amount: Double? = null,
    val reward_status_action: String? = null,
    val is_active: Boolean? = null
)

// --- 2. REQUEST RELASI TOKO & ATURAN ---
@Serializable
data class CreateStoreRuleRequest(
    val store_id: String,
    val rule_id: String
)

@Serializable
data class CreateViolationLogRequest(
    val user_id: String,
    val user_name: String,
    val store_id: String,
    val rule_id: String,
    val reported_by: String, // ID Owner/Manager
    val violation_notes: String? = null,
    val evidence_attachment: String? = null,
    val applied_penalty: String,
    val violation_date: String
)