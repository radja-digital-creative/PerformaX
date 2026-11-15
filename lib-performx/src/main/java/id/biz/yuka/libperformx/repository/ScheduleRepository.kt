package id.biz.yuka.libperformx.repository

import android.util.Log
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.ScheduleEntity
import id.biz.yuka.libperformx.entity.ShiftEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.CreateScheduleRequest
import id.biz.yuka.libperformx.request.ShiftRequest
import id.biz.yuka.libperformx.request.UpdateScheduleShiftRequest
import id.biz.yuka.libperformx.request.UpsertScheduleRequest
import id.biz.yuka.libperformx.response.ProfileNameResponse
import id.biz.yuka.libperformx.response.ScheduleDetailResponse
import id.biz.yuka.libperformx.response.ScheduleIdResponse
import id.biz.yuka.libperformx.response.ScheduleResponse
import id.biz.yuka.libperformx.response.ShiftResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc

class ScheduleRepository {

    // ==========================================
    // BAGIAN 1: MANAJEMEN JADWAL KARYAWAN
    // ==========================================

    suspend fun createSchedule(request: CreateScheduleRequest): ResultState<ScheduleEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["user_schedules"]
                .insert(request) { select() }
                .decodeSingle<ScheduleResponse>()
            response.toEntity()
        }
    }

    suspend fun getDailyAttendanceSchedule(userId: String, date: String): ResultState<ScheduleEntity?> {
        return try {
            // Hapus safeSupabaseCall SEMENTARA untuk debugging
            val response = SupabaseConfig.client.postgrest["user_schedules"]
                .select(columns = Columns.raw("*, shifts(*)")) {
                    filter {
                        eq("user_id", userId)
                        eq("work_date", date)
                    }
                }
                .decodeSingleOrNull<ScheduleResponse>()

            ResultState.Success(response?.toEntity())
        } catch (e: Exception) {
            // ✅ CEK LOGCAT ANDROID STUDIO DENGAN FILTER "Supabase"
            Log.e("SupabaseDebug", "Error Detail: ${e.message}")
            e.printStackTrace()
            ResultState.Error(e.message ?: "Unknown Error")
        }
    }

    suspend fun getMonthlyAttendanceSchedule(userId: String, startDate: String, endDate: String): ResultState<List<ScheduleEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["user_schedules"]
                .select { filter { eq("user_id", userId); gte("work_date", startDate); lte("work_date", endDate) } }
                .decodeList<ScheduleResponse>()
            responseList.map { it.toEntity() }
        }
    }

    suspend fun deleteSchedule(scheduleId: String): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["user_schedules"].delete { filter { eq("id", scheduleId) } }
            Unit
        }
    }

    // ==========================================
    // BAGIAN 2: MANAJEMEN MASTER SHIFT (Admin/Owner)
    // ==========================================

    /** Mengambil semua daftar Shift (Bisa difilter berdasarkan ID Toko jika perlu) */
    suspend fun getShifts(storeId: String? = null): ResultState<List<ShiftEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["shifts"].select {
                if (storeId != null) {
                    filter { eq("store_id", storeId) }
                }
            }.decodeList<ShiftResponse>()

            responseList.map { it.toEntity() }
        }
    }

    /** Membuat Shift Baru */
    suspend fun createShift(request: ShiftRequest): ResultState<ShiftEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["shifts"]
                .insert(request) { select() }
                .decodeSingle<ShiftResponse>()
            response.toEntity()
        }
    }

    /** Mengedit/Memperbarui Shift yang sudah ada */
    suspend fun updateShift(shiftId: String, request: ShiftRequest): ResultState<ShiftEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["shifts"]
                .update(request) {
                    filter { eq("id", shiftId) }
                    select() // Kembalikan data terbaru setelah diupdate
                }
                .decodeSingle<ShiftResponse>()
            response.toEntity()
        }
    }

    /** Menghapus Shift */
    suspend fun deleteShift(shiftId: String): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["shifts"].delete { filter { eq("id", shiftId) } }
            Unit
        }
    }
    /** MENGAMBIL SEMUA JADWAL BULANAN (Khusus Owner / Global View) */
    suspend fun getAllMonthlySchedules(startDate: String, endDate: String): ResultState<List<ScheduleEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["user_schedules"]
                .select {
                    filter {
                        gte("work_date", startDate)
                        lte("work_date", endDate)
                    }
                }
                .decodeList<ScheduleResponse>()
            responseList.map { it.toEntity() }
        }
    }

    /** MENGAMBIL MAP ID KE NAMA KARYAWAN (Membantu Owner mengenali pemilik jadwal) */
    suspend fun getEmployeeNamesMap(): ResultState<Map<String, String>> {
        return safeSupabaseCall {

            val responseList = SupabaseConfig.client.postgrest["profiles"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("id, full_name"))
                .decodeList<ProfileNameResponse>()

            responseList.associate { it.id to it.full_name }
        }
    }

    /** MENGAMBIL JADWAL LEWAT VIEW (Otomatis memuat Nama Karyawan & Nama Toko tanpa RLS Block) */
    suspend fun getScheduleDetailsFromView(userId: String? = null, startDate: String, endDate: String): ResultState<List<ScheduleDetailResponse>> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["vw_schedule_details"]
                .select {
                    filter {
                        gte("work_date", startDate)
                        lte("work_date", endDate)
                        if (userId != null) {
                            eq("user_id", userId) // Jika karyawan biasa, filter jadwal miliknya saja
                        }
                    }
                }
                .decodeList<ScheduleDetailResponse>()
        }
    }


    suspend fun generateBulkSchedule(
        userId: String,
        storeId: String,
        shiftId: String,
        activeDays: List<Int>, // 1 = Senin, 7 = Minggu
        monthsAhead: Int
    ): ResultState<Boolean> {
        return safeSupabaseCall {
            val request = id.biz.yuka.libperformx.request.BulkScheduleRequest(
                p_user_id = userId,
                p_store_id = storeId,
                p_shift_id = shiftId,
                p_active_days = activeDays,
                p_months_ahead = monthsAhead
            )

            // Memanggil fungsi SQL generate_bulk_schedule
            SupabaseConfig.client.postgrest.rpc(
                function = "generate_bulk_schedule",
                parameters = request
            ).decodeAs<Boolean>()
        }
    }

    suspend fun createOrUpdateSchedule(
        scheduleId: String?,
        userId: String,
        storeId: String,
        shiftId: String?,
        workDate: String
    ): ResultState<Boolean> {
        return safeSupabaseCall {
            if (shiftId == null) {
                // 1. JIKA LIBUR: Hapus berdasarkan ID Primary Key
                if (scheduleId != null) {
                    SupabaseConfig.client.postgrest["user_schedules"].delete {
                        filter { eq("id", scheduleId) }
                    }
                }
            } else if (scheduleId != null) {
                // 2. JIKA SUDAH ADA (scheduleId ada): UPDATE shift-nya
                val updateReq = UpdateScheduleShiftRequest(
                    shift_id = shiftId,
                    notes = "Manual Override"
                )
                SupabaseConfig.client.postgrest["user_schedules"].update(updateReq) {
                    filter { eq("id", scheduleId) }
                }
            } else {
                // 3. JIKA BELUM ADA (scheduleId null): INSERT Baru
                val insertReq = CreateScheduleRequest(
                    user_id = userId,
                    store_id = storeId,
                    shift_id = shiftId,
                    work_date = workDate,
                    notes = "Manual Override"
                )
                SupabaseConfig.client.postgrest["user_schedules"].insert(insertReq)
            }
            true
        }
    }
}