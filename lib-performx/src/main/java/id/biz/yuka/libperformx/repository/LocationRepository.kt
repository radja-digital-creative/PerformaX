package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.UserLocationDetailEntity
import id.biz.yuka.libperformx.request.RecordUserLocationRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.time.OffsetDateTime

class LocationRepository {

    suspend fun recordUserLocation(
        latitude: Double,
        longitude: Double,
        provider: String = "hybrid",
        recordedAt: String = OffsetDateTime.now().toString(),
        expectedUserId: String? = null
    ): ResultState<Unit> = locationCall("Gagal memperbarui lokasi. Silakan coba lagi.") {
        val sessionReady = withTimeoutOrNull(5_000L) {
            SupabaseConfig.client.auth.sessionStatus.first { status ->
                status !is SessionStatus.LoadingFromStorage
            }
        }
        check(sessionReady != null) { "Sesi login belum tersedia." }
        val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
        check(!userId.isNullOrEmpty()) { "Sesi login belum tersedia." }
        check(expectedUserId == null || expectedUserId == userId) { "Sesi login telah berubah." }

        val request = RecordUserLocationRequest(
            userId = userId,
            latitude = latitude,
            longitude = longitude,
            provider = provider,
            recordedAt = recordedAt
        )
        // Keep the captured identity in the payload; an account switch must never
        // attribute this fix to a different user. The existing insert RLS still applies.
        SupabaseConfig.client.postgrest["user_locations"].insert(request)
        Unit
    }

    /** Latest visible location for every active non-owner profile, independent of history volume. */
    suspend fun getLatestUserLocations(): ResultState<List<UserLocationDetailEntity>> =
        locationCall("Gagal memuat lokasi karyawan. Silakan coba lagi.") {
            val ownerRole = SupabaseConfig.client.postgrest["roles"]
                .select(columns = Columns.raw("id")) {
                    filter { eq("role_name", "owner") }
                }
                .decodeSingleOrNull<RoleRawForLocation>()
                ?: error("Role owner tidak tersedia.")

            val profiles = mutableListOf<UserProfileRawForLocation>()
            var lastProfileId: String? = null
            // Keyset pagination also works when PostgREST caps responses below our
            // requested page size. Continue until an empty page, not a short page.
            while (true) {
                val cursor = lastProfileId
                val page = SupabaseConfig.client.postgrest["profiles"]
                    .select(columns = Columns.raw("id,full_name,phone_number,profile_url")) {
                        filter {
                            neq("role_id", ownerRole.id)
                            eq("is_active", true)
                            if (cursor != null) gt("id", cursor)
                        }
                        order("id", order = Order.ASCENDING)
                        limit(500)
                    }
                    .decodeList<UserProfileRawForLocation>()
                if (page.isEmpty()) break
                profiles.addAll(page)
                lastProfileId = page.last().id
            }

            val requests = Semaphore(3)
            coroutineScope {
                profiles.map { profile ->
                    async {
                        requests.withPermit {
                            // Query each user's newest row directly. A frequently
                            // reporting device cannot crowd quieter users out.
                            val location = SupabaseConfig.client.postgrest["user_locations"]
                                .select(columns = Columns.raw("user_id,latitude,longitude,recorded_at")) {
                                    filter { eq("user_id", profile.id) }
                                    order("recorded_at", order = Order.DESCENDING)
                                    limit(1)
                                }
                                .decodeSingleOrNull<UserLocationRawResponse>()
                            location?.let {
                                UserLocationDetailEntity(
                                    userId = it.userId,
                                    fullName = profile.fullName ?: "Karyawan",
                                    phoneNumber = profile.phoneNumber ?: "-",
                                    profileUrl = profile.profileUrl ?: "",
                                    latitude = it.latitude,
                                    longitude = it.longitude,
                                    recordedAt = it.recordedAt ?: ""
                                )
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }

    // Do not log transport exceptions: they may include coordinates or auth headers.
    // A failed per-user query fails the entire refresh, preserving the caller's last
    // complete marker set instead of silently presenting a partial success.
    private suspend fun <T> locationCall(message: String, block: suspend () -> T): ResultState<T> =
        try {
            ResultState.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultState.Error(message)
        }
}

@kotlinx.serialization.Serializable
private data class RoleRawForLocation(
    @kotlinx.serialization.SerialName("id") val id: Int
)

@kotlinx.serialization.Serializable
private data class UserLocationRawResponse(
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("latitude") val latitude: Double,
    @kotlinx.serialization.SerialName("longitude") val longitude: Double,
    @kotlinx.serialization.SerialName("recorded_at") val recordedAt: String? = null
)

@kotlinx.serialization.Serializable
private data class UserProfileRawForLocation(
    @kotlinx.serialization.SerialName("id") val id: String,
    @kotlinx.serialization.SerialName("full_name") val fullName: String? = null,
    @kotlinx.serialization.SerialName("phone_number") val phoneNumber: String? = null,
    @kotlinx.serialization.SerialName("profile_url") val profileUrl: String? = null
)
