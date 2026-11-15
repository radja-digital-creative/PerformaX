package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.KpiPointRuleEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.KpiPointRuleRequest
import id.biz.yuka.libperformx.response.KpiPointRuleResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest

class KpiPointRuleRepository {

    /** Mendapatkan semua aturan poin KPI */
    suspend fun getAllRules(): ResultState<List<KpiPointRuleEntity>> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["kpi_point_rules"]
            .select()
            .decodeList<KpiPointRuleResponse>()
        response.map { it.toEntity() }
    }

    /**
     * UPDATE ATURAN (Edit Saja)
     * Menggunakan .update() alih-alih .upsert() agar
     * tidak terjadi penambahan baris baru secara tidak sengaja.
     */
    suspend fun updateRule(ruleCode: String, request: KpiPointRuleRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["kpi_point_rules"]
            .update({
                set("point_value", request.point_value)
                set("description", request.description)
            }) {
                filter { eq("rule_code", ruleCode) }
            }
        Unit
    }
}