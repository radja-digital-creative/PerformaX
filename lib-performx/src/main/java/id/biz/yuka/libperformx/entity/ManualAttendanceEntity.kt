package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.ManualAttendanceResponse

data class ManualAttendanceEntity(
    val id: String,
    val userId: String,
    val userName: String,
    val profileUrl: String,
    val storeName: String,
    val scheduleId: String,
    val date: String,
    
    // Check In Data
    val checkInTime: String?,
    val checkInLatitude: Double?,
    val checkInLongitude: Double?,
    val checkInImage: String?,
    val checkInStatus: String?,

    // Check Out Data
    val checkOutTime: String?,
    val checkOutLatitude: Double?,
    val checkOutLongitude: Double?,
    val checkOutImage: String?,
    val checkOutStatus: String?,

    val earnedPoints: Int,
    val approvalStatus: String,
    val approvedBy: String?,
    val approvalReason: String?,
    val approvedAt: String?,
    val createdAt: String?
)

fun ManualAttendanceResponse.toEntity() = ManualAttendanceEntity(
    id = this.id,
    userId = this.user_id,
    userName = this.user_name ?: "Karyawan",
    profileUrl = this.profile_url ?: "",
    storeName = this.store_name ?: "Toko",
    scheduleId = this.schedule_id ?: "",
    date = this.attendance_date,
    checkInTime = this.check_in_time,
    checkInLatitude = this.check_in_latitude,
    checkInLongitude = this.check_in_longitude,
    checkInImage = this.check_in_image_url,
    checkInStatus = this.check_in_status,
    checkOutTime = this.check_out_time,
    checkOutLatitude = this.check_out_latitude,
    checkOutLongitude = this.check_out_longitude,
    checkOutImage = this.check_out_image_url,
    checkOutStatus = this.check_out_status,
    earnedPoints = this.earned_points ?: 0,
    approvalStatus = this.approval_status,
    approvedBy = this.approved_by,
    approvalReason = this.approval_reason,
    approvedAt = this.approved_at,
    createdAt = this.created_at
)
