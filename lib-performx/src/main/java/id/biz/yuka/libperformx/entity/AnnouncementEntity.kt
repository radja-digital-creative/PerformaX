package id.biz.yuka.libperformx.entity

data class AnnouncementEntity(
    val id: String,
    val title: String,
    val type: String,
    val content: String?,
    val imageUrl: String?,
    val actionUrl: String?,
    val isActive: Boolean,
    val startAt: String,
    val endAt: String,
    val createdBy: String?,
    val createdAt: String?,
    val storeIds: List<String> = emptyList()
)