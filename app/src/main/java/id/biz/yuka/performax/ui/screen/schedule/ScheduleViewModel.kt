package id.biz.yuka.performax.ui.screen.schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.response.ScheduleDetailResponse
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.TreeMap
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var schedules by mutableStateOf<List<ScheduleDetailResponse>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    var selectedDate by mutableStateOf(LocalDate.now())
    var userRole by mutableStateOf("employee")

    var startDate by mutableStateOf(LocalDate.now().minusDays(2))
    var endDate by mutableStateOf(LocalDate.now().plusDays(4))

    var dateList by mutableStateOf<List<LocalDate>>(emptyList())

    init {
        updateDateList()
        loadSchedules()
    }

    fun loadSchedules() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""

            // 1. Dapatkan Role Pengguna
            when (val roleRes = authRepository.getUserRole(currentUserId)) {
                is ResultState.Success -> userRole = roleRes.data
                is ResultState.Error -> userRole = "employee"
            }

            // 2. Tentukan filter berdasarkan Role
            val targetUserId = if (userRole == "owner" || userRole == "admin") null else currentUserId

            // 3. Panggil fungsi View tunggal yang super cepat
            when (val result = scheduleRepository.getScheduleDetailsFromView(targetUserId, startDate.toString(), endDate.toString())) {
                is ResultState.Success -> {
                    schedules = result.data
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    fun updateDateRange(start: LocalDate, end: LocalDate) {
        startDate = start
        endDate = end
        updateDateList()
        if (selectedDate.isBefore(startDate) || selectedDate.isAfter(endDate)) {
            selectedDate = startDate
        }
        loadSchedules()
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

    fun getGroupedSchedules(): Map<LocalDate, List<ScheduleDetailResponse>> {
        val filtered = schedules.filter { item ->
            try {
                val workDate = LocalDate.parse(item.work_date)
                !workDate.isBefore(startDate) && !workDate.isAfter(endDate)
            } catch (e: Exception) {
                false
            }
        }.groupBy { item ->
            LocalDate.parse(item.work_date)
        }

        return TreeMap<LocalDate, List<ScheduleDetailResponse>>(compareByDescending { it }).apply {
            putAll(filtered)
        }
    }
}