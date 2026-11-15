package id.biz.yuka.performax.ui.screen.schedule.shift

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.request.ShiftRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShiftManageViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val storeId: String = checkNotNull(savedStateHandle["storeId"])
    val shiftId: String = checkNotNull(savedStateHandle["shiftId"])
    val isEditMode = shiftId != "new"

    var shiftName by mutableStateOf("")
    var startTime by mutableStateOf("08:00:00")
    var endTime by mutableStateOf( "17:00:00")
    var radius by mutableStateOf("50")
    var isActive by mutableStateOf(true)

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successEvent = mutableStateOf(false)

    init {
        if (isEditMode) loadShiftDetail()
    }

    private fun loadShiftDetail() {
        viewModelScope.launch {
            isLoading = true
            when (val result = scheduleRepository.getShifts(storeId)) {
                is ResultState.Success -> {
                    val shift = result.data.find { it.id == shiftId }
                    shift?.let {
                        shiftName = it.shiftName
                        startTime = it.startTime
                        endTime = it.endTime
                        radius = it.attendanceRadius.toString()
                        isActive = it.isActive
                    }
                }
                is ResultState.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun saveShift() {
        viewModelScope.launch {
            isLoading = true
            val request = ShiftRequest(
                store_id = storeId,
                shift_name = shiftName,
                start_time = startTime,
                end_time = endTime,
                attendance_radius = radius.toIntOrNull() ?: 50,
                is_active = isActive
            )

            val result = if (isEditMode) {
                scheduleRepository.updateShift(shiftId, request)
            } else {
                scheduleRepository.createShift(request)
            }

            when (result) {
                is ResultState.Success -> successEvent.value = true
                is ResultState.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun deleteShift() {
        viewModelScope.launch {
            isLoading = true
            // ✅ PROTEKSI: Cek apakah digunakan oleh karyawan di tabel user_schedules
            val checkInUse = SupabaseConfig.client.postgrest["user_schedules"]
                .select {
                    filter { eq("shift_id", shiftId) }
                    count(Count.EXACT)
                }.countOrNull() ?: 0L

            if (checkInUse > 0) {
                errorMessage = "Gagal menghapus: Shift ini masih digunakan oleh $checkInUse jadwal karyawan. Silakan ganti dulu jadwal mereka."
                isLoading = false
                return@launch
            }

            when (val result = scheduleRepository.deleteShift(shiftId)) {
                is ResultState.Success -> successEvent.value = true
                is ResultState.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }
}