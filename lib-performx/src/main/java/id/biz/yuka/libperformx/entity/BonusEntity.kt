package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.BonusResponse

data class BonusEntity(
    val id: String,
    val userId: String,
    val userName: String,
    val grantedBy: String?,
    val amount: Double,
    val notes: String,
    val bonusDate: String,
    val isProcessed: Boolean,
    val createdAt: String
)

// Extension function untuk mapping otomatis dari Response ke Entity
fun BonusResponse.toEntity() = BonusEntity(
    id = this.id,
    userId = this.user_id,
    userName = this.user_name,
    grantedBy = this.granted_by,
    amount = this.amount,
    notes = this.notes,
    bonusDate = this.bonus_date,
    isProcessed = this.is_processed ?: false,
    createdAt = this.created_at
)

data class BonusItemEntity(
    val id: String,
    val amount: Double,
    val notes: String,
    val bonusDate: String
)

data class PenaltyItemEntity(
    val id: String,
    val penaltyAmount: Double,
    val penaltyType: String,
    val createdAt: String
)