package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.UserProfileResponse


data class UserProfileEntity(
    val id: String,
    val fullName: String,
    val role: String,
    val faceEmbedding: String,
    val stores: List<StoreEntity>,
    val hasGlobalAccess: Boolean,
    val canRequestLoan: Boolean
)

fun UserProfileResponse.toEntity(): UserProfileEntity {
    val currentRole = this.role_name ?: "employee"
    val isGlobalAdmin = currentRole.lowercase() == "owner" || currentRole.lowercase() == "admin"

    return UserProfileEntity(
        id = this.user_id,
        fullName = this.full_name,
        role = currentRole,
        faceEmbedding = this.face_embedding ?: "",
        hasGlobalAccess = isGlobalAdmin,
        canRequestLoan = this.can_request_loan ?: false,
        stores = this.assigned_stores.map { StoreEntity(it.id, it.name) }
    )
}