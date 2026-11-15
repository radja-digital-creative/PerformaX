package id.biz.yuka.libperformx.repository

import android.util.Log
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.libperformx.entity.TaskRoutineEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.CreateTaskRequest
import id.biz.yuka.libperformx.request.CreateTaskRoutineRequest
import id.biz.yuka.libperformx.request.UpdateRoutineScheduleRequest
import id.biz.yuka.libperformx.request.UpdateTaskRequest
import id.biz.yuka.libperformx.request.UpdateTaskTemplateRequest
import id.biz.yuka.libperformx.request.VerifyMultipleTasksRequest
import id.biz.yuka.libperformx.response.RoutineDetailResponse
import id.biz.yuka.libperformx.response.TaskLabelResponse
import id.biz.yuka.libperformx.response.TaskResponse
import id.biz.yuka.libperformx.response.TaskRoutineResponse
import id.biz.yuka.libperformx.response.TaskUrgentResponse
import id.biz.yuka.libperformx.response.EmployeeTaskReportResponse
import id.biz.yuka.libperformx.entity.RoutineDetailEntity
import id.biz.yuka.libperformx.entity.EmployeeTaskReportEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.OffsetDateTime
import java.util.UUID

class TaskRepository {

    suspend fun getTasks(
        filterByUserId: String? = null,
        filterByToday: Boolean = false,
        filterByOverdue: Boolean = false
    ): ResultState<List<TaskEntity>> {
        return safeSupabaseCall {
            val todayStr = OffsetDateTime.now().toLocalDate().toString()

            val responseList = SupabaseConfig.client.postgrest["vw_task_details"]
                .select {
                    filter {
                        if (filterByToday) {
                            gte("due_date", "${todayStr}T00:00:00")
                            lte("due_date", "${todayStr}T23:59:59")
                        } else if (filterByOverdue) {
                            lte("due_date", "${todayStr}T23:59:59")
                        }
                    }
                    order(column = "updated_at", order = Order.DESCENDING)
                }
                .decodeList<TaskResponse>()

            var mappedList = responseList.map { it.toEntity() }

            if (filterByUserId != null) {
                mappedList = mappedList.filter { task ->
                    task.creatorId == filterByUserId || task.assignees.any { it.userId == filterByUserId }
                }
            }

            if (filterByToday) {
                mappedList = mappedList.filter { task ->
                    task.dueDate.startsWith(todayStr)
                }
            }

            if (filterByOverdue) {
                mappedList = mappedList.filter { task ->
                    val isDueTodayOrPast = task.dueDate.take(10) <= todayStr
                    val isNotVerified = if (filterByUserId != null) {
                        task.assignees.filter { it.userId == filterByUserId }.any { it.status != "verified" }
                    } else {
                        task.assignees.any { it.status != "verified" }
                    }
                    isDueTodayOrPast && isNotVerified
                }
            }

            mappedList
        }
    }

    suspend fun getTaskDetail(taskId: String): ResultState<TaskEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["vw_task_details"]
                .select { filter { eq("task_id", taskId) } }
                .decodeSingle<TaskResponse>()
            response.toEntity()
        }
    }

    suspend fun uploadImages(
        images: List<ByteArray>,
        bucketName: String = "tasks"
    ): ResultState<List<String>> {
        return safeSupabaseCall {
            coroutineScope {
                val storageBucket = SupabaseConfig.client.storage[bucketName]
                val deferredUploads = images.map { imageBytes ->
                    async {
                        val fileName = "img_${UUID.randomUUID()}.jpg"
                        storageBucket.upload(fileName, imageBytes, upsert = false)
                        storageBucket.publicUrl(fileName)
                    }
                }
                deferredUploads.awaitAll()
            }
        }
    }

    suspend fun createTask(request: CreateTaskRequest): ResultState<String> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest.rpc(
                function = "create_new_task",
                parameters = request
            ).decodeAs<String>()
        }
    }

    suspend fun createTaskWithImages(
        request: CreateTaskRequest,
        imageBytes: List<ByteArray>
    ): ResultState<String> {
        val imageUrls = mutableListOf<String>()

        if (imageBytes.isNotEmpty()) {
            when (val uploadResult = uploadImages(imageBytes)) {
                is ResultState.Success -> imageUrls.addAll(uploadResult.data)
                is ResultState.Error -> return ResultState.Error("Gagal mengunggah gambar: ${uploadResult.message}")
                else -> return ResultState.Error("Unknown error saat upload")
            }
        }

        val finalRequest = request.copy(p_image_urls = request.p_image_urls + imageUrls)
        return createTask(finalRequest)
    }

    suspend fun updateTask(request: UpdateTaskRequest): ResultState<Boolean> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest.rpc(
                function = "update_task",
                parameters = request
            ).decodeAs<Boolean>()
        }
    }

    suspend fun updateAssignmentStatus(assignmentId: String, newStatus: String): ResultState<Unit> {
        return try {
            val user = SupabaseConfig.client.auth.currentUserOrNull()
            val currentUserId = user?.id
            
            android.util.Log.d("DEBUG_AUTH", "Updating assignment: id=$assignmentId, newStatus=$newStatus")
            android.util.Log.d("DEBUG_AUTH", "Current Auth User ID in App: $currentUserId")
            android.util.Log.d("DEBUG_AUTH", "Current Auth User Email: ${user?.email}")

            val payload = mutableMapOf<String, String?>("status" to newStatus)
            payload["checked_by"] = currentUserId

            if (newStatus == "completed" || newStatus == "verified") {
                payload["completed_at"] = java.time.OffsetDateTime.now().toString()
            } else {
                payload["completed_at"] = null
            }

            val response = SupabaseConfig.client.postgrest["task_assignments"]
                .update(payload) { 
                    select()
                    filter { eq("id", assignmentId) } 
                }.decodeList<kotlinx.serialization.json.JsonObject>()

            if (response.isEmpty()) {
                throw Exception("Gagal memperbarui: Baris tidak ditemukan atau Anda tidak memiliki izin akses (RLS Policy).")
            }

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Gagal memperbarui status tugas")
        }
    }

    suspend fun verifyMultipleTaskAssignments(
        assignmentIds: List<String>,
        checkedBy: String
    ): ResultState<Boolean> {
        return try {
            android.util.Log.d("TASK_VERIFY", "verifyMultipleTaskAssignments STARTED: count=${assignmentIds.size}, ids=$assignmentIds, checkedBy=$checkedBy")

            val request = VerifyMultipleTasksRequest(
                p_assignment_ids = assignmentIds,
                p_checked_by = checkedBy
            )

            val result = SupabaseConfig.client.postgrest.rpc(
                function = "verify_multiple_task_assignments",
                parameters = request
            ).decodeAs<Boolean>()

            android.util.Log.d("TASK_VERIFY", "verifyMultipleTaskAssignments SUCCESS: result=$result")
            ResultState.Success(result)
        } catch (e: Exception) {
            android.util.Log.e("TASK_VERIFY", "verifyMultipleTaskAssignments ERROR: ${e.message}", e)
            ResultState.Error(e.message ?: "Gagal memverifikasi tugas secara massal.")
        }
    }

    suspend fun addComment(
        taskId: String,
        userId: String,
        commentText: String,
        imageUrls: List<String> = emptyList()
    ): ResultState<Unit> {
        return safeSupabaseCall {
            val commentPayload = mapOf(
                "task_id" to taskId,
                "user_id" to userId,
                "comment_text" to commentText
            )

            val insertedComment = SupabaseConfig.client.postgrest["task_comments"]
                .insert(commentPayload) { select() }
                .decodeSingle<JsonObject>()

            val commentId = insertedComment["id"]?.jsonPrimitive?.content

            if (commentId != null && imageUrls.isNotEmpty()) {
                val imagePayloads = imageUrls.map { url ->
                    mapOf(
                        "comment_id" to commentId,
                        "image_url" to url
                    )
                }
                SupabaseConfig.client.postgrest["task_comment_images"].insert(imagePayloads)
            }
            Unit
        }
    }

    suspend fun addCommentWithImages(
        taskId: String,
        userId: String,
        commentText: String,
        imageBytes: List<ByteArray>
    ): ResultState<Unit> {
        val imageUrls = mutableListOf<String>()

        if (imageBytes.isNotEmpty()) {
            when (val uploadResult = uploadImages(imageBytes)) {
                is ResultState.Success -> imageUrls.addAll(uploadResult.data)
                is ResultState.Error -> return ResultState.Error("Gagal mengunggah gambar: ${uploadResult.message}")
                else -> return ResultState.Error("Unknown error saat upload")
            }
        }

        return addComment(taskId, userId, commentText, imageUrls)
    }

    suspend fun deleteTask(taskId: String): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["task_has_labels"].delete { filter { eq("task_id", taskId) } }
            SupabaseConfig.client.postgrest["task_assignments"].delete { filter { eq("task_id", taskId) } }
            SupabaseConfig.client.postgrest["task_images"].delete { filter { eq("task_id", taskId) } }
            SupabaseConfig.client.postgrest["task_comments"].delete { filter { eq("task_id", taskId) } }
            SupabaseConfig.client.postgrest["task_histories"].delete { filter { eq("task_id", taskId) } }
            SupabaseConfig.client.postgrest["tasks"].delete { filter { eq("id", taskId) } }
            Unit
        }
    }

    suspend fun getAllTaskLabels(): ResultState<List<TaskLabelResponse>> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["task_labels"]
                .select()
                .decodeList<TaskLabelResponse>()
        }
    }

    suspend fun getAllTaskRoutines(creatorId: String? = null): ResultState<List<TaskRoutineEntity>> {
        return safeSupabaseCall {
            val query = SupabaseConfig.client.postgrest["task_routine_schedules"]
                .select(columns = Columns.raw("*,task_templates:template_task_id!inner(title,created_by,profiles:created_by(full_name,profile_url),task_template_assignees(assigned_to,profiles:assigned_to(full_name,profile_url)))")) {
                    if (creatorId != null) {
                        filter {
                            eq("task_templates.created_by", creatorId)
                        }
                    }
                }

            val responseList = query.decodeList<TaskRoutineResponse>()
            responseList.map { it.toEntity() }
        }
    }

    suspend fun getRoutineDetail(routineId: String): ResultState<RoutineDetailEntity> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest.rpc(
                function = "get_routine_detail",
                parameters = mapOf("p_routine_id" to routineId)
            ).decodeAs<RoutineDetailResponse>().toEntity()
        }
    }

    suspend fun updateRoutineSchedule(request: UpdateRoutineScheduleRequest): ResultState<Boolean> {
        return try {
            val result = SupabaseConfig.client.postgrest.rpc(
                function = "update_routine_schedule",
                parameters = request
            ).decodeAs<Boolean>()
            ResultState.Success(result)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Gagal memperbarui jadwal rutin.")
        }
    }

    suspend fun updateTaskTemplate(request: UpdateTaskTemplateRequest): ResultState<Boolean> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest.rpc(
                function = "update_task_template",
                parameters = request
            ).decodeAs<Boolean>()
        }
    }

    suspend fun createTaskRoutineSchedule(request: CreateTaskRoutineRequest): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["task_routine_schedules"].insert(request)
            Unit
        }
    }

    suspend fun stopTaskRoutine(routineId: String): ResultState<Unit> {
        return safeSupabaseCall {
            val payload = mapOf("is_active" to false)
            SupabaseConfig.client.postgrest["task_routine_schedules"]
                .update(payload) { filter { eq("id", routineId) } }
            Unit
        }
    }

    suspend fun toggleTaskRoutine(routineId: String, isActive: Boolean): ResultState<Unit> {
        return safeSupabaseCall {
            val payload = mapOf("is_active" to isActive)
            SupabaseConfig.client.postgrest["task_routine_schedules"]
                .update(payload) { filter { eq("id", routineId) } }
            Unit
        }
    }

    suspend fun getAllTaskUrgentReminders(creatorId: String? = null): ResultState<List<id.biz.yuka.libperformx.entity.TaskUrgentEntity>> {
        return safeSupabaseCall {
            val query = SupabaseConfig.client.postgrest["task_urgent_reminders"]
                .select(columns = Columns.raw("*,tasks!inner(title,created_by:assigned_by,profiles:assigned_by(full_name,profile_url),task_template_assignees:task_assignments(assigned_to,profiles:assigned_to(full_name,profile_url)))")) {
                    if (creatorId != null) {
                        filter {
                            eq("tasks.assigned_by", creatorId)
                        }
                    }
                }

            val responseList = query.decodeList<TaskUrgentResponse>()
            responseList.map { it.toEntity() }
        }
    }

    suspend fun toggleTaskUrgentReminder(reminderId: String, isActive: Boolean): ResultState<Unit> {
        return safeSupabaseCall {
            val payload = mapOf("is_active" to isActive)
            SupabaseConfig.client.postgrest["task_urgent_reminders"]
                .update(payload) { filter { eq("id", reminderId) } }
            Unit
        }
    }

    /**
     * MENGAMBIL LAPORAN TUGAS KARYAWAN (Dari View vw_employee_task)
     * Bisa difilter berdasarkan rentang waktu created_at.
     */
    suspend fun getTaskReport(
        startDate: String? = null,
        endDate: String? = null
    ): ResultState<List<EmployeeTaskReportEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["vw_employee_task"]
                .select {
                    filter {
                        if (startDate != null) gte("created_at", startDate)
                        if (endDate != null) lte("created_at", endDate)
                    }
                }
                .decodeList<EmployeeTaskReportResponse>()

            responseList.map { it.toEntity() }
        }
    }
}
