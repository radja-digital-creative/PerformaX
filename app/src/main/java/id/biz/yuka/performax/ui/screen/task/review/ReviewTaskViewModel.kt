package id.biz.yuka.performax.ui.screen.task.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    private var allPotentialTasks = listOf<TaskEntity>()
    var tasks by mutableStateOf<List<TaskEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var userRole by mutableStateOf("employee")
    var errorMessage by mutableStateOf<String?>(null)

    // Filter States
    var availableEmployees by mutableStateOf<List<EmployeeEntity>>(emptyList())
    var selectedEmployeeId by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")
    var selectedTabIndex by mutableStateOf(0) // 0: Review, 1: History

    // Batch Selection States
    var selectedAssignmentIds by mutableStateOf<Set<String>>(emptySet())
    var isBatchVerifying by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)

    val isMultiSelectMode: Boolean get() = selectedAssignmentIds.isNotEmpty()

    init {
        loadReviewTasks()
    }

    fun updateEmployeeFilter(employeeId: String?) {
        selectedEmployeeId = employeeId
        clearSelection()
        applyFilters()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        clearSelection()
        applyFilters()
    }

    fun updateTabIndex(index: Int) {
        selectedTabIndex = index
        clearSelection()
        applyFilters()
    }

    fun toggleAssignmentSelection(assignmentId: String) {
        selectedAssignmentIds = if (selectedAssignmentIds.contains(assignmentId)) {
            selectedAssignmentIds - assignmentId
        } else {
            selectedAssignmentIds + assignmentId
        }
    }

    fun selectAllCurrentTasks() {
        if (selectedTabIndex != 0) return
        val allCompletedAssignmentIds = tasks.flatMap { task ->
            task.assignees.filter { it.status == "completed" }.map { it.assignmentId }
        }.toSet()

        selectedAssignmentIds = if (selectedAssignmentIds.size == allCompletedAssignmentIds.size && allCompletedAssignmentIds.isNotEmpty()) {
            emptySet()
        } else {
            allCompletedAssignmentIds
        }
    }

    fun clearSelection() {
        selectedAssignmentIds = emptySet()
    }

    fun verifySelectedTasks() {
        if (selectedAssignmentIds.isEmpty()) return
        viewModelScope.launch {
            isBatchVerifying = true
            errorMessage = null
            successMessage = null

            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                errorMessage = "Sesi login tidak ditemukan. Silakan login ulang."
                isBatchVerifying = false
                return@launch
            }

            val assignmentList = selectedAssignmentIds.toList()
            val count = assignmentList.size

            when (val result = taskRepository.verifyMultipleTaskAssignments(assignmentList, currentUserId)) {
                is ResultState.Success -> {
                    successMessage = "✅ $count Tugas berhasil diverifikasi!"
                    clearSelection()
                    loadReviewTasks()
                }
                is ResultState.Error -> {
                    errorMessage = "Gagal memverifikasi tugas: ${result.message}"
                }
            }
            isBatchVerifying = false
        }
    }

    private fun applyFilters() {
        val filteredByStatus = allPotentialTasks.filter { task ->
            if (selectedTabIndex == 0) {
                task.assignees.any { it.status == "completed" }
            } else {
                task.assignees.any { it.status == "verified" }
            }
        }

        val filteredByEmployee = if (selectedEmployeeId == null) {
            filteredByStatus
        } else {
            filteredByStatus.filter { task ->
                task.creatorId == selectedEmployeeId || task.assignees.any { it.userId == selectedEmployeeId }
            }
        }

        tasks = filteredByEmployee.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    fun loadReviewTasks() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                errorMessage = "Sesi tidak ditemukan. Silakan login ulang."
                isLoading = false
                return@launch
            }

            // 1. Ambil Role & Store
            val roleJob = async { authRepository.getUserRole(currentUserId) }
            val storesJob = async { storeRepository.getMyStores(currentUserId) }

            val roleRes = roleJob.await()
            val storesRes = storesJob.await()

            if (roleRes is ResultState.Success) {
                userRole = roleRes.data.lowercase()
            }

            val isOwnerOrAdmin = userRole == "owner" || userRole == "admin"
            val isManager = userRole == "manager"

            // 2. Ambil Daftar Karyawan
            val empRes = if (isOwnerOrAdmin) {
                employeeRepository.getAllEmployees()
            } else if (isManager && storesRes is ResultState.Success) {
                employeeRepository.getEmployeesByStores(storesRes.data.map { it.id })
            } else {
                ResultState.Success(emptyList())
            }

            if (empRes is ResultState.Success) {
                availableEmployees = if (isOwnerOrAdmin) {
                    empRes.data
                } else {
                    empRes.data.filter { it.id != currentUserId }
                }
            }

            // 3. Ambil Daftar Tugas
            val result = taskRepository.getTasks()

            when (result) {
                is ResultState.Success -> {
                    val allTasks = result.data
                    val allowedEmployeeIds = availableEmployees.map { it.id }.toSet() + currentUserId

                    allPotentialTasks = allTasks.filter { task ->
                        val hasTargetStatus = task.assignees.any { it.status == "completed" || it.status == "verified" }
                        if (!hasTargetStatus) return@filter false

                        when {
                            isOwnerOrAdmin -> true
                            isManager -> {
                                // Filter: Tugas yang dibuat manager sendiri ATAU tugas yang dikerjakan oleh karyawan di toko-tokonya
                                task.creatorId == currentUserId || 
                                task.assignees.any { allowedEmployeeIds.contains(it.userId) }
                            }
                            else -> task.creatorId == currentUserId
                        }
                    }
                    applyFilters()
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }
}
