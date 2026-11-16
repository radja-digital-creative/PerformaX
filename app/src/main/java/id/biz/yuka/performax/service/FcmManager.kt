package id.biz.yuka.performax.service // Sesuaikan dengan package Komandan

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import id.biz.yuka.libperformx.SupabaseConfig
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FcmManager {

    private val fcm = FirebaseMessaging.getInstance()
    private val tableName = "user_fcm_tokens"

    // 1. SINKRONISASI TOKEN SAAT LOGIN / MASUK HOME
    suspend fun syncTokenToSupabase() {
        withContext(Dispatchers.IO) {
            try {
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@withContext

                // Ambil token dari Google Firebase
                val token = fcm.token.await()

                // Siapkan data untuk di-upsert
                val tokenData = mapOf(
                    "user_id" to userId,
                    "token" to token,
                    "updated_at" to java.time.Instant.now().toString()
                )

                // Simpan ke Supabase
                SupabaseConfig.client.from(tableName).upsert(tokenData)
                Log.d("FCM_MANAGER", "Token berhasil disinkronisasi")

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FCM_MANAGER", "Gagal sinkronisasi token: ${e.message}")
            }
        }
    }

    // 2. HAPUS TOKEN SAAT LOGOUT (SANGAT PENTING!)
    suspend fun clearTokenOnLogout() {
        withContext(Dispatchers.IO) {
            try {
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@withContext

                // Hapus data token dari Supabase
                SupabaseConfig.client.from(tableName).delete {
                    filter { eq("user_id", userId) }
                }

                // (Opsional) Hapus token dari perangkat agar Firebase membuat token baru untuk user berikutnya
                fcm.deleteToken().await()
                Log.d("FCM_MANAGER", "Token berhasil dihapus saat logout")

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FCM_MANAGER", "Gagal hapus token: ${e.message}")
            }
        }
    }

    // 3. SUBSCRIBE TOPIK (Contoh: "store_jakarta", "managers", dll)
    suspend fun subscribeToTopic(topic: String) {
        try {
            fcm.subscribeToTopic(topic).await()
            Log.d("FCM_MANAGER", "Berhasil subscribe ke topik: $topic")
        } catch (e: Exception) {
            Log.e("FCM_MANAGER", "Gagal subscribe topik: ${e.message}")
        }
    }

    // 4. UNSUBSCRIBE TOPIK
    suspend fun unsubscribeFromTopic(topic: String) {
        try {
            fcm.unsubscribeFromTopic(topic).await()
            Log.d("FCM_MANAGER", "Berhasil unsubscribe dari topik: $topic")
        } catch (e: Exception) {
            Log.e("FCM_MANAGER", "Gagal unsubscribe topik: ${e.message}")
        }
    }
}