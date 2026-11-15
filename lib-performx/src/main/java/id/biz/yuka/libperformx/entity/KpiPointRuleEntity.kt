package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.KpiPointRuleResponse

data class KpiPointRuleEntity(
    val ruleCode: String,
    val pointValue: Int,
    val description: String?
)

fun KpiPointRuleResponse.toEntity() = KpiPointRuleEntity(
    ruleCode = this.rule_code,
    pointValue = this.point_value,
    description = this.description
)