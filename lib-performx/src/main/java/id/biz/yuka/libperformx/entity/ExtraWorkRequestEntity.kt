package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.ExtraWorkRequestResponse

data class ExtraWorkRequestEntity(
    val id: String,
    val userId: String,
    val userName: String,
    val profileUrl: String,
    val storeId: String,
    val storeName: String,
    val shiftId: String,
    val shiftName: String,
    val shiftTimeRange: String,
    val workDate: String,
    val checkInTime: String,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkInImageUrl: String,
    val reason: String,
    val status: String,
    val approvedBy: String?,
    val approvalNotes: String?,
    val approvedAt: String?,
    val createdAt: String?
)

fun ExtraWorkRequestResponse.toEntity() = ExtraWorkRequestEntity(
    id = this.id,
    userId = this.user_id,
    userName = this.user_name ?: "Karyawan",
    profileUrl = this.profile_url ?: "",
    storeId = this.store_id,
    storeName = this.store_name ?: "Toko",
    shiftId = this.shift_id,
    shiftName = this.shift_name ?: "Shift",
    shiftTimeRange = if (this.shift_start_time != null && this.shift_end_time != null) {
        "${this.shift_start_time.take(5)} - ${this.shift_end_time.take(5)}"
    } else "-",
    workDate = this.work_date,
    checkInTime = this.check_in_time,
    checkInLatitude = this.check_in_latitude,
    checkInLongitude = this.check_in_longitude,
    checkInImageUrl = this.check_in_image_url,
    reason = this.reason,
    status = this.status,
    approvedBy = this.approved_by,
    approvalNotes = this.approval_notes,
    approvedAt = this.approved_at,
    createdAt = this.created_at
)
