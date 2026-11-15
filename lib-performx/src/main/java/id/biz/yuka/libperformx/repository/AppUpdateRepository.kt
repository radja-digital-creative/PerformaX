package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.AppVersionEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.CreateAppVersionRequest
import id.biz.yuka.libperformx.response.AppVersionResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header

class AppUpdateRepository {

    private val bucketName = "apk_releases"
    private val tableName = "app_versions"

    /** 1. CEK VERSI TERBARU (Mengecek versi aplikasi di DB) */
    suspend fun getLatestAppVersion(): ResultState<AppVersionEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest[tableName]
                .select {
                    order("version_code", order = Order.DESCENDING)
                    limit(1) // Ambil 1 data teratas (paling baru)
                }
                .decodeSingleOrNull<AppVersionResponse>()

            response?.toEntity() ?: throw Exception("Belum ada rilis versi aplikasi di database.")
        }
    }

    /** 2. UPLOAD APK KE STORAGE & SIMPAN DATA KE DATABASE */
    suspend fun uploadNewRelease(
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        fileName: String,
        apkByteArray: ByteArray
    ): ResultState<AppVersionEntity> {
        return safeSupabaseCall {
            // A. Upload File APK ke Storage
            val filePath = "$versionCode/$fileName"
            SupabaseConfig.client.storage.from(bucketName).upload(
                path = filePath,
                data = apkByteArray,
                upsert = true
            )

            // B. Dapatkan Public URL dari APK yang baru diupload
            val publicUrl = SupabaseConfig.client.storage.from(bucketName).publicUrl(filePath)

            // C. Simpan data rilis ke tabel Database
            val request = CreateAppVersionRequest(
                version_code = versionCode,
                version_name = versionName,
                release_notes = releaseNotes,
                apk_url = publicUrl
            )

            val dbResponse = SupabaseConfig.client.postgrest[tableName]
                .insert(request) { select() }
                .decodeSingle<AppVersionResponse>()

            dbResponse.toEntity()
        }
    }

    /** 3. DOWNLOAD APK DARI STORAGE (Untuk proses pembaruan) */
    suspend fun downloadApk(apkUrl: String): ResultState<ByteArray> {
        return safeSupabaseCall {
            // Buat client baru yang 'bersih' khusus untuk download file besar
            val client = io.ktor.client.HttpClient {
                install(HttpTimeout)
            }

            try {
                val response = client.get(apkUrl) {
                    // Beri waktu 10 menit untuk download file besar
                    timeout { 
                        requestTimeoutMillis = 600_000 
                        connectTimeoutMillis = 30_000
                    }
                    // Identitas sebagai mobile browser agar tidak diblokir Cloudflare
                    header("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:114.0) Gecko/114.0 Firefox/114.0")
                }
                response.body<ByteArray>()
            } finally {
                client.close() // Tutup client setelah selesai agar tidak memory leak
            }
        }
    }
}