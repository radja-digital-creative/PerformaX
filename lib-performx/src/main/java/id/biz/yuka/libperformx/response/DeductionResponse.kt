package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class DeductionResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val calculation_type: String,
    val fixed_amount: Double? = 0.0,
    val percentage_value: Double? = 0.0,
    val is_active: Boolean? = true
)