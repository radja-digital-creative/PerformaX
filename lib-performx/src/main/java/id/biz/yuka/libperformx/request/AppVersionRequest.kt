package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateAppVersionRequest(
    val version_code: Int,
    val version_name: String,
    val release_notes: String? = null,
    val apk_url: String
)