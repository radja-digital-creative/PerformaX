package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.UserEntity
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.response.ProfileRoleResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

class AuthRepository {


    /**
     * Mengambil User ID yang sedang aktif / login saat ini.
     * Fungsi ini menjaga agar SupabaseConfig tidak bocor ke ViewModel.
     */
    fun getCurrentUserId(): String? {
        return SupabaseConfig.client.auth.currentUserOrNull()?.id
    }

    /**
     * Mengambil Role Name dari user yang sedang aktif berdasarkan tabel profiles & roles
     */
    suspend fun getUserRole(userId: String): ResultState<String> {
        return safeSupabaseCall {
            // Query dengan join implicit ke tabel roles untuk mengambil nama role
            val response = SupabaseConfig.client.postgrest["profiles"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("roles(role_name)")) {
                    filter { eq("id", userId) }
                }.decodeSingle<ProfileRoleResponse>()

            response.roles?.role_name?.lowercase() ?: "employee"
        }
    }

    /**
     * Mencari UUID User berdasarkan Email (Khusus digunakan oleh HR/Admin saat Registrasi)
     */
    suspend fun getUserIdByEmail(targetEmail: String): ResultState<String> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest.rpc(
                function = "get_user_id_by_email",
                parameters = mapOf("p_email" to targetEmail) // Kirim email sebagai parameter
            ).decodeAs<String>()
        }
    }

    suspend fun login(request: LoginRequest): ResultState<UserEntity> {
        return safeSupabaseCall {
            // ✅ Pastikan sesi lama benar-benar bersih sebelum login baru
            try {
                SupabaseConfig.client.auth.signOut(scope = io.github.jan.supabase.gotrue.SignOutScope.LOCAL)
            } catch (e: Exception) {
                // Abaikan jika gagal logout (misal memang belum login)
            }

            // 1. Eksekusi Login
            SupabaseConfig.client.auth.signInWith(Email) {
                email = request.email
                password = request.password
            }

            // 2. Ambil Sesi
            val session = SupabaseConfig.client.auth.currentSessionOrNull()
            val user = SupabaseConfig.client.auth.currentUserOrNull()

            // 3. Validasi & Mapping
            if (session != null && user != null && user.email != null) {
                user.toUserEntity(session) // Otomatis mengembalikan Entity jika sukses
            } else {
                // Lempar Exception agar ditangkap oleh safeSupabaseCall
                throw Exception("Login berhasil, tetapi gagal mendapatkan profil user.")
            }
        }
    }

    private fun UserInfo.toUserEntity(session: UserSession): UserEntity {
        return UserEntity(
            userId = this.id,
            email = this.email!!,
            accessToken = session.accessToken,
            role = "employee"
        )
    }
}