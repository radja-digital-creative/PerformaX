package id.biz.yuka.performax.ui.screen.task.create

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.request.CreateTaskRequest
import id.biz.yuka.libperformx.response.TaskLabelResponse
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TaskAttachment(
    val uri: Uri,
    val name: String,
    val isImage: Boolean,
    val size: Long
)

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    // Wizard Navigation
    var currentStep by mutableIntStateOf(1)
        private set

    // Step 1: Info Dasar & Label
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var availableLabels by mutableStateOf<List<TaskLabelResponse>>(emptyList())
    val selectedLabels = mutableStateListOf<TaskLabelResponse>()

    // Step 2: Lampiran/Details
    val attachments = mutableStateListOf<TaskAttachment>()

    // Step 3: Penugasan
    var employees by mutableStateOf<List<EmployeeEntity>>(emptyList())
    val selectedAssignees = mutableStateListOf<EmployeeEntity>()
    var isEmployeeLoading by mutableStateOf(false)

    // Step 4: Konfigurasi Khusus (Routine & Urgent)
    var deadline by mutableStateOf<LocalDate?>(null)
    
    // Step 5: Mode Penugasan (Khusus Multi-Assignee)
    var isTeamTask by mutableStateOf(true) // Default Team
    var isRoutineTask by mutableStateOf(false)
    var routineFrequency by mutableStateOf("WEEKLY")
    var routineInterval by mutableIntStateOf(1)
    var routineDeadlineOffset by mutableIntStateOf(0)
    var routineIsInfinite by mutableStateOf(false) // ✅ BARU
    val routineDaysOfWeek = mutableStateListOf<Int>()
    val routineDaysOfMonth = mutableStateListOf<Int>()

    // Urgent
    var isUrgentTask by mutableStateOf(false)
    var urgentReminderType by mutableStateOf("HOURLY")
    var urgentInterval by mutableIntStateOf(1)

    // UI Status
    var isLoading by mutableStateOf(false)
    var successResult by mutableStateOf<Pair<String, Boolean>?>(null) // Pair(id, isRoutine)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadEmployees()
        loadLabels()
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            isEmployeeLoading = true
            val result = employeeRepository.getAllEmployees()
            if (result is ResultState.Success) employees = result.data
            isEmployeeLoading = false
        }
    }

    private fun loadLabels() {
        viewModelScope.launch {
            val result = taskRepository.getAllTaskLabels()
            if (result is ResultState.Success) {
                availableLabels = result.data
                val general = availableLabels.find { it.name.equals("General Task", ignoreCase = true) }
                if (general != null) selectedLabels.add(general)
            }
        }
    }

    fun nextStep() {
        if (validateCurrentStep()) {
            if (currentStep == 4) {
                // Jika hanya 1 orang, lompati pemilihan mode Tim/Individu (Step 5)
                if (selectedAssignees.size <= 1) {
                    currentStep = 6
                } else {
                    currentStep = 5
                }
            } else if (currentStep < 6) {
                currentStep++
            }
        }
    }

    fun prevStep() {
        if (currentStep == 6 && selectedAssignees.size <= 1) {
            currentStep = 4
        } else if (currentStep > 1) {
            currentStep--
        }
    }

    private fun validateCurrentStep(): Boolean {
        errorMessage = null
        return when (currentStep) {
            1 -> {
                if (title.isBlank()) { errorMessage = "Judul tugas wajib diisi!"; false }
                else if (selectedLabels.isEmpty()) { errorMessage = "Pilih minimal 1 label!"; false }
                else true
            }
            2 -> true // Lampiran opsional
            3 -> {
                if (selectedAssignees.isEmpty()) { errorMessage = "Tentukan siapa yang akan mengerjakan tugas ini!"; false }
                else true
            }
            4 -> {
                if (!isRoutineTask && deadline == null) { errorMessage = "Deadline manual wajib diisi untuk tugas non-rutin!"; false }
                else if (isRoutineTask && routineFrequency == "WEEKLY" && routineDaysOfWeek.isEmpty()) { errorMessage = "Pilih hari pengerjaan!"; false }
                else if (isRoutineTask && routineFrequency == "MONTHLY" && routineDaysOfMonth.isEmpty()) { errorMessage = "Pilih tanggal pengerjaan!"; false }
                else true
            }
            else -> true
        }
    }

    fun toggleLabel(label: TaskLabelResponse) {
        if (selectedLabels.contains(label)) {
            if (selectedLabels.size > 1) {
                selectedLabels.remove(label)
                if (label.name.equals("Routine", ignoreCase = true)) isRoutineTask = false
                if (label.name.equals("Urgent", ignoreCase = true)) isUrgentTask = false
            }
        } else {
            selectedLabels.add(label)
            if (label.name.equals("Routine", ignoreCase = true)) isRoutineTask = true
            if (label.name.equals("Urgent", ignoreCase = true)) isUrgentTask = true
        }
    }

    fun toggleAssignee(employee: EmployeeEntity) {
        if (selectedAssignees.any { it.id == employee.id }) selectedAssignees.removeAll { it.id == employee.id }
        else selectedAssignees.add(employee)
    }

    fun toggleDayOfWeek(day: Int) {
        if (routineDaysOfWeek.contains(day)) routineDaysOfWeek.remove(day)
        else routineDaysOfWeek.add(day)
    }

    fun toggleDayOfMonth(date: Int) {
        if (routineDaysOfMonth.contains(date)) routineDaysOfMonth.remove(date)
        else routineDaysOfMonth.add(date)
    }

    fun addAttachments(context: Context, uris: List<Uri>) {
        uris.forEach { uri ->
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val isImage = mimeType.startsWith("image/")
            var name = "File_${System.currentTimeMillis()}"; var size = 0L
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) { name = cursor.getString(ni) ?: name; size = cursor.getLong(si) }
            }
            if (attachments.none { it.uri == uri }) attachments.add(TaskAttachment(uri, name, isImage, size))
        }
    }

    fun submitTask(context: Context) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val imageBytes = attachments.mapNotNull { context.contentResolver.openInputStream(it.uri)?.readBytes() }
                val storeId = selectedAssignees.firstOrNull()?.stores?.firstOrNull()?.id ?: ""
                // Routine: simulasi due_date = now + dueDay (BE hitung offset dari created_at)
                // Non-routine: pakai tanggal yang dipilih user di date picker
                val finalDeadline = if (isRoutineTask)
                    LocalDate.now().plusDays(routineDeadlineOffset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                else
                    deadline!!.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val request = CreateTaskRequest(
                    p_title = title,
                    p_description = description,
                    p_due_date = finalDeadline,
                    p_store_id = storeId,
                    p_assigned_to_ids = selectedAssignees.map { it.id },
                    p_label_ids = selectedLabels.map { it.id },
                    p_is_task_team = isTeamTask || selectedAssignees.size <= 1,
                    p_is_routine = isRoutineTask,
                    p_routine_frequency = if (isRoutineTask) routineFrequency else null,
                    p_routine_interval = if (isRoutineTask) routineInterval else null,
                    p_routine_days_of_week = if (isRoutineTask && routineFrequency == "WEEKLY") routineDaysOfWeek.toList() else null,
                    p_routine_days_of_month = if (isRoutineTask && routineFrequency == "MONTHLY") routineDaysOfMonth.toList() else null,
                    p_routine_deadline_offset = if (isRoutineTask) routineDeadlineOffset else null,
                    p_routine_is_infinite = if (isRoutineTask) routineIsInfinite else false,
                    p_is_urgent = isUrgentTask,
                    p_urgent_reminder_type = if (isUrgentTask) urgentReminderType else null,
                    p_urgent_interval = if (isUrgentTask) urgentInterval else null
                )

                val result = taskRepository.createTaskWithImages(request, imageBytes)
                when (result) {
                    is ResultState.Success -> successResult = Pair(result.data, isRoutineTask)
                    is ResultState.Error -> errorMessage = result.message
                    else -> errorMessage = "Terjadi kesalahan tidak diketahui."
                }
            } catch (e: Exception) { errorMessage = e.message }
            finally { isLoading = false }
        }
    }
}