package id.biz.yuka.libperformx.response

import kotlinx.serialization.Serializable

@Serializable
data class TaskCreatorResponse(
    val user_id: String,
    val name: String,
    val profile_url: String? = null // TAMBAHAN: URL Foto Profil Creator
)

@Serializable
data class TaskLabelResponse(
    val id: String,
    val name: String,
    val color_hex: String? = null
)

@Serializable
data class TaskAssigneeResponse(
    val assignment_id: String,
    val user_id: String,
    val user_name: String,
    val profile_url: String? = null, // TAMBAHAN: URL Foto Profil Assignee
    val store_name: String? = null,
    val status: String,
    val completed_at: String? = null,
    val earned_points: Int? = 0,
    val checked_by: TaskCreatorResponse? = null // ✅ DATA BARU UNTUK VERIFIER
)

@Serializable
data class TaskAttachmentResponse(
    val id: String,
    val uploaded_by_name: String,
    val image_url: String,
    val image_type: String
)

@Serializable
data class TaskCommentResponse(
    val comment_id: String,
    val user_id: String,
    val user_name: String,
    val profile_url: String? = null, // TAMBAHAN: URL Foto Profil Pengirim Komentar
    val comment_text: String,
    val created_at: String,
    val comment_images: List<String> = emptyList()
)

// Response Utama yang membungkus semuanya
@Serializable
data class TaskResponse(
    val task_id: String,
    val title: String,
    val description: String? = null,
    val due_date: String,
    val created_at: String,
    val updated_at: String? = null,

    // ✅ TAMBAHAN: Penanda apakah ini tugas hasil copy/paste otomatis
    val routine_id: String? = null,

    val created_by: TaskCreatorResponse,
    val labels: List<TaskLabelResponse> = emptyList(),
    val assignees: List<TaskAssigneeResponse> = emptyList(),
    val attachments: List<TaskAttachmentResponse> = emptyList(),
    val comments: List<TaskCommentResponse> = emptyList()
)

// Digunakan oleh getAllTaskRoutines untuk join singkat ke task_templates
@Serializable
data class RoutineParentTaskResponse(
    val title: String,
    val created_by: String,
    val profiles: RoutineCreatorNestedResponse? = null, // ✅ Mengacu ke tabel profiles
    val task_template_assignees: List<RoutineAssigneeNestedResponse> = emptyList() // ✅ Pindah ke sini karena relasi lewat task_templates
)

@Serializable
data class RoutineCreatorNestedResponse(
    val full_name: String,
    val profile_url: String? = null
)

@Serializable
data class RoutineAssigneeNestedResponse(
    val assigned_to: String, // ✅ Sesuai kolom di DB
    val profiles: RoutineCreatorNestedResponse? = null
)

@Serializable
data class TaskRoutineResponse(
    val id: String,
    val template_task_id: String,
    val frequency: String,
    val interval: Int,
    val days_of_week: List<Int>? = null,
    val days_of_month: List<Int>? = null,
    val next_run_date: String,
    val last_generated_at: String? = null,
    val is_active: Boolean,
    val is_infinite: Boolean = false, // ✅ BARU
    val created_at: String,
    val task_templates: RoutineParentTaskResponse? = null
)

// ============================================================
// Response untuk RPC get_routine_detail
// ============================================================

@Serializable
data class RoutineDetailScheduleResponse(
    val id: String,
    val template_task_id: String,
    val frequency: String,
    val interval: Int,
    val days_of_week: List<Int>? = null,
    val days_of_month: List<Int>? = null,
    val next_run_date: String,
    val last_generated_date: String? = null,
    val deadline_offset: Int = 0,
    val is_active: Boolean,
    val is_infinite: Boolean = false, // ✅ BARU
    val created_at: String
)

@Serializable
data class RoutineDetailTemplateResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val is_task_team: Boolean = true,
    val created_at: String
)

@Serializable
data class RoutineDetailAssigneeResponse(
    val id: String,
    val user_id: String,
    val user_name: String,
    val profile_url: String? = null,
    val store_id: String? = null,
    val store_name: String? = null
)

@Serializable
data class RoutineDetailLabelResponse(
    val id: String,
    val name: String,
    val color_hex: String? = null
)

@Serializable
data class RoutineDetailImageResponse(
    val id: String,
    val image_url: String,
    val image_type: String
)

@Serializable
data class RoutineDetailResponse(
    val routine: RoutineDetailScheduleResponse,
    val template: RoutineDetailTemplateResponse,
    val assignees: List<RoutineDetailAssigneeResponse> = emptyList(),
    val labels: List<RoutineDetailLabelResponse> = emptyList(),
    val images: List<RoutineDetailImageResponse> = emptyList()
)

@Serializable
data class TaskUrgentResponse(
    val id: String,
    val task_id: String,
    val reminder_type: String,
    val interval_value: Int,
    val is_active: Boolean,
    val created_at: String,
    val last_reminded_at: String? = null,
    val reminders_sent_today: Int = 0,
    // ✅ Menangkap hasil Join dari tabel tasks
    val tasks: RoutineParentTaskResponse? = null
)

@Serializable
data class EmployeeTaskReportResponse(
    val employee_name: String,
    val task_title: String,
    val deadline: String,
    val created_at: String,
    val status: String
)
