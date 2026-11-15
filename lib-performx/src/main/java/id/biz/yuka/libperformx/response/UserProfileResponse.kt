package id.biz.yuka.libperformx.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    @SerialName("id")
    val user_id: String = "",
    val full_name: String,
    val gender: String? = null,
    val phone_number: String? = null,
    val address: String? = null,
    val id_card_url: String? = null,
    val face_reference_url: String? = null,
    @SerialName("is_active")
    val profile_active: Boolean = true,
    val joined_at: String? = null,
    val face_embedding: String? = null,
    val role_name: String? = null,
    val role_description: String? = null,
    val assigned_stores: List<StoreResponse> = emptyList(), // Array JSON yang aman
    val can_request_loan: Boolean? = false,
    val salary_id: String? = null,
    val basic_salary: Double? = null,
    val transport_allowance: Double? = null,
    val meal_allowance: Double? = null,
    val position_allowance: Double? = null,
    val active_deductions: List<ActiveDeductionResponse> = emptyList()

)