package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.ResignationEntity
import id.biz.yuka.libperformx.request.CreateResignationRequest
import id.biz.yuka.libperformx.request.ProcessResignationRequest
import id.biz.yuka.libperformx.request.UpdateResignationStatusRequest
import id.biz.yuka.libperformx.response.ResignationResponse
import id.biz.yuka.libperformx.response.toEntity
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ResignationRepository {

    fun submitResignation(request: CreateResignationRequest): Flow<ResultState<Unit>> = flow {
        val result = safeSupabaseCall {
            SupabaseConfig.client.postgrest["employee_resignations"].insert(request)
            Unit
        }
        emit(result)
    }

    fun getActiveResignationByUser(userId: String): Flow<ResultState<ResignationEntity?>> = flow {
        val result = safeSupabaseCall {
            val list = SupabaseConfig.client.postgrest["employee_resignations"]
                .select(columns = Columns.raw("*, profiles!user_id(full_name)")) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ResignationResponse>()

            list.firstOrNull()?.toEntity()
        }
        emit(result)
    }

    fun getAllResignations(): Flow<ResultState<List<ResignationEntity>>> = flow {
        val result = safeSupabaseCall {
            val list = SupabaseConfig.client.postgrest["employee_resignations"]
                .select(columns = Columns.raw("*, profiles!user_id(full_name)")) {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ResignationResponse>()

            list.map { it.toEntity() }
        }
        emit(result)
    }

    fun processResignation(request: ProcessResignationRequest): Flow<ResultState<Unit>> = flow {
        val result = safeSupabaseCall {
            SupabaseConfig.client.postgrest["employee_resignations"]
                .update(
                    UpdateResignationStatusRequest(
                        status = request.status,
                        approved_by = request.approved_by,
                        rejection_reason = request.rejection_reason,
                        processed_at = request.processed_at
                    )
                ) {
                    filter {
                        eq("id", request.resignation_id)
                    }
                }
            Unit
        }
        emit(result)
    }
}
