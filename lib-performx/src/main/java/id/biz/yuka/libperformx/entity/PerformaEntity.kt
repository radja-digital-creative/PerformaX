package id.biz.yuka.libperformx.entity

// 1. Entity untuk Summary (Home Screen)
data class SummaryPerformaEntity(
    val status: String,
    val title: String,
    val description: String,
    val colorTheme: String
)

// 2. Entity untuk Detail (Halaman Rincian)
data class DetailPerformaEntity(
    val attendance: AttendanceMetricsEntity,
    val tasks: TaskMetricsEntity,
    val leaves: Int,
    val kpi: KpiMetricsEntity
)

data class AttendanceMetricsEntity(
    val onTime: Int,
    val late: Int,
    val earlyLeave: Int,
    val overtimeDays: Int
)

data class TaskMetricsEntity(
    val completedOnTime: Int,
    val lateSubmission: Int,
    val interactionScore: Int
)

data class KpiMetricsEntity(
    val currentScore: Int,
    val lastMonthScore: Int,
    val growthPercentage: Int
)