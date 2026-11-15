package id.biz.yuka.libperformx.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateResignationRequest(
    @SerialName("user_id")
    val user_id: String? = null,
    @SerialName("reason")
    val reason: String,
    @SerialName("proposed_last_day")
    val proposed_last_day: String
)
