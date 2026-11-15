package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.BonusEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.CreateBonusRequest
import id.biz.yuka.libperformx.response.BonusResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest

class BonusRepository {

    /**
     * Mengambil SEMUA bonus dalam rentang tanggal tertentu. (Untuk Owner)
     */
    suspend fun getAllBonuses(
        startDate: String,
        endDate: String
    ): ResultState<List<BonusEntity>> = safeSupabaseCall {

        val response = SupabaseConfig.client.postgrest["employee_bonuses"]
            .select {
                filter {
                    gte("bonus_date", startDate) // Batasi penarikan data dari server
                }
            }
            .decodeList<BonusResponse>()

        // Filter endDate menggunakan Kotlin untuk bypass isu overwrite gte & lte di SDK
        response.map { it.toEntity() }
            .filter { it.bonusDate <= endDate }
    }

    /**
     * Mengambil bonus HANYA untuk satu karyawan spesifik. (Untuk Karyawan)
     */
    suspend fun getBonusesByUserId(
        userId: String,
        startDate: String,
        endDate: String
    ): ResultState<List<BonusEntity>> = safeSupabaseCall {

        val response = SupabaseConfig.client.postgrest["employee_bonuses"]
            .select {
                filter {
                    eq("user_id", userId)
                    gte("bonus_date", startDate)
                }
            }
            .decodeList<BonusResponse>()

        response.map { it.toEntity() }
            .filter { it.bonusDate <= endDate }
    }

    /**
     * Menambahkan bonus baru (Misal owner input bonus manual)
     */
    suspend fun addBonus(request: CreateBonusRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_bonuses"].insert(request)
        Unit
    }

    /**
     * Memproses bonus agar cair / masuk ke sistem Payroll.
     */
    suspend fun processBonus(bonusId: String): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_bonuses"].update({
            set("is_processed", true)
        }) {
            filter { eq("id", bonusId) }
        }
        Unit
    }
}