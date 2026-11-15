package id.biz.yuka.libperformx.repository

import android.util.Log
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.*
import id.biz.yuka.libperformx.request.*
import id.biz.yuka.libperformx.response.*
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import java.util.UUID

class AppealRepository {

    suspend fun hasActiveAppeal(employeeId: String): ResultState<Boolean> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["employee_violation_appeals"]
            .select {
                filter {
                    eq("employee_id", employeeId)
                    eq("status", "ACTIVE")
                }
            }
            .decodeList<AppealResponse>()

        response.isNotEmpty()
    }

    // Fungsi 1: Untuk Owner mengambil semua banding yang aktif beserta nama
    suspend fun getAllActiveAppeals(): ResultState<List<AppealEntity>> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["employee_violation_appeals"]
            .select(columns = Columns.raw("*, profiles(id,full_name)")) {
                filter { eq("status", "ACTIVE") }
            }
            .decodeList<AppealResponse>()

        response.map { it.toEntity() }
    }

    // Fungsi 2: Untuk Karyawan mengambil banding miliknya sendiri beserta nama
    suspend fun getActiveAppealByUser(employeeId: String): ResultState<AppealEntity?> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["employee_violation_appeals"]
            .select(columns = Columns.raw("*, profiles(id,full_name)")) {
                filter {
                    eq("status", "ACTIVE")
                    eq("employee_id", employeeId)
                }
            }
            .decodeList<AppealResponse>()

        response.firstOrNull()?.toEntity()
    }

    suspend fun createAppeal(request: CreateAppealRequest): ResultState<AppealEntity> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["employee_violation_appeals"]
            .insert(request) {
                select()
            }
            .decodeSingle<AppealResponse>()

        response.toEntity()
    }

    // ===============================================
    // PERBAIKAN: AMBIL KOMENTAR + RELASIKAN MEDIA
    // ===============================================
    suspend fun getAppealComments(appealId: String): ResultState<List<AppealCommentEntity>> = safeSupabaseCall {
        // 1. Ambil semua komentar
        val comments = SupabaseConfig.client.postgrest["employee_violation_appeals_comment"]
            .select {
                filter { eq("appeal_id", appealId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<AppealCommentResponse>()

        // 2. Ambil semua media untuk appeal ini
        val medias = SupabaseConfig.client.postgrest["employee_violation_appeals_media"]
            .select {
                filter { eq("appeal_id", appealId) }
            }
            .decodeList<AppealMediaResponse>()

        // 3. Gabungkan URL media ke dalam Entity Komentar yang cocok
        comments.map { commentResponse ->
            val entity = commentResponse.toEntity()
            val matchedMediaUrls = medias.filter { it.comment_id == entity.id }.map { it.file_url }
            entity.images = matchedMediaUrls
            entity
        }
    }

    suspend fun addComment(request: CreateAppealCommentRequest): ResultState<Unit> {
        // 1. Log sebelum eksekusi (menampilkan isi request)
        Log.d("AppealRepository", "Mencoba insert komentar teks...")
        Log.d("AppealRepository", "-> Appeal ID: ${request.appeal_id}")
        Log.d("AppealRepository", "-> Sender: ${request.sender_type} (${request.sender_id})")
        Log.d("AppealRepository", "-> Pesan: ${request.message}")

        val result = safeSupabaseCall {
            SupabaseConfig.client.postgrest["employee_violation_appeals_comment"].insert(request)
            Unit
        }

        // 2. Log setelah menerima respon dari Supabase
        when (result) {
            is ResultState.Success -> {
                Log.d("AppealRepository", "✅ Berhasil insert komentar teks ke database!")
            }
            is ResultState.Error -> {
                Log.e("AppealRepository", "❌ GAGAL insert komentar teks: ${result.message}")
            }
            else -> {
                Log.d("AppealRepository", "Status tidak diketahui: $result")
            }
        }

        return result
    }

    // ===============================================
    // PERBAIKAN: 1 HIT (KOMENTAR -> STORAGE -> MEDIA)
    // ===============================================
    suspend fun addCommentWithImages(
        appealId: String,
        senderId: String?,
        senderType: String,
        message: String,
        imageBytesList: List<ByteArray>
    ): ResultState<Unit> = safeSupabaseCall {
        val tag = "AppealRepo"
        Log.d(tag, "Memulai addCommentWithImages. AppealID: $appealId, Total Gambar: ${imageBytesList.size}")

        // 1. Insert Komentar
        val commentRequest = CreateAppealCommentRequest(
            appeal_id = appealId,
            sender_type = senderType,
            sender_id = senderId,
            message = message
        )

        Log.d(tag, "Proses: Insert komentar ke database...")
        val insertedComment = SupabaseConfig.client.postgrest["employee_violation_appeals_comment"]
            .insert(commentRequest) { select() }
            .decodeSingle<AppealCommentResponse>()

        Log.d(tag, "Berhasil insert komentar. ID Komentar Baru: ${insertedComment.id}")

        // 2. Upload ke Storage & Insert ke Media
        val bucket = SupabaseConfig.client.storage["appeals"]

        imageBytesList.forEachIndexed { index, imageBytes ->
            try {
                val fileName = "${appealId}_${UUID.randomUUID()}.jpg"
                Log.d(tag, "Proses: Upload gambar ke-${index + 1} dengan nama $fileName")

                // Upload
                bucket.upload(path = fileName, data = imageBytes, upsert = true)

                // Ambil URL
                val publicUrl = bucket.publicUrl(fileName)
                Log.d(tag, "Berhasil upload. URL: $publicUrl")

                // Insert ke tabel Media
                val mediaRequest = CreateAppealMediaRequest(
                    appeal_id = appealId,
                    comment_id = insertedComment.id,
                    file_url = publicUrl,
                    file_type = "image"
                )

                Log.d(tag, "Proses: Insert data media ke database...")
                SupabaseConfig.client.postgrest["employee_violation_appeals_media"].insert(mediaRequest)
                Log.d(tag, "Berhasil insert data media ke- ${index + 1}")

            } catch (e: Exception) {
                Log.e(tag, "GAGAL pada gambar ke-${index + 1}: ${e.message}")
                throw e // Tetap throw agar safeSupabaseCall menangkapnya sebagai Error
            }
        }

        Log.d(tag, "Semua proses selesai dengan sukses.")
        Unit
    }

    suspend fun updateAppealState(appealId: String, request: UpdateAppealStatusRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_violation_appeals"].update(request) {
            filter { eq("id", appealId) }
        }
        Unit
    }
}