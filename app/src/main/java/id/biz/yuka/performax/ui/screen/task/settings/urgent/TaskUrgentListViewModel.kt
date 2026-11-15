package id.biz.yuka.performax.ui.screen.task.settings.urgent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.TaskUrgentEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskUrgentListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    private val _urgentReminders = mutableStateListOf<TaskUrgentEntity>()
    var errorMessage by mutableStateOf<String?>(null)

    // Filter States
    var searchQuery by mutableStateOf("")
    var selectedEmployeeId by mutableStateOf<String?>(null)
    var selectedTabIndex by mutableStateOf(0)

    // Employees for filter
    var employees = mutableStateListOf<EmployeeEntity>()

    val filteredReminders: StateFlow<List<TaskUrgentEntity>> = combine(
        snapshotFlow { searchQuery },
        snapshotFlow { selectedEmployeeId },
        snapshotFlow { selectedTabIndex },
        snapshotFlow { _urgentReminders.toList() }
    ) { query, empId, tabIndex, allReminders ->
        allReminders.filter { reminder ->
            val matchesQuery = reminder.taskTitle.contains(query, ignoreCase = true)
            val matchesStatus = if (tabIndex == 0) reminder.isActive else !reminder.isActive
            val matchesEmployee = empId == null || reminder.creatorId == empId || reminder.assigneeIds.contains(empId)
            
            matchesQuery && matchesStatus && matchesEmployee
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadUrgentReminders()
        loadEmployees()
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            val result = employeeRepository.getAllEmployees()
            if (result is ResultState.Success) {
                employees.clear()
                employees.addAll(result.data)
            }
        }
    }

    fun loadUrgentReminders() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val result = taskRepository.getAllTaskUrgentReminders(null) 
            
            if (result is ResultState.Success) {
                _urgentReminders.clear()
                _urgentReminders.addAll(result.data)
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun toggleReminder(reminderId: String, isActive: Boolean) {
        viewModelScope.launch {
            val index = _urgentReminders.indexOfFirst { it.id == reminderId }
            if (index != -1) {
                _urgentReminders[index] = _urgentReminders[index].copy(isActive = isActive)
            }

            val result = taskRepository.toggleTaskUrgentReminder(reminderId, isActive)
            if (result is ResultState.Error) {
                errorMessage = "Gagal mengubah status: ${result.message}"
                loadUrgentReminders()
            }
        }
    }
}