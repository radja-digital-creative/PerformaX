package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.AttendanceDetailEntity
import id.biz.yuka.libperformx.entity.AttendanceEntity
import id.biz.yuka.libperformx.entity.ExtraWorkRequestEntity
import id.biz.yuka.libperformx.entity.ManualAttendanceEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.AnnulAttendanceRequest
import id.biz.yuka.libperformx.request.CreateExtraWorkRequest
import id.biz.yuka.libperformx.request.ProcessAttendanceRequest
import id.biz.yuka.libperformx.request.RespondExtraWorkRequest
import id.biz.yuka.libperformx.request.RespondManualAttendanceRequest
import id.biz.yuka.libperformx.response.AttendanceDetailResponse
import id.biz.yuka.libperformx.response.AttendanceResponse
import id.biz.yuka.libperformx.response.ScheduleIdResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage

/** Access to record attendance does not approve a request or waive its deductions. */
@kotlinx.serialization.Serializable
data class AttendanceExceptionAccess(
    val offsite_status: String? = null,
    val late_status: String? = null,
    val early_status: String? = null
) {
    val allowsOffsite: Boolean get() = isEligible(offsite_status)
    val allowsLateArrival: Boolean get() = isEligible(late_status)
    val allowsEarlyDeparture: Boolean get() = isEligible(early_status)
    fun hasPendingFor(attendanceType: String): Boolean =
        offsite_status == "pending" ||
            (attendanceType == "check_in" && late_status == "pending") ||
            (attendanceType == "check_out" && early_status == "pending")

    private fun isEligible(status: String?): Boolean = status == "pending" || status == "approved"
}

class AttendanceRepository {

    /** Server scopes requests to this employee/schedule and resolves exception categories. */
    suspend fun getAttendanceExceptionAccess(userId: String, scheduleId: String): ResultState<AttendanceExceptionAccess> = safeSupabaseCall {
        SupabaseConfig.client.postgrest.rpc(
            function = "get_attendance_exception_access",
            parameters = mapOf("p_user_id" to userId, "p_schedule_id" to scheduleId)
        ).decodeAs<AttendanceExceptionAccess>()
    }


    // ==========================================
    // FUNGSI UPLOAD FOTO ABSENSI
    // ==========================================
    suspend fun uploadAttendanceImage(imageBytes: ByteArray, userId: String): ResultState<String> {
        return safeSupabaseCall {
            val fileName = "att_${userId}_${System.currentTimeMillis()}.jpg"
            val bucket = SupabaseConfig.client.storage["attendances"]

            // ✅ PERBAIKAN: Menggunakan parameter biasa seperti di TaskRepository
            bucket.upload(fileName, imageBytes, upsert = false)

            // Kembalikan Public URL
            bucket.publicUrl(fileName)
        }
    }

    // ==========================================
    // FUNGSI MANAJEMEN DATA ABSENSI
    // ==========================================

    // PATTERN 1: Ambil Data Absen Hari Ini (Berdasarkan Schedule)
    suspend fun getAttendanceBySchedule(userId: String, scheduleId: String): ResultState<AttendanceEntity?> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["attendances"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("schedule_id", scheduleId)
                    }
                }
                .decodeSingleOrNull<AttendanceResponse>()

            response?.toEntity()
        }
    }

    // PATTERN 2: Panggil Fungsi RPC untuk Check-In atau Check-Out
    suspend fun processAttendance(request: ProcessAttendanceRequest): ResultState<AttendanceEntity> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest.rpc(
                function = "process_attendance",
                parameters = request
            )

            if (request.p_is_manual_flag) {
                // Jika p_is_manual_flag true, RPC menyimpan data ke tabel manual_attendances (butuh approval),
                // sehingga tabel attendances reguler tidak memiliki baris data untuk dikueri.
                // Kembalikan entity dummy sebagai tanda proses sukses.
                AttendanceEntity(
                    id = "manual_pending",
                    date = java.time.LocalDate.now().toString(),
                    checkInTime = "--:--",
                    checkOutTime = "--:--",
                    statusMasuk = "pending_approval",
                    statusKeluar = "pending_approval",
                    poinDidapat = 0
                )
            } else {
                val response = SupabaseConfig.client.postgrest["attendances"]
                    .select {
                        filter {
                            eq("user_id", request.p_user_id)
                            eq("schedule_id", request.p_schedule_id)
                        }
                    }
                    .decodeSingle<AttendanceResponse>()

                response.toEntity()
            }
        }
    }

    /** MENGAMBIL DAFTAR ABSENSI (Filter Dinamis) */
    suspend fun getAllAttendances(startDate: String, endDate: String, storeId: String? = null): ResultState<List<AttendanceDetailEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["vw_attendance_details"]
                .select {
                    filter {
                        gte("attendance_date", startDate)
                        lte("attendance_date", endDate)
                        if (storeId != null) {
                            eq("store_id", storeId)
                        }
                    }
                }.decodeList<AttendanceDetailResponse>()
            responseList.map { it.toEntity() }
        }
    }



    suspend fun getUserAttendances(userId: String, startDate: String, endDate: String): ResultState<List<AttendanceDetailEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["vw_attendance_details"]
                .select {
                    filter {
                        gte("attendance_date", startDate)
                        lte("attendance_date", endDate)
                        eq("user_id", userId)
                    }
                }.decodeList<AttendanceDetailResponse>()
            responseList.map { it.toEntity() }
        }
    }

    /** MENGAMBIL DETAIL SATU ABSENSI */
    suspend fun getAttendanceDetail(attendanceId: String): ResultState<AttendanceDetailEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["vw_attendance_details"]
                .select { filter { eq("attendance_id", attendanceId) } }
                .decodeSingle<AttendanceDetailResponse>()
            response.toEntity()
        }
    }

    /** MENGANULIR ABSENSI KARYAWAN */
    suspend fun annulAttendance(attendanceId: String, request: AnnulAttendanceRequest): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["attendances"]
                .update(request) { filter { eq("id", attendanceId) } }
            Unit
        }
    }

    suspend fun getMonthlyScheduleCount(userId: String, startDate: String, endDate: String): ResultState<Int> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["vw_schedule_details"]
            .select {
                filter {
                    eq("user_id", userId)
                    gte("work_date", startDate)
                    lte("work_date", endDate)
                }
            }.decodeList<id.biz.yuka.libperformx.response.ScheduleDetailResponse>()
        response.size
    }
    suspend fun getMonthlySchedulesList(userId: String, startDate: String, endDate: String): ResultState<List<String>> = safeSupabaseCall {
        val response = SupabaseConfig.client.postgrest["vw_schedule_details"]
            .select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<id.biz.yuka.libperformx.response.ScheduleDetailResponse>()

        // Filter rentang tanggal (startDate <= work_date <= endDate) & hilangkan duplikasi di level Kotlin
        response
            .map { it.work_date.take(10).trim() }
            .filter { dateStr -> dateStr >= startDate && dateStr <= endDate }
            .distinct()
    }

    /** CEK IZIN ABSEN LUAR RADIUS (OFFSITE) YANG DISETUJUI */
    suspend fun checkHasApprovedOffsiteRequest(userId: String, scheduleId: String): ResultState<Boolean> = safeSupabaseCall {
        val list = SupabaseConfig.client.postgrest["offsite_attendance_requests"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("schedule_id", scheduleId)
                    eq("status", "approved")
                }
            }.decodeList<kotlinx.serialization.json.JsonObject>()
        list.isNotEmpty()
    }

    /** CEK IZIN BYPASS JAM SHIFT YANG DISETUJUI */
    suspend fun checkHasApprovedShiftBypassRequest(userId: String, scheduleId: String): ResultState<Boolean> = safeSupabaseCall {
        val list = SupabaseConfig.client.postgrest["shift_bypass_requests"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("schedule_id", scheduleId)
                    eq("status", "approved")
                }
            }.decodeList<kotlinx.serialization.json.JsonObject>()
        list.isNotEmpty()
    }

    /** MENGAMBIL DAFTAR PENGAJUAN ABSEN MANUAL (APPROVAL MANAGER & OWNER) */
    suspend fun getManualAttendances(approvalStatus: String? = null, storeId: String? = null): ResultState<List<ManualAttendanceEntity>> {
        return safeSupabaseCall {
            val query = SupabaseConfig.client.postgrest["manual_attendances"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, profiles!manual_attendances_user_id_fkey(full_name, profile_url), user_schedules(stores(name))")) {
                    filter {
                        if (!approvalStatus.isNullOrBlank()) {
                            eq("approval_status", approvalStatus)
                        }
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }

            val list = query.decodeList<kotlinx.serialization.json.JsonObject>()
            list.map { json ->
                val id = json["id"]?.toString()?.replace("\"", "") ?: ""
                val userId = json["user_id"]?.toString()?.replace("\"", "") ?: ""
                val scheduleId = json["schedule_id"]?.toString()?.replace("\"", "") ?: ""
                val attendanceDate = json["attendance_date"]?.toString()?.replace("\"", "") ?: ""
                val checkInTime = json["check_in_time"]?.toString()?.replace("\"", "")
                val checkInLat = json["check_in_latitude"]?.toString()?.toDoubleOrNull()
                val checkInLng = json["check_in_longitude"]?.toString()?.toDoubleOrNull()
                val checkInImg = json["check_in_image_url"]?.toString()?.replace("\"", "")
                val checkInStatus = json["check_in_status"]?.toString()?.replace("\"", "")
                val checkOutTime = json["check_out_time"]?.toString()?.replace("\"", "")
                val checkOutLat = json["check_out_latitude"]?.toString()?.toDoubleOrNull()
                val checkOutLng = json["check_out_longitude"]?.toString()?.toDoubleOrNull()
                val checkOutImg = json["check_out_image_url"]?.toString()?.replace("\"", "")
                val checkOutStatus = json["check_out_status"]?.toString()?.replace("\"", "")
                val earnedPoints = json["earned_points"]?.toString()?.toIntOrNull() ?: 0
                val status = json["approval_status"]?.toString()?.replace("\"", "") ?: "pending"
                val approvedBy = json["approved_by"]?.toString()?.replace("\"", "")
                val approvalReason = json["approval_reason"]?.toString()?.replace("\"", "")
                val approvedAt = json["approved_at"]?.toString()?.replace("\"", "")
                val createdAt = json["created_at"]?.toString()?.replace("\"", "")

                val profilesObj = json["profiles"] as? kotlinx.serialization.json.JsonObject
                val userName = profilesObj?.get("full_name")?.toString()?.replace("\"", "") ?: "Karyawan"
                val profileUrl = profilesObj?.get("profile_url")?.toString()?.replace("\"", "") ?: ""

                val scheduleObj = json["user_schedules"] as? kotlinx.serialization.json.JsonObject
                val storeObj = scheduleObj?.get("stores") as? kotlinx.serialization.json.JsonObject
                val storeName = storeObj?.get("name")?.toString()?.replace("\"", "") ?: "Toko"

                ManualAttendanceEntity(
                    id = id,
                    userId = userId,
                    userName = userName,
                    profileUrl = profileUrl,
                    storeName = storeName,
                    scheduleId = scheduleId,
                    date = attendanceDate,
                    checkInTime = checkInTime,
                    checkInLatitude = checkInLat,
                    checkInLongitude = checkInLng,
                    checkInImage = checkInImg,
                    checkInStatus = checkInStatus,
                    checkOutTime = checkOutTime,
                    checkOutLatitude = checkOutLat,
                    checkOutLongitude = checkOutLng,
                    checkOutImage = checkOutImg,
                    checkOutStatus = checkOutStatus,
                    earnedPoints = earnedPoints,
                    approvalStatus = status,
                    approvedBy = approvedBy,
                    approvalReason = approvalReason,
                    approvedAt = approvedAt,
                    createdAt = createdAt
                )
            }
        }
    }

    /** MENYETUJUI ATAU MENOLAK PENGAJUAN ABSEN MANUAL */
    suspend fun respondManualAttendance(id: String, request: RespondManualAttendanceRequest): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["manual_attendances"]
                .update(request) {
                    filter {
                        eq("id", id)
                    }
                }
            Unit
        }
    }

    /** CEK PENGAJUAN ABSEN MANUAL PENDING HARI INI */
    suspend fun checkPendingManualAttendance(userId: String, workDate: String): ResultState<ManualAttendanceEntity?> {
        return safeSupabaseCall {
            val list = getManualAttendances(approvalStatus = "pending").let {
                if (it is ResultState.Success) it.data else emptyList()
            }
            list.firstOrNull { it.userId == userId && it.date == workDate }
        }
    }

    /** MENGIRIM PENGAJUAN MASUK KERJA (EXTRA WORK) */
    suspend fun submitExtraWorkRequest(request: CreateExtraWorkRequest): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["request_schedule_and_attendances"]
                .insert(request)
            Unit
        }
    }

    /** CEK PENGAJUAN MASUK KERJA PENDING HARI INI */
    suspend fun checkPendingExtraWorkRequest(userId: String, workDate: String): ResultState<ExtraWorkRequestEntity?> {
        return safeSupabaseCall {
            val query = SupabaseConfig.client.postgrest["request_schedule_and_attendances"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, profiles!request_schedule_and_attendances_user_id_fkey(full_name, profile_url), stores!request_schedule_and_attendances_store_id_fkey(name), shifts!request_schedule_and_attendances_shift_id_fkey(shift_name, start_time, end_time)")) {
                    filter {
                        eq("user_id", userId)
                        eq("work_date", workDate)
                        eq("status", "pending")
                    }
                }
            val list = query.decodeList<kotlinx.serialization.json.JsonObject>()
            val firstJson = list.firstOrNull() ?: return@safeSupabaseCall null

            parseExtraWorkJson(firstJson)
        }
    }

    /** MENGAMBIL DAFTAR PENGAJUAN MASUK KERJA (APPROVAL MANAGER & OWNER) */
    suspend fun getExtraWorkRequests(statusFilter: String? = null, storeId: String? = null): ResultState<List<ExtraWorkRequestEntity>> {
        return safeSupabaseCall {
            val query = SupabaseConfig.client.postgrest["request_schedule_and_attendances"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, profiles!request_schedule_and_attendances_user_id_fkey(full_name, profile_url), stores!request_schedule_and_attendances_store_id_fkey(name), shifts!request_schedule_and_attendances_shift_id_fkey(shift_name, start_time, end_time)")) {
                    filter {
                        if (!statusFilter.isNullOrBlank()) {
                            eq("status", statusFilter)
                        }
                        if (!storeId.isNullOrBlank()) {
                            eq("store_id", storeId)
                        }
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }

            val list = query.decodeList<kotlinx.serialization.json.JsonObject>()
            list.map { parseExtraWorkJson(it) }
        }
    }

    /** MENYETUJUI ATAU MENOLAK PENGAJUAN MASUK KERJA */
    suspend fun respondExtraWorkRequest(id: String, request: RespondExtraWorkRequest): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["request_schedule_and_attendances"]
                .update(request) {
                    filter {
                        eq("id", id)
                    }
                }
            Unit
        }
    }

    private fun parseExtraWorkJson(json: kotlinx.serialization.json.JsonObject): ExtraWorkRequestEntity {
        val id = json["id"]?.toString()?.replace("\"", "") ?: ""
        val userId = json["user_id"]?.toString()?.replace("\"", "") ?: ""
        val storeId = json["store_id"]?.toString()?.replace("\"", "") ?: ""
        val shiftId = json["shift_id"]?.toString()?.replace("\"", "") ?: ""
        val workDate = json["work_date"]?.toString()?.replace("\"", "") ?: ""
        val checkInTime = json["check_in_time"]?.toString()?.replace("\"", "") ?: ""
        val checkInLat = json["check_in_latitude"]?.toString()?.toDoubleOrNull() ?: 0.0
        val checkInLng = json["check_in_longitude"]?.toString()?.toDoubleOrNull() ?: 0.0
        val checkInImg = json["check_in_image_url"]?.toString()?.replace("\"", "") ?: ""
        val reason = json["reason"]?.toString()?.replace("\"", "") ?: ""
        val status = json["status"]?.toString()?.replace("\"", "") ?: "pending"
        val approvedBy = json["approved_by"]?.toString()?.replace("\"", "")
        val approvalNotes = json["approval_notes"]?.toString()?.replace("\"", "")
        val approvedAt = json["approved_at"]?.toString()?.replace("\"", "")
        val createdAt = json["created_at"]?.toString()?.replace("\"", "")

        val profilesObj = json["profiles"] as? kotlinx.serialization.json.JsonObject
        val userName = profilesObj?.get("full_name")?.toString()?.replace("\"", "") ?: "Karyawan"
        val profileUrl = profilesObj?.get("profile_url")?.toString()?.replace("\"", "") ?: ""

        val storeObj = json["stores"] as? kotlinx.serialization.json.JsonObject
        val storeName = storeObj?.get("name")?.toString()?.replace("\"", "") ?: "Toko"

        val shiftObj = json["shifts"] as? kotlinx.serialization.json.JsonObject
        val shiftName = shiftObj?.get("shift_name")?.toString()?.replace("\"", "")
            ?: shiftObj?.get("name")?.toString()?.replace("\"", "")
            ?: "Shift"
        val shiftStart = shiftObj?.get("start_time")?.toString()?.replace("\"", "")
        val shiftEnd = shiftObj?.get("end_time")?.toString()?.replace("\"", "")
        val shiftRange = if (shiftStart != null && shiftEnd != null) {
            "${shiftStart.take(5)} - ${shiftEnd.take(5)}"
        } else "-"

        return ExtraWorkRequestEntity(
            id = id,
            userId = userId,
            userName = userName,
            profileUrl = profileUrl,
            storeId = storeId,
            storeName = storeName,
            shiftId = shiftId,
            shiftName = shiftName,
            shiftTimeRange = shiftRange,
            workDate = workDate,
            checkInTime = checkInTime,
            checkInLatitude = checkInLat,
            checkInLongitude = checkInLng,
            checkInImageUrl = checkInImg,
            reason = reason,
            status = status,
            approvedBy = approvedBy,
            approvalNotes = approvalNotes,
            approvedAt = approvedAt,
            createdAt = createdAt
        )
    }
}

