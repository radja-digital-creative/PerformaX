package id.biz.yuka.libperformx.entity

data class KpiEntity(
    val userId: String,
    val employeeName: String,
    val roleName: String,
    val taskPoints: Int,
    val attendanceScore: Int,
    val totalKpiScore: Int,
    val rank: Int
)
