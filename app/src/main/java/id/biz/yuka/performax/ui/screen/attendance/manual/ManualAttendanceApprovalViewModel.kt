package id.biz.yuka.performax.ui.screen.attendance.manual

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.ManualAttendanceEntity
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.request.RespondManualAttendanceRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class ManualAttendanceApprovalViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    var selectedTabIndex by mutableIntStateOf(0)
    var items by mutableStateOf<List<ManualAttendanceEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isProcessing by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val statusFilter = when (selectedTabIndex) {
                0 -> "pending"
                1 -> "approved"
                2 -> "rejected"
                else -> null
            }
            val result = attendanceRepository.getManualAttendances(approvalStatus = statusFilter)
            if (result is ResultState.Success) {
                items = result.data
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun onTabSelected(index: Int) {
        selectedTabIndex = index
        loadData()
    }

    fun respondToRequest(id: String, isApproved: Boolean, reason: String? = null) {
        viewModelScope.launch {
            isProcessing = true
            errorMessage = null
            successMessage = null
            try {
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    errorMessage = "Sesi login tidak valid. Silakan login kembali."
                    return@launch
                }

                val status = if (isApproved) "approved" else "rejected"
                val request = RespondManualAttendanceRequest(
                    approval_status = status,
                    approved_by = currentUserId,
                    approval_reason = reason.takeIf { !it.isNullOrBlank() } ?: if (isApproved) "Disetujui" else "Ditolak",
                    approved_at = OffsetDateTime.now().toString()
                )

                val result = attendanceRepository.respondManualAttendance(id, request)
                when (result) {
                    is ResultState.Success -> {
                        successMessage = if (isApproved) "✅ Pengajuan absen manual disetujui." else "Pengajuan absen manual ditolak."
                        loadData()
                    }
                    is ResultState.Error -> {
                        errorMessage = "Gagal memproses: ${result.message}"
                    }
                }
            } finally {
                isProcessing = false
            }
        }
    }
}
