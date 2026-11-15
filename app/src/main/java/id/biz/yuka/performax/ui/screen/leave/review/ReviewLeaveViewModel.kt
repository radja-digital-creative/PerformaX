package id.biz.yuka.performax.ui.screen.leave.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.LeaveEntity
import id.biz.yuka.libperformx.repository.LeaveRepository
import id.biz.yuka.libperformx.request.RespondLeaveRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewLeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository
) : ViewModel() {

    var pendingLeaves by mutableStateOf<List<LeaveEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init { loadPendingLeaves() }

    fun loadPendingLeaves() {
        viewModelScope.launch {
            isLoading = true
            val result = leaveRepository.getLeaveRequests(status = "pending")
            if (result is ResultState.Success) {
                pendingLeaves = result.data
            }
            isLoading = false
        }
    }

    fun respondToLeave(leaveId: String, isApproved: Boolean) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            successMessage = null
            try {
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    errorMessage = "Sesi login tidak ditemukan. Silakan login ulang."
                    return@launch
                }

                val status = if (isApproved) "approved" else "rejected"
                val request = RespondLeaveRequest(
                    status = status,
                    approved_by = userId,
                    approval_notes = if (isApproved) "Disetujui" else "Ditolak oleh atasan"
                )

                val result = leaveRepository.respondLeave(leaveId, request)
                when (result) {
                    is ResultState.Success -> {
                        successMessage = if (isApproved) "✅ Izin berhasil disetujui." else "Izin berhasil ditolak."
                        loadPendingLeaves()
                    }
                    is ResultState.Error -> {
                        errorMessage = "Gagal memproses izin: ${result.message}"
                    }
                }
            } finally {
                // Pastikan isLoading selalu false meski terjadi error
                isLoading = false
            }
        }
    }
}