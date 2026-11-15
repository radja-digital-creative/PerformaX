package id.biz.yuka.libperformx.summary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response dari tabel daily_reports — digunakan oleh DailyReportRepository.
 */
@Serializable
data class DailyReportResponse(
    @SerialName("id") val id: String,
    @SerialName("report_date") val reportDate: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("generated_by") val generatedBy: String = "system"
) {
    fun toEntity() = DailyReportEntity(
        id = id,
        reportDate = reportDate,
        fileUrl = fileUrl,
        generatedAt = generatedAt,
        generatedBy = generatedBy
    )
}
