package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.UserProfileEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.response.UserProfileResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest

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

    suspend fun updateEmployeeRole(userId: String, roleId: Int): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["profiles"].update(
                mapOf("role_id" to roleId)
            ) {
                filter {
                    eq("id", userId)
                }
            }
            Unit
        }
    }
}