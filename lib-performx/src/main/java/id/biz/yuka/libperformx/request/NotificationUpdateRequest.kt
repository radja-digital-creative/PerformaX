package id.biz.yuka.libperformx.request // Sesuaikan dengan package Komandan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationUpdateRequest(
    @SerialName("is_read") val isRead: Boolean
)