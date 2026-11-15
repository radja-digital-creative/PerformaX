package id.biz.yuka.libperformx.summary

import android.util.Log
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

/**
 * Repository untuk fitur Daily Report.
 * Berinteraksi dengan:
 *  - Tabel `daily_reports` (cek ketersediaan metadata)
 *  - Supabase Storage bucket `daily-reports` (download konten .md)
 *  - Edge Function `daily-report` (trigger generate/regenerate)
 */
class DailyReportRepository {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CEK KETERSEDIAAN REPORT HARI INI
    //    SELECT dari tabel daily_reports WHERE report_date = today
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun checkTodayReport(date: String): ResultState<DailyReportEntity?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = SupabaseConfig.client.postgrest["daily_reports"]
                    .select {
                        filter { eq("report_date", date) }
                        limit(1)
                    }
                    .decodeSingleOrNull<DailyReportResponse>()

                ResultState.Success(response?.toEntity())
            } catch (e: Exception) {
                Log.e("DailyReportRepo", "checkTodayReport error: ${e.message}", e)
                ResultState.Error(e.message ?: "Gagal memeriksa laporan harian")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. TRIGGER GENERATE / REGENERATE VIA EDGE FUNCTION
    //    POST /functions/v1/daily-report?date=YYYY-MM-DD
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun generateReport(date: String? = null): ResultState<DailyReportEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val queryParam = if (date != null) "?date=$date" else ""
                val response = SupabaseConfig.client.functions.invoke(
                    function = "daily-report$queryParam"
                )
                
                if (response.status.value >= 300) {
                    return@withContext ResultState.Error("Gagal men-generate laporan: Server Error (${response.status.value})")
                }

                val responseText = response.bodyAsText()
                // Parse JSON response dari edge function
                val json = Json.parseToJsonElement(responseText).jsonObject
                
                val reportDate = json["date"]?.jsonPrimitive?.content ?: date ?: ""
                
                // Berikan jeda sebentar agar DB/Storage sinkron
                delay(1.seconds)

                // Fetch ulang dari tabel untuk mendapatkan id yang valid
                val entityResult = checkTodayReport(reportDate)
                if (entityResult is ResultState.Success && entityResult.data != null) {
                    ResultState.Success(entityResult.data)
                } else {
                    val fileUrl = json["file_url"]?.jsonPrimitive?.content ?: ""
                    val generatedAt = json["generated_at"]?.jsonPrimitive?.content ?: ""
                    
                    // Fallback: buat entity dari response edge function
                    ResultState.Success(
                        DailyReportEntity(
                            id = "generated_temp",
                            reportDate = reportDate,
                            fileUrl = fileUrl,
                            generatedAt = generatedAt,
                            generatedBy = "system"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("DailyReportRepo", "generateReport error: ${e.message}", e)
                ResultState.Error(e.message ?: "Gagal membuat laporan harian")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. AMBIL SEMUA LAPORAN (untuk layar daftar download)
    //    SELECT * FROM daily_reports ORDER BY report_date DESC
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getAllReports(): ResultState<List<DailyReportEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val list = SupabaseConfig.client.postgrest["daily_reports"]
                    .select {
                        order("report_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<DailyReportResponse>()
                    .map { it.toEntity() }
                ResultState.Success(list)
            } catch (e: Exception) {
                Log.e("DailyReportRepo", "getAllReports error: ${e.message}", e)
                ResultState.Error(e.message ?: "Gagal memuat daftar laporan")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. DOWNLOAD KONTEN MARKDOWN DARI STORAGE
    //    Menggunakan signed URL yang sudah tersimpan di daily_reports.file_url
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun downloadMarkdownContent(date: String): ResultState<String> {
        return withContext(Dispatchers.IO) {
            try {
                val filePath = "$date.md"
                val bytes = SupabaseConfig.client.storage["daily-reports"]
                    .downloadAuthenticated(filePath)

                ResultState.Success(bytes.decodeToString())
            } catch (e: NotFoundRestException) {
                Log.w("DailyReportRepo", "File not found in storage: $date.md - ${e.message}")
                ResultState.Error("Laporan harian belum dibuat atau tidak ditemukan untuk tanggal $date.")
            } catch (e: Exception) {
                Log.e("DailyReportRepo", "downloadMarkdownContent error: ${e.message}", e)
                ResultState.Error(e.message ?: "Gagal mengunduh konten laporan")
            }
        }
    }
}
