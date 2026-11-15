package id.biz.yuka.performax.ui.screen.schedule.generate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.ShiftEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenerateScheduleViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // Data Karyawan
    var availableEmployees = mutableStateListOf<EmployeeEntity>()
    var selectedEmployee by mutableStateOf<EmployeeEntity?>(null)

    // Data Store (Dari karyawan yang dipilih) & Shift
    var selectedStoreId by mutableStateOf("")
    var availableShifts = mutableStateListOf<ShiftEntity>()
    var selectedShiftId by mutableStateOf("")

    // Data Konfigurasi Generate
    // Menyimpan hari aktif (1=Senin, 2=Selasa, dst)
    var selectedDays = mutableStateListOf<Int>()
    var selectedMonthCount by mutableStateOf(1) // Default 1 bulan

    init {
        fetchEmployees()
    }

    private fun fetchEmployees() {
        viewModelScope.launch {
            isLoading = true
            val result = employeeRepository.getAllEmployees()
            if (result is ResultState.Success) {
                availableEmployees.clear()
                availableEmployees.addAll(result.data)
            } else if (result is ResultState.Error) {
                errorMessage = "Gagal memuat karyawan: ${result.message}"
            }
            isLoading = false
        }
    }

    fun selectEmployee(employee: EmployeeEntity) {
        selectedEmployee = employee
        selectedShiftId = ""
        availableShifts.clear()

        // Otomatis pilih toko pertama (Primary) dari karyawan tersebut
        if (employee.stores.isNotEmpty()) {
            selectStore(employee.stores.first().id)
        } else {
            selectedStoreId = ""
        }
    }

    fun selectStore(storeId: String) {
        selectedStoreId = storeId
        selectedShiftId = ""
        fetchShifts(storeId)
    }

    private fun fetchShifts(storeId: String) {
        viewModelScope.launch {
            val result = scheduleRepository.getShifts(storeId)
            if (result is ResultState.Success) {
                availableShifts.clear()
                availableShifts.addAll(result.data)
            }
        }
    }

    fun toggleDay(dayNumber: Int) {
        if (selectedDays.contains(dayNumber)) {
            selectedDays.remove(dayNumber)
        } else {
            selectedDays.add(dayNumber)
        }
    }

    fun executeGenerate() {
        val empId = selectedEmployee?.id
        if (empId == null || selectedStoreId.isBlank() || selectedShiftId.isBlank()) {
            errorMessage = "Karyawan, Toko, dan Shift wajib dipilih!"
            return
        }
        if (selectedDays.isEmpty()) {
            errorMessage = "Minimal pilih 1 hari kerja aktif!"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = scheduleRepository.generateBulkSchedule(
                userId = empId,
                storeId = selectedStoreId,
                shiftId = selectedShiftId,
                activeDays = selectedDays.toList(),
                monthsAhead = selectedMonthCount
            )

            if (result is ResultState.Success) {
                successMessage = "Jadwal berhasil di-generate!"
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }
}