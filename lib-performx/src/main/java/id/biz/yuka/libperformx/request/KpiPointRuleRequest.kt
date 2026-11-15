package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class KpiPointRuleRequest(
    val rule_code: String,
    val point_value: Int,
    val description: String? = null
)