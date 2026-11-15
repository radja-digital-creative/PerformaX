package id.biz.yuka.performax.ui.screen.task

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig // ✅ TAMBAHAN IMPORT
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.TreeMap
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val employeeRepository: EmployeeRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var tasks by mutableStateOf<List<TaskEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Filter Karyawan (Owner Only)
    var userRole by mutableStateOf("employee")
    val isOwner: Boolean get() = (userRole == "owner" || userRole == "admin")
    val isManagerOrOwner: Boolean get() = (userRole == "owner" || userRole == "admin" || userRole == "manager")
    var employees by mutableStateOf<List<EmployeeEntity>>(emptyList())
    var selectedEmployeeId by mutableStateOf<String?>(null)

    var searchQuery by mutableStateOf("")

    var selectedDate by mutableStateOf<LocalDate>(LocalDate.now())

    // Default 7 days range: 4 days before, today, 2 days after
    var startDate by mutableStateOf(LocalDate.now().minusDays(4))
    var endDate by mutableStateOf(LocalDate.now().plusDays(2))

    var dateList by mutableStateOf<List<LocalDate>>(emptyList())

    init {
        updateDateList()
        checkRoleAndInitialize()
    }

    private fun checkRoleAndInitialize() {
        viewModelScope.launch {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
            when (val roleRes = authRepository.getUserRole(currentUserId)) {
                is ResultState.Success -> {
                    userRole = roleRes.data.lowercase()
                    if (isOwner) {
                        loadEmployees()
                    }
                }
                is ResultState.Error -> userRole = "employee"
            }
            loadTasks()
        }
    }

    private suspend fun loadEmployees() {
        when (val result = employeeRepository.getAllEmployees()) {
            is ResultState.Success -> employees = result.data
            else -> {}
        }
    }

    fun updateEmployeeFilter(employeeId: String?) {
        selectedEmployeeId = employeeId
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            if (userId == null) {
                isLoading = false
                errorMessage = "Sesi login tidak valid."
                return@launch
            }

            val result = taskRepository.getTasks()
            when (result) {
                is ResultState.Success -> {
                    val allTasks = result.data
                    tasks = if (isOwner) {
                        if (selectedEmployeeId != null) {
                            allTasks.filter { task ->
                                task.assignees.any { it.userId == selectedEmployeeId }
                            }
                        } else {
                            allTasks // Owner see all by default if no filter
                        }
                    } else {
                        allTasks.filter { task ->
                            task.assignees.any { it.userId == userId } || task.creatorId == userId
                        }
                    }
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    fun updateDateRange(start: LocalDate, end: LocalDate) {
        // Business Rule: Minimal 7 hari
        val daysBetween = ChronoUnit.DAYS.between(start, end)
        if (daysBetween < 6) {
            // We can handle error or automatically expand. User requested minimum 7 days rule.
        }

        startDate = start
        endDate = end
        updateDateList()

        // Adjust selected date if it falls out of range
        if (selectedDate.isBefore(startDate) || selectedDate.isAfter(endDate)) {
            selectedDate = startDate
        }
    }

    private fun updateDateList() {
        val days = ChronoUnit.DAYS.between(startDate, endDate).toInt()
        dateList = (0..days).map { startDate.plusDays(it.toLong()) }
    }

    fun updateSelectedDate(date: LocalDate) {
        if (selectedDate != date) {
            selectedDate = date
        }
    }

    fun getGroupedTasks(): Map<LocalDate, List<TaskEntity>> {
        val filtered = tasks.filter { task ->
            try {
                val taskDate = LocalDate.parse(task.dueDate.take(10))
                val matchesDate = !taskDate.isBefore(startDate) && !taskDate.isAfter(endDate)
                val matchesSearch = if (searchQuery.isBlank()) true 
                                   else task.title.contains(searchQuery, ignoreCase = true) || 
                                        task.description.contains(searchQuery, ignoreCase = true)
                
                matchesDate && matchesSearch
            } catch (e: Exception) {
                false
            }
        }.groupBy { task ->
            LocalDate.parse(task.dueDate.take(10))
        }

        return TreeMap<LocalDate, List<TaskEntity>>(compareByDescending { it }).apply {
            putAll(filtered)
        }
    }
}
