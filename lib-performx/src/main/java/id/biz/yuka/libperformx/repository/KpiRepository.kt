package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.KpiEntity
import id.biz.yuka.libperformx.request.EmployeeKpiRequest
import id.biz.yuka.libperformx.response.KpiResponse
import id.biz.yuka.libperformx.response.toEntity
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

// --- REPOSITORY ---
class KpiRepository {

    /**
     * Memanggil RPC Supabase untuk menghitung & mengambil ranking KPI.
     * Mengembalikan list semua karyawan yang diurutkan dari skor tertinggi.
     */
    suspend fun getKpiLeaderboard(month: Int, year: Int): ResultState<List<KpiEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest.rpc(
                function = "get_kpi_leaderboard",
                parameters = mapOf("p_month" to month, "p_year" to year)
            ).decodeList<KpiResponse>()

            responseList.map { it.toEntity() }.sortedBy { it.rank }
        }
    }

    /**
     * Memanggil RPC Supabase untuk mengambil detail KPI spesifik 1 karyawan.
     */
    suspend fun getEmployeeKpiDetail(userId: String, month: Int, year: Int): ResultState<KpiEntity> {
        return safeSupabaseCall {
            val request = EmployeeKpiRequest(
                p_user_id = userId,
                p_month = month,
                p_year = year
            )

            val response = SupabaseConfig.client.postgrest.rpc(
                function = "get_employee_kpi", // Pastikan membuat RPC ini di database
                parameters = request
            ).decodeSingle<KpiResponse>()

            response.toEntity()
        }
    }
}