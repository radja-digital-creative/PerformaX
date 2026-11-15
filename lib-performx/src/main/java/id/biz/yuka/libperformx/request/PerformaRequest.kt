package id.biz.yuka.libperformx.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 1. Request untuk fungsi get_performance_summary
@Serializable
data class SummaryPerformaRequest(
    @SerialName("p_user_id") val pUserId: String
)

// 2. Request untuk fungsi get_performance
@Serializable
data class DetailPerformaRequest(
    @SerialName("p_user_id") val pUserId: String,
    @SerialName("p_month") val pMonth: Int,
    @SerialName("p_year") val pYear: Int
)