package id.biz.yuka.libperformx.repository

import android.util.Log
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.request.RecordUserLocationRequest
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.OffsetDateTime

class LocationRepository {

    suspend fun recordUserLocation(
        latitude: Double,
        longitude: Double,
        provider: String = "hybrid"
    ): ResultState<Unit> {
        return safeSupabaseCall {
            // Tunggu sampai auth session selesai di-load dari storage (max 5 detik)
            val sessionReady = withTimeoutOrNull(5_000L) {
                SupabaseConfig.client.auth.sessionStatus.first { status ->
                    status !is SessionStatus.LoadingFromStorage
                }
            }
            Log.d("LOCATION_REPO", "Session status setelah wait: $sessionReady")

            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            Log.d("LOCATION_REPO", "Memproses simpan lokasi. User ID: $userId")

            if (userId.isNullOrEmpty()) {
                Log.e("LOCATION_REPO", "User ID tidak ditemukan (null), tidak dapat menyimpan lokasi ke Supabase.")
                error("Sesi login belum tersedia untuk memperbarui lokasi.")
            }

            val request = RecordUserLocationRequest(
                userId = userId,
                latitude = latitude,
                longitude = longitude,
                provider = provider,
                recordedAt = OffsetDateTime.now().toString()
            )

            Log.d("LOCATION_REPO", "Mengirim payload ke Supabase: $request")
            SupabaseConfig.client.postgrest["user_locations"].insert(request)
            Log.d("LOCATION_REPO", "Berhasil insert lokasi ke tabel user_locations!")
            Unit
        }
    }

    /**
     * Mengambil lokasi TERBARU dari seluruh karyawan beserta data profilnya.
     */
    suspend fun getLatestUserLocations(): ResultState<List<id.biz.yuka.libperformx.entity.UserLocationDetailEntity>> {
        return safeSupabaseCall {
            // 1. Ambil seluruh data lokasi, diurutkan terbaru dahulu
            val rawLocations = SupabaseConfig.client.postgrest["user_locations"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("user_id,latitude,longitude,recorded_at")) {
                    order("recorded_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<UserLocationRawResponse>()

            if (rawLocations.isEmpty()) {
                return@safeSupabaseCall emptyList()
            }

            // 2. Ambil record TERBARU saja untuk setiap user_id
            val latestLocationsByUser = mutableMapOf<String, UserLocationRawResponse>()
            for (loc in rawLocations) {
                if (!latestLocationsByUser.containsKey(loc.userId)) {
                    latestLocationsByUser[loc.userId] = loc
                }
            }

            val userIds = latestLocationsByUser.keys.toList()
            if (userIds.isEmpty()) {
                return@safeSupabaseCall emptyList()
            }

            // 3. Ambil role_id untuk 'owner' agar bisa di-exclude
            val ownerRole = try {
                SupabaseConfig.client.postgrest["roles"]
                    .select { filter { eq("role_name", "owner") } }
                    .decodeSingleOrNull<RoleRawForLocation>()
            } catch (e: Exception) { null }

            // Ambil data profil karyawan (exclude owner)
            val profiles = SupabaseConfig.client.postgrest["profiles"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("id,role_id,full_name,phone_number,profile_url")) {
                    filter {
                        isIn("id", userIds)
                        if (ownerRole != null) {
                            neq("role_id", ownerRole.id)
                        }
                        eq("is_active", true)
                    }
                }
                .decodeList<UserProfileRawForLocation>()

            val profileMap = profiles.associateBy { it.id }

            // 4. Susun daftar UserLocationDetailEntity (hanya karyawan aktif non-owner)
            latestLocationsByUser.values
                .filter { loc -> profileMap.containsKey(loc.userId) }
                .map { loc ->
                    val profile = profileMap[loc.userId]!!
                    id.biz.yuka.libperformx.entity.UserLocationDetailEntity(
                        userId = loc.userId,
                        fullName = profile.fullName ?: "Karyawan",
                        phoneNumber = profile.phoneNumber ?: "-",
                        profileUrl = profile.profileUrl ?: "",
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        recordedAt = loc.recordedAt ?: ""
                    )
                }
        }
    }
}

@kotlinx.serialization.Serializable
private data class RoleRawForLocation(
    @kotlinx.serialization.SerialName("id") val id: Int,
    @kotlinx.serialization.SerialName("role_name") val roleName: String
)

@kotlinx.serialization.Serializable
private data class UserLocationRawResponse(
    @kotlinx.serialization.SerialName("id") val id: String = "",
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("latitude") val latitude: Double,
    @kotlinx.serialization.SerialName("longitude") val longitude: Double,
    @kotlinx.serialization.SerialName("recorded_at") val recordedAt: String? = null
)

@kotlinx.serialization.Serializable
private data class UserProfileRawForLocation(
    @kotlinx.serialization.SerialName("id") val id: String,
    @kotlinx.serialization.SerialName("role_id") val roleId: Int? = null,
    @kotlinx.serialization.SerialName("full_name") val fullName: String? = null,
    @kotlinx.serialization.SerialName("phone_number") val phoneNumber: String? = null,
    @kotlinx.serialization.SerialName("profile_url") val profileUrl: String? = null
)


