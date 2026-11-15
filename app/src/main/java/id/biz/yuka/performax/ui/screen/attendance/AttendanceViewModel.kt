package id.biz.yuka.performax.ui.screen.attendance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig // TAMBAHAN
import id.biz.yuka.libperformx.entity.AttendanceDetailEntity
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth // TAMBAHAN
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val storeRepository: StoreRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var dateList by mutableStateOf<List<LocalDate>>(emptyList())
    var selectedDate by mutableStateOf(LocalDate.now())

    var storeName by mutableStateOf("")
    var storeLat by mutableStateOf<Double?>(null)
    var storeLng by mutableStateOf<Double?>(null)

    private var allAttendances by mutableStateOf<List<AttendanceDetailEntity>>(emptyList())

    init {
        generateDateList()
        loadAttendances()
    }

    private fun generateDateList() {
        val dates = mutableListOf<LocalDate>()
        val today = LocalDate.now()
        for (i in 6 downTo 0) {
            dates.add(today.minusDays(i.toLong()))
        }
        dateList = dates.reversed()
    }

    fun loadAttendances() {
        viewModelScope.launch {
            isLoading = true
            val startDate = LocalDate.now().minusDays(6).toString()
            val endDate = LocalDate.now().toString()

            // ✅ AMBIL USER ID YANG SEDANG LOGIN SAAT INI
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id

            if (userId != null) {
                // ✅ UBAH: Minta repository untuk mencari absen berdasarkan userId
                val result = attendanceRepository.getUserAttendances(userId, startDate, endDate)
                if (result is ResultState.Success) {
                    allAttendances = result.data
                    fetchStoreDetailForSelectedDate()
                }
            }
            isLoading = false
        }
    }

    fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
        fetchStoreDetailForSelectedDate()
    }

    fun getSelectedAttendance(): AttendanceDetailEntity? {
        val dateStr = selectedDate.toString()
        return allAttendances.find { (it.date) == dateStr }
    }

    private fun fetchStoreDetailForSelectedDate() {
        val currentAttendance = getSelectedAttendance()
        val storeId = currentAttendance?.storeId

        if (storeId != null) {
            viewModelScope.launch {
                val result = storeRepository.getStoreDetail(storeId)
                if (result is ResultState.Success) {
                    storeName = result.data.name
                    storeLat = result.data.latitude
                    storeLng = result.data.longitude
                } else {
                    storeName = "Gagal memuat lokasi toko"
                    storeLat = null
                    storeLng = null
                }
            }
        } else {
            storeName = ""
            storeLat = null
            storeLng = null
        }
    }
}