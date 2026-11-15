package id.biz.yuka.libperformx.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskRequest(
    val p_title: String,
    val p_description: String,
    val p_due_date: String, // ISO 8601. Untuk routine: now() + dueDay
    val p_store_id: String,
    val p_assigned_to_ids: List<String>,
    val p_label_ids: List<String> = emptyList(),
    val p_image_urls: List<String> = emptyList(),
    val p_is_task_team: Boolean, // ✅ BARU: Mode tim (true) atau individu (false)

    // ✅ PARAMETER RUTIN (Opsional)
    val p_is_routine: Boolean,
    val p_routine_frequency: String? = null,
    val p_routine_interval: Int?,
    val p_routine_days_of_week: List<Int>? = null,
    val p_routine_days_of_month: List<Int>? = null,
    val p_routine_deadline_offset: Int?,
    val p_routine_is_infinite: Boolean, // ✅ BARU

    // ✅ PARAMETER URGENT (Opsional)
    val p_is_urgent: Boolean,
    val p_urgent_reminder_type: String? = null,
    val p_urgent_interval: Int?
)

@Serializable
data class UpdateTaskRequest(
    val p_task_id: String,
    val p_title: String,
    val p_description: String,
    val p_due_date: String,
    val p_store_id: String,
    val p_assigned_to_ids: List<String>,
    val p_label_ids: List<String> = emptyList(),
    val p_image_urls: List<String> = emptyList()
)


@Serializable
data class CompleteTaskAssignmentRequest(
    val status: String = "completed",
    val completed_at: String
)

// ✅ TAMBAHAN BARU: Request Khusus untuk Mendaftarkan Jadwal Rutin
@Serializable
data class CreateTaskRoutineRequest(
    val template_task_id: String,
    val frequency: String, // Wajib diisi "DAILY", "WEEKLY", atau "MONTHLY"
    val interval: Int,
    val days_of_week: List<Int>? = null,
    val days_of_month: List<Int>? = null,
    val deadline_offset: Int,
    val is_infinite: Boolean,
    val next_run_date: String // Format YYYY-MM-DD
)


@Serializable
data class UpdateRoutineScheduleRequest(
    val p_routine_id: String,
    val p_frequency: String,
    val p_interval: Int,
    val p_days_of_week: List<Int>? = null,
    val p_days_of_month: List<Int>? = null,
    val p_deadline_offset: Int,
    val p_is_infinite: Boolean
)

// ✅ Request untuk update template + replace assignees & labels
@Serializable
data class UpdateTaskTemplateRequest(
    val p_template_id: String,
    val p_title: String,
    val p_description: String? = null,
    val p_is_task_team: Boolean = true,
    val p_assigned_to_ids: List<String> = emptyList(),
    val p_store_id: String? = null,
    val p_label_ids: List<String> = emptyList()
)

// ✅ Request untuk verifikasi tugas massal via RPC
@Serializable
data class VerifyMultipleTasksRequest(
    val p_assignment_ids: List<String>,
    val p_checked_by: String
)
