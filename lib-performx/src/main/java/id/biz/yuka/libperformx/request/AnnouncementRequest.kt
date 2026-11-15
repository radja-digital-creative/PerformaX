package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementStoreInsertRequest(
    val announcement_id: String,
    val store_id: String
)

@Serializable
data class CreateAnnouncementRequest(
    val title: String,
    val type: String,
    val content: String? = null,
    val image_url: String? = null,
    val action_url: String? = null,
    val is_active: Boolean = true,
    val start_at: String,
    val end_at: String,
    val created_by: String? = null
)

@Serializable
data class UpdateAnnouncementRequest(
    val title: String? = null,
    val type: String? = null,
    val content: String? = null,
    val image_url: String? = null,
    val action_url: String? = null,
    val is_active: Boolean? = null,
    val start_at: String? = null,
    val end_at: String? = null
)