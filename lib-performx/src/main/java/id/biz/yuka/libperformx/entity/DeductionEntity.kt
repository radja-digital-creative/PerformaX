package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.DeductionResponse

data class DeductionEntity(
    val id: String,
    val name: String,
    val description: String?,
    val calculationType: String, // 'fixed' atau 'percentage'
    val fixedAmount: Double,
    val percentageValue: Double,
    val isActive: Boolean
)

fun DeductionResponse.toEntity() = DeductionEntity(
    id = this.id,
    name = this.name,
    description = this.description,
    calculationType = this.calculation_type,
    fixedAmount = this.fixed_amount ?: 0.0,
    percentageValue = this.percentage_value ?: 0.0,
    isActive = this.is_active ?: true
)