package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.EmployeeRuleResponse
import id.biz.yuka.libperformx.response.StoreRuleResponse
import id.biz.yuka.libperformx.response.ViolationLogResponse

// --- 1. ENTITY ATURAN KARYAWAN ---
data class EmployeeRuleEntity(
    val id: String,
    val ruleName: String,
    val description: String?,
    val triggerType: String,
    val triggerMode: String?,
    val triggerThreshold: Int?,
    val ruleType: String,
    val penaltyType: String?,
    val penaltyMultiplier: Double,
    val penaltyFixedAmount: Double,
    val penaltyStatusAction: String?,
    val rewardType: String?,
    val rewardMultiplier: Double,
    val rewardFixedAmount: Double,
    val rewardStatusAction: String?,
    val isActive: Boolean
)

fun EmployeeRuleResponse.toEntity() = EmployeeRuleEntity(
    id = this.id,
    ruleName = this.rule_name,
    description = this.description,
    triggerType = this.trigger_type,
    triggerMode = this.trigger_mode,
    triggerThreshold = this.trigger_threshold,
    ruleType = this.rule_type ?: "violation",
    penaltyType = this.penalty_type,
    penaltyMultiplier = this.penalty_multiplier ?: 0.0,
    penaltyFixedAmount = this.penalty_fixed_amount ?: 0.0,
    penaltyStatusAction = this.penalty_status_action,
    rewardType = this.reward_type,
    rewardMultiplier = this.reward_multiplier ?: 0.0,
    rewardFixedAmount = this.reward_fixed_amount ?: 0.0,
    rewardStatusAction = this.reward_status_action,
    isActive = this.is_active ?: true
)

// --- 2. ENTITY LOG PELANGGARAN ---
data class ViolationLogEntity(
    val id: String,
    val userId: String,
    val userName: String,
    val storeId: String,
    val ruleId: String,
    val reportedBy: String?,
    val violationNotes: String?,
    val evidenceAttachment: String?,
    val violationDate: String,
    val appliedPenalty: String,
    val isProcessedToPayroll: Boolean,
    val status: String, // <--- KOLOM BARU
    val createdAt: String
)

fun ViolationLogResponse.toEntity() = ViolationLogEntity(
    id = this.id,
    userId = this.user_id,
    userName = this.user_name,
    storeId = this.store_id,
    ruleId = this.rule_id,
    reportedBy = this.reported_by,
    violationNotes = this.violation_notes,
    evidenceAttachment = this.evidence_attachment,
    violationDate = this.violation_date,
    appliedPenalty = this.applied_penalty,
    isProcessedToPayroll = this.is_processed_to_payroll ?: false,
    status = this.status ?: "PENDING", // <--- MAPPER BARU
    createdAt = this.created_at
)

// --- 3. ENTITY RELASI TOKO & ATURAN ---
data class StoreRuleEntity(
    val id: String,
    val storeId: String,
    val ruleId: String,
    val isActive: Boolean
)

fun StoreRuleResponse.toEntity() = StoreRuleEntity(
    id = this.id,
    storeId = this.store_id,
    ruleId = this.rule_id,
    isActive = this.is_active ?: true
)