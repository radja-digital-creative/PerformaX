package id.biz.yuka.libperformx.repository

import android.util.Log
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.data.remote.dto.CreateManualDeductionRequest
import id.biz.yuka.libperformx.data.remote.dto.InsertManualDeductionDto
import id.biz.yuka.libperformx.data.remote.dto.InsertUserSalaryDeductionDto
import id.biz.yuka.libperformx.entity.ManualDeductionEntity
import id.biz.yuka.libperformx.response.ManualDeductionResponse
import id.biz.yuka.libperformx.response.toEntity
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ManualDeductionRepository {

    suspend fun createManualDeduction(request: CreateManualDeductionRequest): ResultState<Unit> = safeSupabaseCall {
        val validSalaryId = if (request.userSalaryId.isNotBlank() && request.userSalaryId != "all") request.userSalaryId else null

        val insertedList = SupabaseConfig.client.postgrest["manual_deductions"]
            .insert(
                InsertManualDeductionDto(
                    title = request.title,
                    amount = request.amount,
                    notes = request.notes,
                    user_salary_id = validSalaryId
                )
            ) { select() }
            .decodeList<ManualDeductionResponse>()

        val insertedId = insertedList.firstOrNull()?.id

        if (insertedId != null && validSalaryId != null) {
            try {
                SupabaseConfig.client.postgrest["user_salary_deductions"]
                    .insert(
                        InsertUserSalaryDeductionDto(
                            user_salary_id = validSalaryId,
                            manual_deduction_id = insertedId
                        )
                    )
            } catch (e: Exception) {
                Log.e("ManualDeductionRepo", "Error inserting user_salary_deduction", e)
                // Secondary link is optional
            }
        }
        Unit
    }

    suspend fun getManualDeductionsByUserSalary(userSalaryId: String): ResultState<List<ManualDeductionEntity>> = safeSupabaseCall {
        val filterBySalary = userSalaryId.isNotBlank() && userSalaryId != "all"
        try {
            val list = SupabaseConfig.client.postgrest["manual_deductions"]
                .select(columns = Columns.raw("*, profiles!user_salary_id(full_name)")) {
                    if (filterBySalary) {
                        filter { eq("user_salary_id", userSalaryId) }
                    }
                }
                .decodeList<ManualDeductionResponse>()
            list.map { it.toEntity() }
        } catch (e: Exception) {
            Log.e("ManualDeductionRepo", "Error fetching manual deductions with profiles", e)
            // Fallback without FK join if profiles FK isn't linked
            val list = SupabaseConfig.client.postgrest["manual_deductions"]
                .select {
                    if (filterBySalary) {
                        filter { eq("user_salary_id", userSalaryId) }
                    }
                }
                .decodeList<ManualDeductionResponse>()
            list.map { it.toEntity() }
        }
    }

    suspend fun deleteManualDeduction(id: String): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["manual_deductions"]
            .delete { filter { eq("id", id) } }
        Unit
    }
}
