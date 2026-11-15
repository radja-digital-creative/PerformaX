package id.biz.yuka.libperformx.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreResponse(
    val id: String,
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("is_active")
    val is_active: Boolean = true
)

@Serializable
data class UserStoreJoinResponse(
    val stores: StoreResponse
)

// Response untuk menangkap View vw_store_details
@Serializable
data class StoreDetailResponse(
    val store_id: String,
    val store_name: String,
    val address: String? = null,
    val phone_number: String? = null,
    val logo_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_active: Boolean,

    val setting_id: String? = null,
    val open_before_minutes: Int? = null,
    val late_tolerance_minutes: Int? = null,
    val overtime_threshold_minutes: Int? = null,
    val daily_close_time: String? = null
)