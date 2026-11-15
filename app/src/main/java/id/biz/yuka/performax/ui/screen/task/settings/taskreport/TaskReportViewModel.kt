package id.biz.yuka.performax.ui.screen.task.settings.taskreport

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeTaskReportEntity
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class TaskReportPeriod(val label: String) {
    TODAY("Hari Ini"),
    WEEK("1 Minggu Terakhir"),
    MONTH("1 Bulan Terakhir")
}

@HiltViewModel
class TaskReportViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Data Laporan
    private var allReports = listOf<EmployeeTaskReportEntity>()
    var filteredReports by mutableStateOf<List<EmployeeTaskReportEntity>>(emptyList())
        private set

    // Filter States
    var selectedPeriod by mutableStateOf(TaskReportPeriod.MONTH)
    var selectedEmployeeName by mutableStateOf<String?>(null)
    var availableEmployeeNames by mutableStateOf<List<String>>(emptyList())

    init {
        fetchReport()
    }

    fun updatePeriod(period: TaskReportPeriod) {
        selectedPeriod = period
        fetchReport()
    }

    fun updateEmployeeFilter(name: String?) {
        selectedEmployeeName = name
        applyLocalFilter()
    }

    fun fetchReport() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val today = LocalDate.now()
            val startDate = when (selectedPeriod) {
                TaskReportPeriod.TODAY -> today
                TaskReportPeriod.WEEK -> today.minusWeeks(1)
                TaskReportPeriod.MONTH -> today.minusMonths(1)
            }
            
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val result = taskRepository.getTaskReport(
                startDate = startDate.format(formatter),
                endDate = today.format(formatter)
            )

            if (result is ResultState.Success) {
                allReports = result.data
                // Ambil daftar nama unik untuk filter
                availableEmployeeNames = allReports.asSequence().map { it.employeeName }.distinct().sorted().toList()
                applyLocalFilter()
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            
            isLoading = false
        }
    }

    private fun applyLocalFilter() {
        filteredReports = if (selectedEmployeeName == null) {
            allReports
        } else {
            allReports.filter { it.employeeName == selectedEmployeeName }
        }
    }
}
