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
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import kotlinx.coroutines.ensureActive
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

    /** Stream to disk: avoids keeping an entire APK in memory on slow connections. */
    suspend fun downloadApk(apkUrl: String, destination: java.io.File,
                            onProgress: suspend (Long, Long) -> Unit): ResultState<java.io.File> = safeSupabaseCall {
        require(java.net.URI(apkUrl).scheme == "https") { "Alamat pembaruan harus HTTPS." }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val client = io.ktor.client.HttpClient {
                expectSuccess = true
                install(HttpTimeout)
            }
            try {
                client.prepareGet(apkUrl) {
                    timeout {
                        requestTimeoutMillis = 600_000
                        connectTimeoutMillis = 15_000
                        socketTimeoutMillis = 30_000
                    }
                }.execute { response ->
                    val expected = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                    var received = 0L
                    var lastReported = 0L
                    destination.outputStream().buffered().use { output ->
                        val channel = response.bodyAsChannel()
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            val count = channel.readAvailable(buffer, 0, buffer.size)
                            if (count == -1) break
                            if (count == 0) continue
                            output.write(buffer, 0, count)
                            received += count
                            val now = System.nanoTime()
                            if (now - lastReported > 200_000_000L) {
                                onProgress(received, expected)
                                lastReported = now
                            }
                        }
                    }
                    require(received > 0 && (expected < 0 || received == expected)) { "Unduhan belum lengkap. Silakan coba lagi." }
                    onProgress(received, expected)
                }
                destination
            } catch (e: Exception) {
                destination.delete()
                throw e
            } finally { client.close() }
        }
    }
}
