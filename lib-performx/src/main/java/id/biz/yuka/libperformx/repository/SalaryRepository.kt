package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.DeductionComponentEntity
import id.biz.yuka.libperformx.entity.SalaryDetailEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.AddDeductionToEmployeeRequest
import id.biz.yuka.libperformx.request.CreateDeductionComponentRequest
import id.biz.yuka.libperformx.request.HistoricalSalaryRequest
import id.biz.yuka.libperformx.response.DeductionItemResponse
import id.biz.yuka.libperformx.response.SalaryDetailResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.MDC.put

@Serializable
data class InsertDeductionResponse(val id: String)

class SalaryRepository {

    /** 1. ADMIN: Membuat Master Potongan Baru (BPJS, Kasbon, dll) */
    suspend fun createDeductionComponent(request: CreateDeductionComponentRequest): ResultState<String> = safeSupabaseCall {
        val res = SupabaseConfig.client.postgrest["deduction_components"]
            .insert(request) { select() }
            .decodeList<InsertDeductionResponse>()
        res.first().id
    }

    /** MENGAMBIL SEMUA MASTER POTONGAN (Untuk dropdown/checkbox) */
    suspend fun getAllDeductionComponents(): ResultState<List<DeductionComponentEntity>> = safeSupabaseCall {
        val responseList = SupabaseConfig.client.postgrest["deduction_components"]
            .select()
            .decodeList<id.biz.yuka.libperformx.response.DeductionComponentResponse>()

        responseList.map { it.toEntity() }
    }

    /** 2. ADMIN: Melihat Seluruh Daftar Gaji Karyawan */
    suspend fun getAllSalaries(): ResultState<List<SalaryDetailEntity>> = safeSupabaseCall {
        val responseList = SupabaseConfig.client.postgrest["vw_salary_details"]
            .select()
            .decodeList<SalaryDetailResponse>()
        responseList.map { it.toEntity() }
    }

    /** 3. ADMIN/KARYAWAN: Melihat Detail Gaji Spesifik berdasarkan User ID */
    suspend fun getSalaryDetail(userId: String): ResultState<SalaryDetailEntity> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["vw_salary_details"]
            .select { filter { eq("user_id", userId) } }
            .decodeSingle<SalaryDetailResponse>()
        response.toEntity()
    }

    /** 4. ADMIN: Menerapkan potongan spesifik ke Gaji Karyawan */
    suspend fun addDeductionToEmployee(request: AddDeductionToEmployeeRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["user_salary_deductions"].insert(request)
        Unit
    }

    /** MENGAMBIL SELURUH RIWAYAT GAJI SATU KARYAWAN */
    suspend fun getSalaryHistory(userId: String): ResultState<List<SalaryDetailEntity>> = safeSupabaseCall {
        val responseList = SupabaseConfig.client.postgrest["vw_salary_details"]
            .select { filter { eq("user_id", userId) } }
            .decodeList<SalaryDetailResponse>() // Menggunakan decodeList agar tidak crash saat data > 1

        // Urutkan dari yang terbaru (opsional, tergantung urutan default dari database Anda)
        responseList.map { it.toEntity() }.sortedByDescending { it.updatedAt }
    }

    /**
     * BARU: ADMIN - Mengambil seluruh data slip gaji dengan filter riwayat waktu (Bulan & Tahun / Range Tanggal)
     * Menggunakan RPC Function get_historical_salary
     */
    suspend fun getHistoricalSalaries(
        month: Int? = null,
        year: Int? = null,
        startDate: String? = null,
        endDate: String? = null
    ): ResultState<List<SalaryDetailEntity>> = safeSupabaseCall {

        val request = HistoricalSalaryRequest(
            p_month = month,
            p_year = year,
            p_start_date = startDate,
            p_end_date = endDate
        )

        val responseList = SupabaseConfig.client.postgrest
            .rpc("get_historical_salary", request)
            .decodeList<SalaryDetailResponse>()

        responseList.map { it.toEntity() }
    }

    /**
     * BARU: ADMIN/KARYAWAN - Mengambil data slip gaji spesifik user dengan filter riwayat waktu
     * Menggunakan RPC Function get_historical_salary
     */
    suspend fun getHistoricalSalaryByUser(
        userId: String,
        month: Int? = null,
        year: Int? = null,
        startDate: String? = null,
        endDate: String? = null
    ): ResultState<SalaryDetailEntity> = safeSupabaseCall {

        val request = HistoricalSalaryRequest(
            p_user_id = userId,
            p_month = month,
            p_year = year,
            p_start_date = startDate,
            p_end_date = endDate
        )

        val responseList = SupabaseConfig.client.postgrest
            .rpc("get_historical_salary", request)
            .decodeList<SalaryDetailResponse>()

        responseList.first().toEntity()
    }
}