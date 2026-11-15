package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.ScheduleResponse
import id.biz.yuka.libperformx.response.ShiftResponse

data class ScheduleEntity(
    val id: String,
    val storeId: String,
    val shiftId: String,
    val workDate: String,
    val notes: String,
    val shifts: ShiftEntity? // ✅ Tambahkan ini
)

// Standar Mapper (Update agar bisa memetakan relasi shift)
fun ScheduleResponse.toEntity(): ScheduleEntity {
    return ScheduleEntity(
        id = this.id,
        storeId = this.store_id,
        shiftId = this.shift_id,
        workDate = this.work_date,
        notes = this.notes ?: "",
        shifts = this.shifts?.toEntity() // ✅ Petakan data relasi shift
    )
}

data class ShiftEntity(
    val id: String,
    val storeId: String,
    val shiftName: String,
    val startTime: String,
    val endTime: String,
    val attendanceRadius: Int,
    val isActive: Boolean
)

fun ShiftResponse.toEntity(): ShiftEntity {
    return ShiftEntity(
        id = this.id,
        storeId = this.store_id ?: "",
        shiftName = this.shift_name,
        startTime = this.start_time,
        endTime = this.end_time,
        attendanceRadius = this.attendance_radius,
        isActive = this.is_active
    )
}