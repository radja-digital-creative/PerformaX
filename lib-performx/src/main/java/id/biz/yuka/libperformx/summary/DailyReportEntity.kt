package id.biz.yuka.libperformx.summary

/**
 * Entity untuk metadata Daily Report yang tersimpan di tabel daily_reports.
 */
data class DailyReportEntity(
    val id: String,
    val reportDate: String,
    val fileUrl: String,
    val generatedAt: String,
    val generatedBy: String
)

/**
 * State untuk card Daily Report di SummaryCarouselSection.
 * Hanya dipakai oleh OperasionalViewModel untuk cek ketersediaan report hari ini.
 */
sealed class DailyReportCardState {
    /** Belum pernah fetch */
    object Idle : DailyReportCardState()

    /** Sedang mengecek ke Supabase */
    object Loading : DailyReportCardState()

    /** Report hari ini tersedia — simpan URL & waktu generate */
    data class Available(
        val generatedAt: String,
        val fileUrl: String
    ) : DailyReportCardState()

    /** Report belum tersedia (belum di-generate atau belum jam 19.00) */
    object NotAvailable : DailyReportCardState()

    /** Terjadi error saat fetch */
    data class Error(val message: String) : DailyReportCardState()
}
