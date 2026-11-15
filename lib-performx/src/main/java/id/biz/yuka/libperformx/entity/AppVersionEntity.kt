package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.AppVersionResponse

data class AppVersionEntity(
    val id: String,
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val createdAt: String
)

fun AppVersionResponse.toEntity() = AppVersionEntity(
    id = this.id,
    versionCode = this.version_code,
    versionName = this.version_name,
    releaseNotes = this.release_notes ?: "Tidak ada catatan rilis.",
    apkUrl = this.apk_url,
    createdAt = this.created_at
)