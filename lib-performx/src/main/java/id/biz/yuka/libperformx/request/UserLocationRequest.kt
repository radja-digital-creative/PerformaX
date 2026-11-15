package id.biz.yuka.libperformx.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecordUserLocationRequest(
    @SerialName("user_id")
    val userId: String,
    @SerialName("latitude")
    val latitude: Double,
    @SerialName("longitude")
    val longitude: Double,
    @SerialName("provider")
    val provider: String,
    @SerialName("recorded_at")
    val recordedAt: String
)
