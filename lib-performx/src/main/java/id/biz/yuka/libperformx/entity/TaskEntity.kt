package id.biz.yuka.libperformx.entity

import id.biz.yuka.libperformx.response.TaskResponse
import id.biz.yuka.libperformx.response.TaskRoutineResponse

data class TaskAssigneeEntity(
    val assignmentId: String,
    val userId: String,
    val userName: String,
    val profileUrl: String?, // TAMBAHAN 1: Foto profil penerima tugas
    val storeName: String,
    val status: String,
    val completedAt: String?,
    val isCompleted: Boolean,
    val earnedPoints: Int,
    val checkedBy: CheckedByEntity? = null // ✅ DATA BARU UNTUK VERIFIER
)

data class CheckedByEntity(
    val userId: String,
    val name: String,
    val profileUrl: String?
)

data class TaskLabelEntity(
    val id: String,
    val name: String,
    val colorHex: String
)

data class TaskAttachmentEntity(
    val id: String,
    val uploaderName: String,
    val imageUrl: String,
    val imageType: String
)

data class TaskCommentEntity(
    val commentId: String,
    val userId: String,
    val userName: String,
    val userProfileUrl: String?, // TAMBAHAN 2: Foto profil pengirim komentar
    val commentText: String,
    val createdAt: String,
    val commentImages: List<String>
)

data class TaskEntity(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: String,
    val createdAt: String,
    val routineId: String?,
    val isRoutine: Boolean,
    val creatorId: String,
    val creatorName: String,
    val creatorProfileUrl: String?, // TAMBAHAN 3: Foto profil pembuat tugas
    val assignees: List<TaskAssigneeEntity>,
    val labels: List<TaskLabelEntity>,
    val attachments: List<TaskAttachmentEntity>,
    val comments: List<TaskCommentEntity>
)

// Extension Function (Sang Mapper)
fun TaskResponse.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.task_id,
        title = this.title,
        description = this.description ?: "",
        dueDate = this.due_date,
        createdAt = this.created_at,
        routineId = this.routine_id,
        isRoutine = this.routine_id != null,
        // Ekstrak langsung dari object JSON
        creatorId = this.created_by.user_id,
        creatorName = this.created_by.name,
        creatorProfileUrl = this.created_by.profile_url, // MAPPING 1

        assignees = this.assignees.map {
            TaskAssigneeEntity(
                assignmentId = it.assignment_id,
                userId = it.user_id,
                userName = it.user_name,
                profileUrl = it.profile_url, // MAPPING 2
                storeName = it.store_name ?: "-",
                status = it.status,
                completedAt = it.completed_at,
                isCompleted = it.completed_at != null,
                earnedPoints = it.earned_points ?: 0,
                checkedBy = it.checked_by?.let { cb ->
                    CheckedByEntity(cb.user_id, cb.name, cb.profile_url)
                }
            )
        },
        labels = this.labels.map { TaskLabelEntity(it.id, it.name, it.color_hex ?: "#808080") },
        attachments = this.attachments.map { TaskAttachmentEntity(it.id, it.uploaded_by_name, it.image_url, it.image_type) },
        comments = this.comments.map {
            TaskCommentEntity(
                commentId = it.comment_id,
                userId = it.user_id,
                userName = it.user_name,
                userProfileUrl = it.profile_url, // MAPPING 3

                commentText = it.comment_text,
                createdAt = it.created_at,
                commentImages = it.comment_images
            )
        }
    )
}

data class TaskRoutineEntity(
    val id: String,
    val templateTaskId: String,
    val frequency: String,
    val interval: Int,
    val daysOfWeek: List<Int>,
    val daysOfMonth: List<Int>,
    val nextRunDate: String,
    val lastGeneratedAt: String?,
    val isActive: Boolean,
    val isInfinite: Boolean, // ✅ BARU
    val createdAt: String,

    // ✅ Data hasil ekstrak
    val taskTitle: String,
    val creatorId: String,
    val creatorName: String,
    val creatorProfileUrl: String,
    val assigneeIds: List<String>, // ✅ Tambahan ID Assignee
    val assigneeProfiles: List<String> 
)

// Extension Function Mapper
fun TaskRoutineResponse.toEntity() = TaskRoutineEntity(
    id = this.id,
    templateTaskId = this.template_task_id,
    frequency = this.frequency,
    interval = this.interval,
    daysOfWeek = this.days_of_week ?: emptyList(),
    daysOfMonth = this.days_of_month ?: emptyList(),
    nextRunDate = this.next_run_date,
    lastGeneratedAt = this.last_generated_at,
    isActive = this.is_active,
    isInfinite = this.is_infinite, // ✅ MAPPING BARU
    createdAt = this.created_at,

    taskTitle = this.task_templates?.title ?: "Tugas Tanpa Judul",
    creatorId = this.task_templates?.created_by ?: "",
    creatorName = this.task_templates?.profiles?.full_name ?: "-",
    creatorProfileUrl = this.task_templates?.profiles?.profile_url ?: "",
    assigneeIds = this.task_templates?.task_template_assignees?.map { it.assigned_to } ?: emptyList(),
    assigneeProfiles = this.task_templates?.task_template_assignees?.mapNotNull { it.profiles?.profile_url } ?: emptyList()
)

// ============================================================
// Entity untuk RPC get_routine_detail
// ============================================================

data class TaskTemplateAssigneeEntity(
    val id: String,
    val userId: String,
    val userName: String,
    val profileUrl: String?,
    val storeId: String?,
    val storeName: String?
)

data class TaskTemplateLabelEntity(
    val id: String,
    val name: String,
    val colorHex: String
)

data class TaskTemplateImageEntity(
    val id: String,
    val imageUrl: String,
    val imageType: String
)

data class RoutineDetailEntity(
    // Schedule
    val routineId: String,
    val templateTaskId: String,
    val frequency: String,
    val interval: Int,
    val daysOfWeek: List<Int>,
    val daysOfMonth: List<Int>,
    val nextRunDate: String,
    val lastGeneratedDate: String?,
    val deadlineOffset: Int,
    val isActive: Boolean,
    val isInfinite: Boolean, // ✅ BARU
    val routineCreatedAt: String,
    // Template
    val templateId: String,
    val title: String,
    val description: String,
    val isTaskTeam: Boolean,
    // Relations
    val assignees: List<TaskTemplateAssigneeEntity>,
    val labels: List<TaskTemplateLabelEntity>,
    val images: List<TaskTemplateImageEntity>
)

fun id.biz.yuka.libperformx.response.RoutineDetailResponse.toEntity() = RoutineDetailEntity(
    routineId         = routine.id,
    templateTaskId    = routine.template_task_id,
    frequency         = routine.frequency,
    interval          = routine.interval,
    daysOfWeek        = routine.days_of_week ?: emptyList(),
    daysOfMonth       = routine.days_of_month ?: emptyList(),
    nextRunDate       = routine.next_run_date,
    lastGeneratedDate = routine.last_generated_date,
    deadlineOffset    = routine.deadline_offset,
    isActive          = routine.is_active,
    isInfinite        = routine.is_infinite, // ✅ MAPPING BARU
    routineCreatedAt  = routine.created_at,
    templateId        = template.id,
    title             = template.title,
    description       = template.description ?: "",
    isTaskTeam        = template.is_task_team,
    assignees         = assignees.map {
        TaskTemplateAssigneeEntity(it.id, it.user_id, it.user_name, it.profile_url, it.store_id, it.store_name)
    },
    labels            = labels.map {
        TaskTemplateLabelEntity(it.id, it.name, it.color_hex ?: "#808080")
    },
    images            = images.map {
        TaskTemplateImageEntity(it.id, it.image_url, it.image_type)
    }
)

data class TaskUrgentEntity(
    val id: String,
    val taskId: String,
    val reminderType: String,
    val intervalValue: Int,
    val isActive: Boolean,
    val createdAt: String,
    val lastRemindedAt: String?,
    val remindersSentToday: Int,

    // ✅ Data hasil ekstrak
    val taskTitle: String,
    val creatorId: String,
    val creatorName: String,
    val creatorProfileUrl: String,
    val assigneeIds: List<String>, // ✅ Tambahan ID Assignee
    val assigneeProfiles: List<String>
)

fun id.biz.yuka.libperformx.response.TaskUrgentResponse.toEntity() = TaskUrgentEntity(
    id = this.id,
    taskId = this.task_id,
    reminderType = this.reminder_type,
    intervalValue = this.interval_value,
    isActive = this.is_active,
    createdAt = this.created_at,
    lastRemindedAt = this.last_reminded_at,
    remindersSentToday = this.reminders_sent_today,

    taskTitle = this.tasks?.title ?: "Tugas Tanpa Judul",
    creatorId = this.tasks?.created_by ?: "",
    creatorName = this.tasks?.profiles?.full_name ?: "-",
    creatorProfileUrl = this.tasks?.profiles?.profile_url ?: "",
    assigneeIds = this.tasks?.task_template_assignees?.map { it.assigned_to } ?: emptyList(),
    assigneeProfiles = this.tasks?.task_template_assignees?.mapNotNull { it.profiles?.profile_url } ?: emptyList()
)

data class EmployeeTaskReportEntity(
    val employeeName: String,
    val taskTitle: String,
    val deadline: String,
    val createdAt: String,
    val status: String
)

fun id.biz.yuka.libperformx.response.EmployeeTaskReportResponse.toEntity() = EmployeeTaskReportEntity(
    employeeName = employee_name,
    taskTitle = task_title,
    deadline = deadline,
    createdAt = created_at,
    status = status
)
