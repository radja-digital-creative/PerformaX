package id.biz.yuka.libperformx.utils

import android.util.Log
import id.biz.yuka.libperformx.SupabaseConfig
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

@Serializable
private data class UserFcmTokenRecord(
    @SerialName("user_id") val userId: String
)

/**
 * Helper class untuk mengirim permintaan pembaruan lokasi (FCM Location Ping)
 * ke karyawan tertentu atau seluruh karyawan via Edge Function `send-fcm`.
 */
class LocationRefreshHelper {

    /**
     * Minta pembaruan lokasi secara spesifik untuk SATU karyawan.
     */
    suspend fun requestSingleLocation(userId: String): ResultState<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = buildJsonObject {
                    putJsonObject("record") {
                        put("user_id", userId)
                        put("isDataOnly", true)
                        put("title", "Ping Lokasi Real-Time")
                        put("message", "Meminta pembaruan lokasi presisi...")
                        putJsonObject("payload") {
                            put("id", userId)
                            put("type", "REQUEST_LOCATION")
                            put("silent", "true")
                        }
                    }
                }

                SupabaseConfig.client.functions.invoke(
                    function = "send-fcm",
                    body = payload
                )

                Log.d("LocationRefreshHelper", "Berhasil kirim request lokasi ke user $userId")
                ResultState.Success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LocationRefreshHelper", "Gagal requestSingleLocation: ${e.message}", e)
                ResultState.Error(e.message ?: "Gagal mengirim permintaan lokasi ke karyawan.")
            }
        }
    }

    /**
     * Minta pembaruan lokasi ke SELURUH karyawan yang memiliki FCM token terdaftar.
     * @return ResultState berisi jumlah karyawan yang berhasil dikirimi permintaan.
     */
    suspend fun requestAllLocations(): ResultState<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Ambil daftar user_id unik dari user_fcm_tokens
                val tokensData = SupabaseConfig.client.postgrest["user_fcm_tokens"]
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("user_id"))
                    .decodeList<UserFcmTokenRecord>()

                val userIds = tokensData.map { it.userId }.distinct()

                if (userIds.isEmpty()) {
                    return@withContext ResultState.Error("Tidak ada karyawan dengan token FCM terdaftar.")
                }

                var successCount = 0
                for (userId in userIds) {
                    val res = requestSingleLocation(userId)
                    if (res is ResultState.Success) {
                        successCount++
                    }
                }

                Log.d("LocationRefreshHelper", "Ping lokasi dikirim ke $successCount dari ${userIds.size} user.")
                ResultState.Success(successCount)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LocationRefreshHelper", "Gagal requestAllLocations: ${e.message}", e)
                ResultState.Error(e.message ?: "Gagal memperbarui lokasi karyawan.")
            }
        }
    }
}
