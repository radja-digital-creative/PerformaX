package id.biz.yuka.libperformx.response

import id.biz.yuka.libperformx.entity.KpiEntity
import kotlinx.serialization.Serializable

@Serializable
data class KpiResponse(
    val user_id: String,
    val full_name: String,
    val role_name: String,
    val task_points: Int,
    val attendance_score: Int,
    val total_kpi_score: Int,
    val rank: Int
)


fun KpiResponse.toEntity() = KpiEntity(
    userId = this.user_id,
    employeeName = this.full_name,
    roleName = this.role_name,
    taskPoints = this.task_points,
    attendanceScore = this.attendance_score,
    totalKpiScore = this.total_kpi_score,
    rank = this.rank
)