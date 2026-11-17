package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.UserProfileEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.response.UserProfileResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.functions.functions
import io.ktor.util.InternalAPI
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class UserRepository {

    suspend fun getUserProfile(userId: String): ResultState<UserProfileEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["vw_employee_details"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfileResponse>()

            if (response != null) {
                if (!response.profile_active) {
                    throw Exception("Akun Anda telah dinonaktifkan.")
                }
                response.toEntity() // Return Entity
            } else {
                throw Exception("Data profil tidak ditemukan di server.")
            }
        }
    }

    @OptIn(InternalAPI::class)
    suspend fun updateEmployeeRole(userId: String, roleId: Int): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.functions.invoke("update_user_admin") {
                body = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_role_id", roleId)
                }.toString()
                headers.append("Content-Type", "application/json")
            }
            Unit
        }
    }
}
