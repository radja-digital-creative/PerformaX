package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.LeaveEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.CreateLeaveRequest
import id.biz.yuka.libperformx.request.CreateLeaveTypeRequest
import id.biz.yuka.libperformx.request.RespondLeaveRequest
import id.biz.yuka.libperformx.request.UpdateLeaveTypeRequest
import id.biz.yuka.libperformx.response.LeaveResponse
import id.biz.yuka.libperformx.response.LeaveTypeResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class LeaveRepository {

    /** Mendapatkan daftar kategori izin (Sakit, Cuti, dsb) */
    suspend fun getLeaveTypes(): ResultState<List<LeaveTypeResponse>> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["leave_types"].select().decodeList<LeaveTypeResponse>()
    }

    /** KARYAWAN: Mengajukan Izin Baru */
    suspend fun submitLeave(request: CreateLeaveRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["leave_requests"].insert(request)
        Unit
    }

    /** UPLOAD LAMPIRAN IZIN: Ke Bucket 'requests' */
    suspend fun uploadLeaveAttachment(userId: String, imageBytes: ByteArray): ResultState<String> {
        return safeSupabaseCall {
            val fileName = "leave_${userId}_${System.currentTimeMillis()}.jpg"
            val bucket = SupabaseConfig.client.storage.from("requests")

            // Upload gambar
            bucket.upload(fileName, imageBytes, upsert = true)

            // Dapatkan URL Publik
            bucket.publicUrl(fileName)
        }
    }

    /**
     * KARYAWAN: Melihat daftar pengajuan izin milik sendiri
     * (Bisa filter status & cek apakah rentang tanggal mencakup hari ini)
     */
    suspend fun getMyLeaveRequests(
        userId: String,
        status: String? = null,
        onDate: String? = null
    ): ResultState<List<LeaveEntity>> = safeSupabaseCall {

        val response = SupabaseConfig.client.postgrest["vw_leave_details"].select {
            filter {
                // 1. Filter wajib: Hanya ambil izin milik user yang sedang login
                eq("user_id", userId)

                // 2. Filter opsional: Berdasarkan status (misal: "approved")
                if (status != null) {
                    eq("status", status)
                }

                // 3. Filter opsional: Cek apakah onDate berada di antara start_date dan end_date
                if (onDate != null) {
                    lte("start_date", onDate) // start_date <= onDate
                    gte("end_date", onDate)   // end_date >= onDate
                }
            }
        }.decodeList<LeaveResponse>()

        response.map { it.toEntity() }
    }

    /** MANAGER/ADMIN: Melihat daftar pengajuan (Bisa filter status & tanggal) */
    suspend fun getLeaveRequests(
        status: String? = null,
        onDate: String? = null // Format tanggal biasanya "YYYY-MM-DD"
    ): ResultState<List<LeaveEntity>> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["vw_leave_details"].select {
            filter {
                // Filter berdasarkan status jika tidak null
                if (status != null) {
                    eq("status", status)
                }

                // Filter berdasarkan rentang tanggal jika onDate tidak null
                if (onDate != null) {
                    lte("start_date", onDate) // start_date <= onDate
                    gte("end_date", onDate)   // end_date >= onDate
                }
            }
        }.decodeList<LeaveResponse>()

        response.map { it.toEntity() }
    }

    /** MANAGER/ADMIN: Menyetujui atau Menolak Izin */
    suspend fun respondLeave(leaveId: String, request: RespondLeaveRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["leave_requests"].update(request) {
            filter { eq("id", leaveId) }
        }
        Unit
    }

    /** ADMIN: Menambah tipe izin baru */
    suspend fun createLeaveType(request: CreateLeaveTypeRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["leave_types"].insert(request)
        Unit
    }

    /** ADMIN: Mengubah tipe izin */
    suspend fun updateLeaveType(id: String, request: UpdateLeaveTypeRequest): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["leave_types"].update(request) {
            filter { eq("id", id) }
        }
        Unit
    }

    /** ADMIN: Menghapus tipe izin */
    suspend fun deleteLeaveType(id: String): ResultState<Unit> = safeSupabaseCall {
        SupabaseConfig.client.postgrest["leave_types"].delete {
            filter { eq("id", id) }
        }
        Unit
    }
}