package id.biz.yuka.performax.ui.screen.task.settings.routine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.RoutineDetailEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.request.UpdateRoutineScheduleRequest
import id.biz.yuka.libperformx.request.UpdateTaskTemplateRequest
import id.biz.yuka.libperformx.response.TaskLabelResponse
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskRoutineDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val employeeRepository: EmployeeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routineId: String = checkNotNull(savedStateHandle["routineId"])

    var detail by mutableStateOf<RoutineDetailEntity?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    var showEditScheduleSheet by mutableStateOf(false)
    var editFrequency by mutableStateOf("WEEKLY")
    var editInterval by mutableIntStateOf(1)
    var editDeadlineOffset by mutableIntStateOf(0)
    val editDaysOfWeek = mutableStateListOf<Int>()
    val editDaysOfMonth = mutableStateListOf<Int>()
    var isSavingSchedule by mutableStateOf(false)

    var showEditTemplateSheet by mutableStateOf(false)
    var editTitle by mutableStateOf("")
    var editDescription by mutableStateOf("")
    var editIsTaskTeam by mutableStateOf(true)
    var isSavingTemplate by mutableStateOf(false)

    var availableEmployees by mutableStateOf<List<EmployeeEntity>>(emptyList())
    val editSelectedAssignees = mutableStateListOf<EmployeeEntity>()
    var isEmployeeLoading by mutableStateOf(false)
    var availableLabels by mutableStateOf<List<TaskLabelResponse>>(emptyList())
    val editSelectedLabels = mutableStateListOf<TaskLabelResponse>()

    init { loadDetail(); loadEmployees(); loadLabels() }

    fun loadDetail() {
        viewModelScope.launch {
            isLoading = true; errorMessage = null
            when (val res = taskRepository.getRoutineDetail(routineId)) {
                is ResultState.Success -> detail = res.data
                is ResultState.Error   -> errorMessage = res.message
                else -> {}
            }
            isLoading = false
        }
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            isEmployeeLoading = true
            val res = employeeRepository.getAllEmployees()
            if (res is ResultState.Success) availableEmployees = res.data
            isEmployeeLoading = false
        }
    }

    private fun loadLabels() {
        viewModelScope.launch {
            val res = taskRepository.getAllTaskLabels()
            if (res is ResultState.Success) availableLabels = res.data
        }
    }

    fun toggleRoutine(isActive: Boolean) {
        viewModelScope.launch {
            val d = detail ?: return@launch
            // Optimistic Update
            val oldDetail = d
            detail = d.copy(isActive = isActive)
            
            isLoading = true
            when (val res = taskRepository.toggleTaskRoutine(routineId, isActive)) {
                is ResultState.Success -> { /* Data will be refreshed by loadDetail if needed, but we already updated state */ }
                is ResultState.Error -> {
                    errorMessage = res.message
                    detail = oldDetail // Rollback
                }
            }
            isLoading = false
        }
    }

    fun toggleInfinite(isInfinite: Boolean) {
        viewModelScope.launch {
            val d = detail ?: return@launch
            // ✅ Optimistic Update: Langsung ubah di UI
            val oldDetail = d
            detail = d.copy(isInfinite = isInfinite)

            isLoading = true
            val req = UpdateRoutineScheduleRequest(
                p_routine_id = routineId,
                p_frequency = d.frequency,
                p_interval = d.interval,
                p_days_of_week = if (d.frequency == "WEEKLY") d.daysOfWeek else null,
                p_days_of_month = if (d.frequency == "MONTHLY") d.daysOfMonth else null,
                p_deadline_offset = d.deadlineOffset,
                p_is_infinite = isInfinite
            )
            when (val res = taskRepository.updateRoutineSchedule(req)) {
                is ResultState.Success -> {
                    if (!res.data) {
                        errorMessage = "Gagal memperbarui status: Respon server negatif."
                        detail = oldDetail // Rollback
                    }
                }
                is ResultState.Error -> {
                    errorMessage = res.message
                    detail = oldDetail // Rollback
                }
            }
            isLoading = false
        }
    }

    fun openEditSchedule() {
        val d = detail ?: return
        editFrequency = d.frequency; editInterval = d.interval; editDeadlineOffset = d.deadlineOffset
        editDaysOfWeek.clear(); editDaysOfWeek.addAll(d.daysOfWeek)
        editDaysOfMonth.clear(); editDaysOfMonth.addAll(d.daysOfMonth)
        showEditScheduleSheet = true
    }

    fun toggleEditDayOfWeek(day: Int) {
        if (editDaysOfWeek.contains(day)) editDaysOfWeek.remove(day) else editDaysOfWeek.add(day)
    }

    fun toggleEditDayOfMonth(date: Int) {
        if (editDaysOfMonth.contains(date)) editDaysOfMonth.remove(date) else editDaysOfMonth.add(date)
    }

    fun saveSchedule() {
        viewModelScope.launch {
            isSavingSchedule = true; errorMessage = null
            val req = UpdateRoutineScheduleRequest(
                p_routine_id      = routineId,
                p_frequency       = editFrequency,
                p_interval        = editInterval,
                p_days_of_week    = if (editFrequency == "WEEKLY") editDaysOfWeek.toList() else null,
                p_days_of_month   = if (editFrequency == "MONTHLY") editDaysOfMonth.toList() else null,
                p_deadline_offset = editDeadlineOffset,
                p_is_infinite     = detail?.isInfinite ?: false
            )
            when (val res = taskRepository.updateRoutineSchedule(req)) {
                is ResultState.Success -> { 
                    showEditScheduleSheet = false
                    successMessage = "Jadwal diperbarui!"
                    loadDetail() 
                }
                is ResultState.Error   -> errorMessage = res.message
            }
            isSavingSchedule = false
        }
    }

    fun openEditTemplate() {
        val d = detail ?: return
        editTitle = d.title; editDescription = d.description; editIsTaskTeam = d.isTaskTeam
        val uids = d.assignees.map { it.userId }.toSet()
        val lids = d.labels.map { it.id }.toSet()
        editSelectedAssignees.clear(); editSelectedAssignees.addAll(availableEmployees.filter { it.id in uids })
        editSelectedLabels.clear(); editSelectedLabels.addAll(availableLabels.filter { it.id in lids })
        showEditTemplateSheet = true
    }

    fun toggleEditAssignee(emp: EmployeeEntity) {
        if (editSelectedAssignees.any { it.id == emp.id }) editSelectedAssignees.removeAll { it.id == emp.id }
        else editSelectedAssignees.add(emp)
    }

    fun toggleEditLabel(label: TaskLabelResponse) {
        if (editSelectedLabels.contains(label)) editSelectedLabels.remove(label) else editSelectedLabels.add(label)
    }

    fun saveTemplate() {
        viewModelScope.launch {
            isSavingTemplate = true; errorMessage = null
            val storeId = editSelectedAssignees.firstOrNull()?.stores?.firstOrNull()?.id
            val req = UpdateTaskTemplateRequest(
                p_template_id     = detail?.templateId ?: return@launch,
                p_title           = editTitle,
                p_description     = editDescription.ifBlank { null },
                p_is_task_team    = editIsTaskTeam,
                p_assigned_to_ids = editSelectedAssignees.map { it.id },
                p_store_id        = storeId,
                p_label_ids       = editSelectedLabels.map { it.id }
            )
            when (val res = taskRepository.updateTaskTemplate(req)) {
                is ResultState.Success -> { showEditTemplateSheet = false; successMessage = "Template diperbarui!"; loadDetail() }
                is ResultState.Error   -> errorMessage = res.message
                else -> {}
            }
            isSavingTemplate = false
        }
    }
}
