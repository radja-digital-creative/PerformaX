package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.*
import id.biz.yuka.libperformx.request.*
import id.biz.yuka.libperformx.response.*
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EmployeeRuleRepository {

    // =======================================================
    // BAGIAN 1: CRUD ATURAN KARYAWAN (MASTER RULES)
    // =======================================================

    suspend fun getAllRules(): ResultState<List<EmployeeRuleEntity>> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["employee_rules"]
            .select { filter { eq("is_active", true) } }
            .decodeList<EmployeeRuleResponse>()
        response.map { it.toEntity() }
    }

    suspend fun getRulesByStoreId(storeId: String): ResultState<List<StoreRuleEntity>> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["store_rules"]
            .select { filter { eq("store_id", storeId) } }
            .decodeList<StoreRuleResponse>()
        response.map { it.toEntity() }
    }

    suspend fun createRule(request: CreateEmployeeRuleRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_rules"].insert(request)
        Unit
    }

    suspend fun updateRule(ruleId: String, request: UpdateEmployeeRuleRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_rules"].update(request) {
            filter { eq("id", ruleId) }
        }
        Unit
    }

    suspend fun deleteRule(ruleId: String): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_rules"].update({
            set("is_active", false)
        }) {
            filter { eq("id", ruleId) }
        }
        Unit
    }

    // =======================================================
    // BAGIAN 2: LOG PELANGGARAN (VIOLATION LOGS)
    // =======================================================

    /**
     * Mengambil semua log pelanggaran.
     * Default: Filter otomatis dari awal bulan hingga akhir bulan ini.
     */
    suspend fun getAllViolationLogs(
        startDate: String,
        endDate: String
    ): ResultState<List<ViolationLogEntity>> = safeSupabaseCall {

        val response = SupabaseConfig.client.postgrest["employee_violation_logs"]
            .select {
                filter {
                    // Batasi data masa lalu dari database agar loading tetap ringan
                    gte("violation_date", startDate)
                }
            }
            .decodeList<ViolationLogResponse>()

        // BYPASS BUG SUPABASE SDK: Filter batas akhir (endDate) di level Kotlin
        response.map { it.toEntity() }
            .filter { it.violationDate <= endDate }
    }

    /**
     * Mengambil log pelanggaran untuk satu user.
     * Default: Filter otomatis bulan ini.
     */
    suspend fun getViolationLogsByUserId(
        userId: String,
        startDate: String,
        endDate: String
    ): ResultState<List<ViolationLogEntity>> = safeSupabaseCall {

        val response = SupabaseConfig.client.postgrest["employee_violation_logs"]
            .select {
                filter {
                    eq("user_id", userId)
                    // Batasi data masa lalu dari database agar loading tetap ringan
                    gte("violation_date", startDate)
                }
            }
            .decodeList<ViolationLogResponse>()

        // BYPASS BUG SUPABASE SDK: Filter batas akhir (endDate) di level Kotlin
        response.map { it.toEntity() }
            .filter { it.violationDate <= endDate }
    }

    /**
     * Mengambil SATU detail log pelanggaran berdasarkan ID.
     * Digunakan untuk memuat detail pelanggaran di ruang chat banding.
     */
    suspend fun getViolationLogById(logId: String): ResultState<ViolationLogEntity> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["employee_violation_logs"]
            .select { filter { eq("id", logId) } }
            .decodeSingle<ViolationLogResponse>()
        response.toEntity()
    }

    /**
     * Fungsi baru untuk mencatat teguran manual (Manual SP)
     * dari Owner/Manager ke Karyawan.
     */
    suspend fun addViolationLog(request: CreateViolationLogRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_violation_logs"].insert(request)
        Unit
    }

    /**
     * Memproses log pelanggaran (Mengubah status menjadi Processed/Payroll)
     */
    suspend fun processViolationLog(logId: String): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_violation_logs"].update({
            set("status", "PROCESSED")
            set("is_processed_to_payroll", true)
        }) {
            filter { eq("id", logId) }
        }
        Unit
    }

    /**
     * Memperbarui status pelanggaran (Misal: dari PENDING menjadi ARCHIVED karena banding diterima)
     */
    suspend fun updateViolationStatus(logId: String, newStatus: String): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["employee_violation_logs"].update({
            set("status", newStatus)
            if (newStatus == "PROCESSED") set("is_processed_to_payroll", true)
        }) {
            filter { eq("id", logId) }
        }
        Unit
    }

    // =======================================================
    // BAGIAN 3: PENGATURAN ATURAN DI TOKO (STORE RULES)
    // =======================================================

    suspend fun addRuleToStore(request: CreateStoreRuleRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["store_rules"].insert(request)
        Unit
    }

    suspend fun removeRuleFromStore(storeId: String, ruleId: String): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["store_rules"].delete {
            filter {
                eq("store_id", storeId)
                eq("rule_id", ruleId)
            }
        }
        Unit
    }
}