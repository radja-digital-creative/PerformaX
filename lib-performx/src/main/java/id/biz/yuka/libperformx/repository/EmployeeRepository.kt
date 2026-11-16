package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.RegisterEmployeeRequest
import id.biz.yuka.libperformx.request.SoftDeleteEmployeeRequest
import id.biz.yuka.libperformx.request.UpdateEmployeeRequest
import id.biz.yuka.libperformx.request.UpdateKycRequest
import id.biz.yuka.libperformx.response.EmployeeResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import io.ktor.util.InternalAPI
import kotlinx.serialization.encodeToString
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json

class EmployeeRepository {

    suspend fun getAttendancePreferences(userId: String): ResultState<id.biz.yuka.libperformx.entity.EmployeeAttendancePreferences> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_attendance_preferences"]
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<id.biz.yuka.libperformx.entity.EmployeeAttendancePreferences>()
            ?: id.biz.yuka.libperformx.entity.EmployeeAttendancePreferences(user_id = userId)
    }

    suspend fun saveAttendancePreferences(settings: id.biz.yuka.libperformx.entity.EmployeeAttendancePreferences): ResultState<Unit> = safeSupabaseCall {
        val payload = Json { encodeDefaults = true }.encodeToJsonElement(
            id.biz.yuka.libperformx.entity.EmployeeAttendancePreferences.serializer(), settings
        )
        SupabaseConfig.client.postgrest["employee_attendance_preferences"].upsert(payload)
        Unit
    }

    /** READ ALL: Mendapatkan semua karyawan aktif */
    suspend fun getAllEmployees(storeId: String? = null): ResultState<List<EmployeeEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["vw_employee_details"]
                .select { filter { eq("profile_active", true) } }
                .decodeList<EmployeeResponse>()

            val mappedList = responseList.map { it.toEntity() }

            // Filter manual untuk toko (karena toko berbentuk Array JSON di view)
            if (storeId != null) {
                mappedList.filter { employee -> employee.stores.any { it.id == storeId } }
            } else {
                mappedList
            }
        }
    }

    /** MENGAMBIL KARYAWAN BERDASARKAN DAFTAR TOKO */
    suspend fun getEmployeesByStores(storeIds: List<String>): ResultState<List<EmployeeEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["vw_employee_details"]
                .select { filter { eq("profile_active", true) } }
                .decodeList<EmployeeResponse>()

            val mappedList = responseList.map { it.toEntity() }

            mappedList.filter { employee ->
                employee.stores.any { it.id in storeIds }
            }
        }
    }

    /** READ DETAIL: Mendapatkan spesifik 1 karyawan */
    suspend fun getEmployeeById(userId: String): ResultState<EmployeeEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["vw_employee_details"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingle<EmployeeResponse>()
            response.toEntity()
        }
    }

    /** CREATE (REGISTER): Menggunakan Edge Function (Email & PW) -> RPC */
    @OptIn(InternalAPI::class)
    suspend fun createEmployee(request: RegisterEmployeeRequest): ResultState<Boolean> {
        return safeSupabaseCall {
            val payload = Json.encodeToJsonElement(RegisterEmployeeRequest.serializer(), request).jsonObject
            accountAction(kotlinx.serialization.json.JsonObject(payload + ("action" to kotlinx.serialization.json.JsonPrimitive("create"))))
            true
        }
    }

    /** UPDATE: Menggunakan RPC agar Atomic/Aman */
    @OptIn(InternalAPI::class)
    suspend fun updateEmployee(request: UpdateEmployeeRequest): ResultState<Boolean> {
        return safeSupabaseCall {
            // Memanggil Edge Function 'update_user_admin'
            SupabaseConfig.client.functions.invoke("update_user_admin") {
                body = Json.encodeToString(request)
                headers.append("Content-Type", "application/json")
            }
            true // Jika lolos tanpa error, berarti fungsi sukses dieksekusi
        }
    }

    /** Account actions are authorized by the server, never by caller-supplied role. */
    @OptIn(InternalAPI::class)
    private suspend fun accountAction(payload: kotlinx.serialization.json.JsonObject): kotlinx.serialization.json.JsonObject {
        val response = SupabaseConfig.client.functions.invoke("employee-account") {
            body = payload.toString()
            headers.append("Content-Type", "application/json")
        }
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    suspend fun getEmployeeAccountEmail(userId: String): ResultState<String> = safeSupabaseCall {
        val result = accountAction(kotlinx.serialization.json.buildJsonObject {
            put("action", kotlinx.serialization.json.JsonPrimitive("get_email"))
            put("user_id", kotlinx.serialization.json.JsonPrimitive(userId))
        })
        result["email"]?.jsonPrimitive?.content ?: error("Email akun tidak tersedia.")
    }

    suspend fun updateEmployeeCredentials(userId: String, email: String?, password: String?): ResultState<Unit> = safeSupabaseCall {
        accountAction(kotlinx.serialization.json.buildJsonObject {
            put("action", kotlinx.serialization.json.JsonPrimitive("update_credentials"))
            put("user_id", kotlinx.serialization.json.JsonPrimitive(userId))
            email?.let { put("email", kotlinx.serialization.json.JsonPrimitive(it)) }
            password?.let { put("password", kotlinx.serialization.json.JsonPrimitive(it)) }
        })
        Unit
    }

    suspend fun deleteEmployee(userId: String): ResultState<Unit> = safeSupabaseCall {
        accountAction(kotlinx.serialization.json.buildJsonObject {
            put("action", kotlinx.serialization.json.JsonPrimitive("disable"))
            put("user_id", kotlinx.serialization.json.JsonPrimitive(userId.trim()))
        })
        Unit
    }

    suspend fun uploadKtpImage(userId: String, imageBytes: ByteArray): ResultState<String> {
        return safeSupabaseCall {
            val fileName = "ktp_$userId.jpg"
            val bucket = SupabaseConfig.client.storage.from("profiles")

            // Upload gambar
            bucket.upload(fileName, imageBytes, upsert = true)

            // Dapatkan URL Publik
            bucket.publicUrl(fileName)
        }
    }

    // Contoh penambahan di EmployeeRepository.kt
    suspend fun updateEmployeeKyc(userId: String, request: UpdateKycRequest): ResultState<Boolean> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["profiles"]
                .update(request) { filter { eq("id", userId) } }
            true
        }
    }

    suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): ResultState<String> {
        return try {
            // Menggunakan bucket "profiles" sesuai yang Komandan setel sebelumnya
            val bucket = SupabaseConfig.client.storage.from("profiles")

            // Penamaan file khusus untuk foto profil
            val fileName = "avatar_$userId.jpg"

            // Proses upload dengan upsert = true (akan menimpa file lama jika user ganti foto)
            bucket.upload(
                path = fileName,
                data = imageBytes,
                upsert = true
            )

            // Mendapatkan URL publik agar bisa ditampilkan di aplikasi
            val publicUrl = bucket.publicUrl(fileName)

            ResultState.Success(publicUrl)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Gagal mengunggah foto profil")
        }
    }

    @kotlinx.serialization.Serializable
    private data class UpdateLoanEligibilityRequest(
        val can_request_loan: Boolean
    )

    /** UPDATE LOAN ELIGIBILITY: Mengubah status can_request_loan karyawan */
    suspend fun updateCanRequestLoan(userId: String, canRequestLoan: Boolean): ResultState<Boolean> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["profiles"]
                .update(UpdateLoanEligibilityRequest(canRequestLoan)) {
                    filter { eq("id", userId) }
                }
            true
        }
    }
}