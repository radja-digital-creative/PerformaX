package id.biz.yuka.performax.ui.screen.task.settings.routine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.TaskRoutineEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskRoutineListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    private val _routines = mutableStateListOf<TaskRoutineEntity>()
    var errorMessage by mutableStateOf<String?>(null)

    // Filter States
    var searchQuery by mutableStateOf("")
    var selectedEmployeeId by mutableStateOf<String?>(null)
    var selectedTabIndex by mutableStateOf(0)

    // Employees for filter
    var employees = mutableStateListOf<EmployeeEntity>()

    val filteredRoutines: StateFlow<List<TaskRoutineEntity>> = combine(
        snapshotFlow { searchQuery },
        snapshotFlow { selectedEmployeeId },
        snapshotFlow { selectedTabIndex },
        snapshotFlow { _routines.toList() }
    ) { query, empId, tabIndex, allRoutines ->
        allRoutines.filter { routine ->
            val matchesQuery = routine.taskTitle.contains(query, ignoreCase = true)
            val matchesStatus = if (tabIndex == 0) routine.isActive else !routine.isActive
            val matchesEmployee = empId == null || routine.creatorId == empId || routine.assigneeIds.contains(empId)
            
            matchesQuery && matchesStatus && matchesEmployee
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadRoutines()
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

    fun loadRoutines() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val result = taskRepository.getAllTaskRoutines(null) 
            
            if (result is ResultState.Success) {
                _routines.clear()
                _routines.addAll(result.data)
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun toggleRoutine(routineId: String, isActive: Boolean) {
        viewModelScope.launch {
            val index = _routines.indexOfFirst { it.id == routineId }
            if (index != -1) {
                _routines[index] = _routines[index].copy(isActive = isActive)
            }

            val result = taskRepository.toggleTaskRoutine(routineId, isActive)
            if (result is ResultState.Error) {
                errorMessage = "Gagal mengubah status: ${result.message}"
                loadRoutines()
            }
        }
    }
}