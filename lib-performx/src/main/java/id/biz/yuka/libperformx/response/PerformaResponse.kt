package id.biz.yuka.libperformx.response

import id.biz.yuka.libperformx.entity.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 1. Response untuk Summary
@Serializable
data class SummaryPerformaResponse(
    val status: String = "neutral",
    val title: String = "",
    val description: String = "",
    @SerialName("color_theme") val colorTheme: String = "WonderGrey"
) {
    fun toEntity() = SummaryPerformaEntity(
        status = status,
        title = title,
        description = description,
        colorTheme = colorTheme
    )
}

// 2. Response untuk Detail
@Serializable
data class DetailPerformaResponse(
    val attendance: AttendanceMetricsResponse,
    val tasks: TaskMetricsResponse,
    val leaves: Int = 0,
    val kpi: KpiMetricsResponse
) {
    fun toEntity() = DetailPerformaEntity(
        attendance = attendance.toEntity(),
        tasks = tasks.toEntity(),
        leaves = leaves,
        kpi = kpi.toEntity()
    )
}

@Serializable
data class AttendanceMetricsResponse(
    @SerialName("on_time") val onTime: Int = 0,
    val late: Int = 0,
    @SerialName("early_leave") val earlyLeave: Int = 0,
    @SerialName("overtime_days") val overtimeDays: Int = 0
) {
    fun toEntity() = AttendanceMetricsEntity(onTime, late, earlyLeave, overtimeDays)
}

@Serializable
data class TaskMetricsResponse(
    @SerialName("completed_on_time") val completedOnTime: Int = 0,
    @SerialName("late_submission") val lateSubmission: Int = 0,
    @SerialName("interaction_score") val interactionScore: Int = 0
) {
    fun toEntity() = TaskMetricsEntity(completedOnTime, lateSubmission, interactionScore)
}

@Serializable
data class KpiMetricsResponse(
    @SerialName("current_score") val currentScore: Int = 0,
    @SerialName("last_month_score") val lastMonthScore: Int = 0,
    @SerialName("growth_percentage") val growthPercentage: Int = 0
) {
    fun toEntity() = KpiMetricsEntity(currentScore, lastMonthScore, growthPercentage)
}