package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.AnnouncementEntity
import id.biz.yuka.libperformx.request.AnnouncementStoreInsertRequest
import id.biz.yuka.libperformx.request.CreateAnnouncementRequest
import id.biz.yuka.libperformx.request.UpdateAnnouncementRequest
import id.biz.yuka.libperformx.response.AnnouncementResponse
import id.biz.yuka.libperformx.response.toEntity
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Clock
import java.util.UUID

class AnnouncementRepository {

    private val bucketName = "announcements"

    /**
     * 1. Ambil semua pengumuman.
     * Bisa difilter 1 storeId, multi storeId, atau null / kosong untuk seluruh store.
     */
    suspend fun getAllAnnouncements(storeIds: List<String>? = null): ResultState<List<AnnouncementEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest.from("announcements").select(
                columns = Columns.raw("*, announcement_stores(store_id)")
            ) {
                if (!storeIds.isNullOrEmpty()) {
                    filter {
                        isIn("announcement_stores.store_id", storeIds)
                    }
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<AnnouncementResponse>()

            responseList.map { it.toEntity() }
        }
    }

    /**
     * 2. Ambil pengumuman yang aktif tayang saat ini (now between start_at and end_at & is_active = true).
     * Mendukung filter single / multi store atau null untuk semua.
     */
    suspend fun getActiveAnnouncements(storeIds: List<String>? = null): ResultState<List<AnnouncementEntity>> {
        return safeSupabaseCall {
            val nowIso = Clock.System.now().toString()

            val responseList = SupabaseConfig.client.postgrest.from("announcements").select(
                columns = Columns.raw("*, announcement_stores(store_id)")
            ) {
                filter {
                    eq("is_active", true)
                    lte("start_at", nowIso)
                    gte("end_at", nowIso)
                    if (!storeIds.isNullOrEmpty()) {
                        isIn("announcement_stores.store_id", storeIds)
                    }
                }
                order("start_at", Order.DESCENDING)
            }.decodeList<AnnouncementResponse>()

            responseList.map { it.toEntity() }
        }
    }

    /**
     * 3. Tambah pengumuman baru (Support teks & gambar via upload ke Storage bucket).
     * @param targetStoreIds Daftar store yang dihubungkan.
     * @param imageByteArray File byte gambar jika tipe adalah image.
     */
    suspend fun createAnnouncement(
        title: String,
        type: String, // "text" atau "image"
        content: String? = null,
        imageByteArray: ByteArray? = null,
        imageExtension: String = "jpg",
        actionUrl: String? = null,
        startAt: String,
        endAt: String,
        createdBy: String? = null,
        targetStoreIds: List<String> = emptyList()
    ): ResultState<AnnouncementEntity> {
        return safeSupabaseCall {
            var uploadedImageUrl: String? = null

            // Upload gambar ke Supabase Storage bucket "announcements" jika type == "image"
            if (type.equals("image", ignoreCase = true) && imageByteArray != null) {
                val fileName = "announcement_${System.currentTimeMillis()}_${UUID.randomUUID()}.$imageExtension"
                val bucket = SupabaseConfig.client.storage.from(bucketName)
                bucket.upload(fileName, imageByteArray)
                uploadedImageUrl = bucket.publicUrl(fileName)
            }

            // Insert ke tabel announcements
            val insertedAnnouncement = SupabaseConfig.client.postgrest.from("announcements").insert(
                CreateAnnouncementRequest(
                    title = title,
                    type = type,
                    content = content,
                    image_url = uploadedImageUrl,
                    action_url = actionUrl,
                    start_at = startAt,
                    end_at = endAt,
                    created_by = createdBy
                )
            ) {
                select()
            }.decodeSingle<AnnouncementResponse>()

            // Insert mapping relasi multi-store ke junction table (announcement_stores)
            if (targetStoreIds.isNotEmpty()) {
                val storePayload = targetStoreIds.map { storeId ->
                    AnnouncementStoreInsertRequest(
                        announcement_id = insertedAnnouncement.id,
                        store_id = storeId
                    )
                }
                SupabaseConfig.client.postgrest.from("announcement_stores").insert(storePayload)
            }

            insertedAnnouncement.toEntity().copy(storeIds = targetStoreIds)
        }
    }

    /**
     * 4a. Edit Pengumuman & update daftar relasi store jika diberikan.
     */
    suspend fun updateAnnouncement(
        announcementId: String,
        request: UpdateAnnouncementRequest,
        newImageByteArray: ByteArray? = null,
        imageExtension: String = "jpg",
        updatedStoreIds: List<String>? = null
    ): ResultState<Unit> {
        return safeSupabaseCall {
            var finalRequest = request

            // Upload gambar baru jika disertakan
            if (newImageByteArray != null) {
                val fileName = "announcement_${System.currentTimeMillis()}_${UUID.randomUUID()}.$imageExtension"
                val bucket = SupabaseConfig.client.storage.from(bucketName)
                bucket.upload(fileName, newImageByteArray)
                finalRequest = finalRequest.copy(image_url = bucket.publicUrl(fileName))
            }

            // Update tabel utama
            SupabaseConfig.client.postgrest.from("announcements").update(finalRequest) {
                filter {
                    eq("id", announcementId)
                }
            }

            // Sinkronisasi store_ids jika parameter diisi
            if (updatedStoreIds != null) {
                // Hapus relasi lama
                SupabaseConfig.client.postgrest.from("announcement_stores").delete {
                    filter {
                        eq("announcement_id", announcementId)
                    }
                }

                // Insert relasi baru
                if (updatedStoreIds.isNotEmpty()) {
                    val storePayload = updatedStoreIds.map { storeId ->
                        AnnouncementStoreInsertRequest(
                            announcement_id = announcementId,
                            store_id = storeId
                        )
                    }
                    SupabaseConfig.client.postgrest.from("announcement_stores").insert(storePayload)
                }
            }
        }
    }

    /**
     * 4b. Hapus Pengumuman (Junction table `announcement_stores` terhapus otomatis via CASCADE).
     */
    suspend fun deleteAnnouncement(announcementId: String): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest.from("announcements").delete {
                filter {
                    eq("id", announcementId)
                }
            }
        }
    }
}