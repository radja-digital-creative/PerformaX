package id.biz.yuka.performax.ui.screen.schedule.advanced

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.ShiftEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.response.ScheduleDetailResponse
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AdvancedScheduleViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // Data Karyawan & Store
    var availableEmployees = mutableStateListOf<EmployeeEntity>()
    var selectedEmployee by mutableStateOf<EmployeeEntity?>(null)
    var selectedStoreId by mutableStateOf("")

    // Data Shift & Legend Warna
    var availableShifts = mutableStateListOf<ShiftEntity>()
    var shiftColors = mutableStateMapOf<String, Color>() // Memetakan ID Shift ke Warna
    private val palette = listOf(
        Color(0xFFFF9800), // Orange
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Light Blue
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Deep Purple
    )

    // Data Kalender
    var currentYearMonth by mutableStateOf(YearMonth.now())
    var monthlySchedules = mutableStateMapOf<LocalDate, ScheduleDetailResponse>()
    var otherStoreSchedules = mutableStateMapOf<LocalDate, List<String>>() // Tanggal -> Daftar Nama Toko Lain

    // Modal Edit Shift
    var showShiftModal by mutableStateOf(false)
    var selectedDateForEdit by mutableStateOf<LocalDate?>(null)

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
        if (employee.stores.isNotEmpty()) {
            selectStore(employee.stores.first().id)
        } else {
            selectedStoreId = ""
            availableShifts.clear()
            monthlySchedules.clear()
        }
    }

    fun selectStore(storeId: String) {
        selectedStoreId = storeId
        fetchShiftsAndSchedules(storeId)
    }

    private fun fetchShiftsAndSchedules(storeId: String) {
        viewModelScope.launch {
            isLoading = true
            // Ambil master shift
            val shiftRes = scheduleRepository.getShifts(storeId)
            if (shiftRes is ResultState.Success) {
                availableShifts.clear()
                availableShifts.addAll(shiftRes.data)

                // Buat legend warna untuk setiap shift
                shiftColors.clear()
                shiftRes.data.forEachIndexed { index, shift ->
                    shiftColors[shift.id] = palette[index % palette.size]
                }
            }

            // Ambil jadwal bulan ini
            loadMonthlySchedules()
            isLoading = false
        }
    }

    fun changeMonth(offset: Int) {
        currentYearMonth = currentYearMonth.plusMonths(offset.toLong())
        if (selectedEmployee != null && selectedStoreId.isNotBlank()) {
            viewModelScope.launch {
                isLoading = true
                loadMonthlySchedules()
                isLoading = false
            }
        }
    }

    private suspend fun loadMonthlySchedules() {
        val empId = selectedEmployee?.id ?: return
        val currentStoreId = selectedStoreId

        // Buat range tanggal awal dan akhir bulan
        val startDate = currentYearMonth.atDay(1).toString()
        val endDate = currentYearMonth.atEndOfMonth().toString()

        val result = scheduleRepository.getScheduleDetailsFromView(empId, startDate, endDate)
        if (result is ResultState.Success) {
            monthlySchedules.clear()
            otherStoreSchedules.clear()

            result.data.forEach { schedule ->
                try {
                    val date = LocalDate.parse(schedule.work_date)
                    
                    if (schedule.store_id == currentStoreId) {
                        // Simpan ke map utama jika toko cocok dengan tab aktif
                        monthlySchedules[date] = schedule
                    } else {
                        // Simpan info toko lain untuk deteksi tabrakan (Collision)
                        val existing = otherStoreSchedules[date] ?: emptyList()
                        otherStoreSchedules[date] = existing + (schedule.store_name ?: "Toko Lain")
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun openEditModal(date: LocalDate) {
        selectedDateForEdit = date
        showShiftModal = true
    }

    fun saveScheduleOverride(scheduleId: String?,shiftId: String?) {
        val date = selectedDateForEdit ?: return
        val empId = selectedEmployee?.id ?: return
        val storeId = selectedStoreId.ifBlank { return }

        viewModelScope.launch {
            isLoading = true
            showShiftModal = false

            val result = scheduleRepository.createOrUpdateSchedule(
                scheduleId = scheduleId,
                userId = empId,
                storeId = storeId,
                shiftId = shiftId, // Jika null berarti libur/hapus
                workDate = date.toString()
            )

            if (result is ResultState.Success) {
                successMessage = "Jadwal tanggal $date berhasil diperbarui!"
                loadMonthlySchedules() // Refresh UI Kalender
            } else if (result is ResultState.Error) {
                errorMessage = "Gagal mengubah jadwal: ${result.message}"
            }
            isLoading = false
        }
    }
}