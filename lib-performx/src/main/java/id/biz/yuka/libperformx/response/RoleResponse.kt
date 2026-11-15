package id.biz.yuka.libperformx.response
import kotlinx.serialization.Serializable

@Serializable
data class RoleResponse(val id: Int, val role_name: String)