package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateDeductionRequest(
    val name: String,
    val description: String? = null,
    val calculation_type: String, // 'fixed' atau 'percentage'
    val fixed_amount: Double? = 0.0,
    val percentage_value: Double? = 0.0
)

@Serializable
data class UpdateDeductionRequest(
    val name: String? = null,
    val description: String? = null,
    val calculation_type: String? = null,
    val fixed_amount: Double? = null,
    val percentage_value: Double? = null,
    val is_active: Boolean? = null
)