package id.biz.yuka.libperformx.repository
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.RoleEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.response.RoleResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest

class RoleRepository {
    suspend fun getAllRoles(): ResultState<List<RoleEntity>> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["roles"].select().decodeList<RoleResponse>().map { it.toEntity() }
    }
}