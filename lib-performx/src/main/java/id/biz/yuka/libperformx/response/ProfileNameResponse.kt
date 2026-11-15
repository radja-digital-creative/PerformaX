package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class ProfileNameResponse(
    val id: String,
    val full_name: String
)
