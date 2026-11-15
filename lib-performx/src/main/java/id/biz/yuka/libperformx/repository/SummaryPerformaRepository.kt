package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.DetailPerformaEntity
import id.biz.yuka.libperformx.entity.SummaryPerformaEntity
import id.biz.yuka.libperformx.request.DetailPerformaRequest
import id.biz.yuka.libperformx.request.SummaryPerformaRequest
import id.biz.yuka.libperformx.response.DetailPerformaResponse
import id.biz.yuka.libperformx.response.SummaryPerformaResponse
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SummaryPerformaRepository {

    // 1. Fungsi memanggil API Singkat (Home Screen)
    suspend fun getPerformanceSummary(userId: String): ResultState<SummaryPerformaEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SummaryPerformaRequest(pUserId = userId)

                // Decode ke Response, lalu langsung petakan ke Entity
                val response = SupabaseConfig.client.postgrest.rpc(
                    function = "get_performance_summary",
                    parameters = request
                ).decodeAs<SummaryPerformaResponse>()

                ResultState.Success(response.toEntity())
            } catch (e: Exception) {
                ResultState.Error(e.message ?: "Gagal memuat ringkasan performa")
            }
        }
    }

    // 2. Fungsi memanggil API Detail (Halaman Khusus)
    suspend fun getPerformanceDetail(userId: String, month: Int, year: Int): ResultState<DetailPerformaEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DetailPerformaRequest(
                    pUserId = userId,
                    pMonth = month,
                    pYear = year
                )

                // Decode ke Response, lalu langsung petakan ke Entity
                val response = SupabaseConfig.client.postgrest.rpc(
                    function = "get_performance",
                    parameters = request
                ).decodeAs<DetailPerformaResponse>()

                ResultState.Success(response.toEntity())
            } catch (e: Exception) {
                ResultState.Error(e.message ?: "Gagal memuat detail performa")
            }
        }
    }
}