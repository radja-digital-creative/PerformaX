package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateBonusRequest(
    val user_id: String,
    val user_name: String,
    val granted_by: String, // ID Owner/Manajer yang memberikan
    val amount: Double,
    val notes: String,
    val bonus_date: String
)