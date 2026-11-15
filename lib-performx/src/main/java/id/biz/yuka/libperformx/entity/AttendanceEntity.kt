package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.AttendanceDetailResponse
import id.biz.yuka.libperformx.response.AttendanceResponse

data class AttendanceEntity(
    val id: String,
    val date: String,
    val checkInTime: String,
    val checkOutTime: String,
    val statusMasuk: String,
    val statusKeluar: String,
    val poinDidapat: Int
)

// Standar Mapper
fun AttendanceResponse.toEntity(): AttendanceEntity {
    return AttendanceEntity(
        id = this.id,
        date = this.attendance_date,
        checkInTime = this.check_in_time ?: "--:--",
        checkOutTime = this.check_out_time ?: "--:--",
        statusMasuk = this.check_in_status,
        statusKeluar = this.check_out_status,
        poinDidapat = this.earned_points
    )
}

data class AttendanceDetailEntity(
    val id: String,
    val userId: String,
    val userName: String,
    val storeId: String,
    val storeName: String,
    val date: String,
    val checkInTime: String,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkInStatus: String,
    val checkInImage: String,
    val checkOutTime: String,
    val checkOutLatitude: Double,
    val checkOutLongitude: Double,
    val checkOutStatus: String,
    val checkOutImage: String,
    val isAnnulled: Boolean,
    val annulReason: String,
    val annulledByName: String
)

fun AttendanceDetailResponse.toEntity() = AttendanceDetailEntity(
    id = this.attendance_id,
    userId = this.user_id,
    userName = this.user_name,
    storeId = this.store_id,
    storeName = this.store_name,
    date = this.attendance_date,
    checkInTime = this.check_in_time ?: "-",
    checkInStatus = this.check_in_status ?: "Belum Absen",
    checkInLatitude = this.check_in_latitude ?: 0.0,
    checkInLongitude = this.check_in_longitude ?: 0.0,
    checkInImage = this.check_in_image_url ?: "",
    checkOutTime = this.check_out_time ?: "-",
    checkOutStatus = this.check_out_status ?: "Belum Absen",
    checkOutLatitude = this.check_out_latitude ?: 0.0,
    checkOutLongitude = this.check_out_longitude ?: 0.0,
    checkOutImage = this.check_out_image_url ?: "",
    isAnnulled = this.is_annulled,
    annulReason = this.annul_reason ?: "-",
    annulledByName = this.annulled_by_name ?: "-"
)