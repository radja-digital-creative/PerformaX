package id.biz.yuka.libperformx.repository // Sesuaikan dengan package Komandan

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.entity.NotificationEntity
import id.biz.yuka.libperformx.request.NotificationUpdateRequest
import id.biz.yuka.libperformx.response.NotificationResponse
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository{

    suspend fun getNotifications(userId: String): ResultState<List<NotificationEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = SupabaseConfig.client.postgrest["notifications"]
                    .select {
                        filter { eq("user_id", userId) }
                        order(column = "created_at", order = Order.DESCENDING)
                    }
                    .decodeList<NotificationResponse>() // Decode ke Response dulu

                // Refaktor/Mapping ke Entity
                val entityList = response.map { it.toEntity() }

                ResultState.Success(entityList)
            } catch (e: Exception) {
                ResultState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    suspend fun markAsRead(notificationId: String): ResultState<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updateRequest = NotificationUpdateRequest(isRead = true)

                SupabaseConfig.client.postgrest["notifications"]
                    .update(updateRequest) {
                        filter {
                            eq("id", notificationId)
                        }
                    }
                ResultState.Success(true)
            } catch (e: Exception) {
                ResultState.Error(e.message ?: "Gagal menandai notifikasi")
            }
        }
    }
}