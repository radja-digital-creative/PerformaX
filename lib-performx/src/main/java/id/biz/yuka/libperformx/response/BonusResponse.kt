package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class BonusResponse(
    val id: String,
    val user_id: String,
    val user_name: String,
    val granted_by: String? = null,
    val amount: Double,
    val notes: String,
    val bonus_date: String,
    val is_processed: Boolean? = false,
    val created_at: String
)
