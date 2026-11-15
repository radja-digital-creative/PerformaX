package id.biz.yuka.libperformx.response

import id.biz.yuka.libperformx.entity.AnnouncementEntity
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementStoreItemResponse(
    val store_id: String
)

@Serializable
data class AnnouncementResponse(
    val id: String,
    val title: String,
    val type: String,
    val content: String? = null,
    val image_url: String? = null,
    val action_url: String? = null,
    val is_active: Boolean = true,
    val start_at: String,
    val end_at: String,
    val created_by: String? = null,
    val created_at: String? = null,
    val announcement_stores: List<AnnouncementStoreItemResponse> = emptyList()
)

fun AnnouncementResponse.toEntity() = AnnouncementEntity(
    id = this.id,
    title = this.title,
    type = this.type,
    content = this.content,
    imageUrl = this.image_url,
    actionUrl = this.action_url,
    isActive = this.is_active,
    startAt = this.start_at,
    endAt = this.end_at,
    createdBy = this.created_by,
    createdAt = this.created_at,
    storeIds = this.announcement_stores.map { it.store_id }
)