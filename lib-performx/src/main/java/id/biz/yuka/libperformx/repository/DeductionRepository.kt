package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.DeductionEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.CreateDeductionRequest
import id.biz.yuka.libperformx.request.UpdateDeductionRequest
import id.biz.yuka.libperformx.response.DeductionResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest

class DeductionRepository {

    /** Mengambil semua daftar potongan aktif */
    suspend fun getAllDeductions(): ResultState<List<DeductionEntity>> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["deduction_components"]
            .select { filter { eq("is_active", true) } }
            .decodeList<DeductionResponse>()
        response.map { it.toEntity() }
    }

    /** Menambah kategori potongan baru */
    suspend fun createDeduction(request: CreateDeductionRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["deduction_components"].insert(request)
        Unit
    }

    /** Memperbarui data potongan */
    suspend fun updateDeduction(id: String, request: UpdateDeductionRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["deduction_components"].update(request) {
            filter { eq("id", id) }
        }
        Unit
    }

    /** Menghapus/Menonaktifkan potongan (Soft Delete) */
    suspend fun deleteDeduction(id: String): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["deduction_components"].update({
            set("is_active", false)
        }) {
            filter { eq("id", id) }
        }
        Unit
    }
}