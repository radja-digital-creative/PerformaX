package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class EmployeeKpiRequest(
    val p_user_id: String,
    val p_month: Int,
    val p_year: Int
)