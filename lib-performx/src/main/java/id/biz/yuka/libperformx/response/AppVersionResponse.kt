package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class AppVersionResponse(
    val id: String,
    val version_code: Int,
    val version_name: String,
    val release_notes: String? = null,
    val apk_url: String,
    val created_at: String
)