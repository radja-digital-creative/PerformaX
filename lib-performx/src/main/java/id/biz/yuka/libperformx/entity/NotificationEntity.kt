package id.biz.yuka.libperformx.entity // Sesuaikan dengan package Komandan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationEntity(
    val id: String, // Atau Int, sesuaikan tipe data ID di database
    @SerialName("user_id") val userId: String,
    val title: String,
    val message: String,
    val payload: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String
)