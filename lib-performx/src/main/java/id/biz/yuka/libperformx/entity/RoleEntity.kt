package id.biz.yuka.libperformx.entity
import id.biz.yuka.libperformx.response.RoleResponse

data class RoleEntity(val id: Int, val roleName: String)
fun RoleResponse.toEntity() = RoleEntity(id, role_name)