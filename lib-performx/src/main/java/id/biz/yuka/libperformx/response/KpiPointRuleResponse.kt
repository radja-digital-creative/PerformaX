package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class KpiPointRuleResponse(
    val rule_code: String,
    val point_value: Int,
    val description: String? = null
)