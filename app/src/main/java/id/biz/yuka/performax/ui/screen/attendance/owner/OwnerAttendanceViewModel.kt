package id.biz.yuka.performax.ui.screen.attendance.owner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.AttendanceDetailEntity
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Data class gabungan untuk UI menggunakan EmployeeEntity
data class EmployeeAttendanceUiModel(
    val employee: EmployeeEntity,
    val attendance: AttendanceDetailEntity? = null
)

@HiltViewModel
class OwnerAttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var selectedDate by mutableStateOf(LocalDate.now())

    // Data list
    var presentEmployees by mutableStateOf<List<EmployeeAttendanceUiModel>>(emptyList())
    var absentEmployees by mutableStateOf<List<EmployeeAttendanceUiModel>>(emptyList())

    var totalEmployees by mutableStateOf(0)
    var totalStores by mutableStateOf(0)

    init {
        loadAttendanceData()
    }

    fun changeDate(offsetDays: Long) {
        selectedDate = selectedDate.plusDays(offsetDays)
        loadAttendanceData()
    }

    fun loadAttendanceData() {
        viewModelScope.launch {
            isLoading = true

            // 1. Ambil semua karyawan
            val employeesResult = employeeRepository.getAllEmployees()

            // 2. Ambil absensi dengan rentang tanggal hari itu saja (startDate = endDate)
            val dateStr = selectedDate.toString()
            val attendanceResult = attendanceRepository.getAllAttendances(
                startDate = dateStr,
                endDate = dateStr
            )

            if (employeesResult is ResultState.Success && attendanceResult is ResultState.Success) {
                val allEmployees = employeesResult.data
                val allAttendances = attendanceResult.data

                totalEmployees = allEmployees.size

                // Hitung jumlah toko unik dengan menggabungkan list stores dari setiap karyawan
                val uniqueStoreIds = allEmployees.flatMap { it.stores }.map { it.id }.toSet()
                totalStores = uniqueStoreIds.size

                val present = mutableListOf<EmployeeAttendanceUiModel>()
                val absent = mutableListOf<EmployeeAttendanceUiModel>()

                allEmployees.forEach { employee ->
                    // Cocokkan attendance berdasarkan userId
                    val employeeAttendance = allAttendances.find { it.userId == employee.id }
                    val uiModel = EmployeeAttendanceUiModel(employee = employee, attendance = employeeAttendance)

                    if (employeeAttendance != null) {
                        present.add(uiModel)
                    } else {
                        absent.add(uiModel)
                    }
                }

                presentEmployees = present
                absentEmployees = absent
            }
            isLoading = false
        }
    }
}