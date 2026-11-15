package id.biz.yuka.libperformx.response // Sesuaikan package

import id.biz.yuka.libperformx.entity.NotificationEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NotificationResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val message: String,
    val payload: JsonElement? = null, // Menggunakan JsonElement agar fleksibel (bisa string/objek)
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String
) {
    // Fungsi Refaktor: Mengubah Response menjadi Entity
    fun toEntity() = NotificationEntity(
        id = id,
        userId = userId,
        title = title,
        message = message,
        payload = payload?.toString(), // Konversi ke string aman
        isRead = isRead,
        createdAt = createdAt
    )
}