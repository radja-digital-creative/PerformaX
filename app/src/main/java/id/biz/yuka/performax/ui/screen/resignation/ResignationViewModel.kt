package id.biz.yuka.performax.ui.screen.resignation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.ResignationEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.ResignationRepository
import id.biz.yuka.libperformx.request.CreateResignationRequest
import id.biz.yuka.libperformx.request.ProcessResignationRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ResignationViewModel @Inject constructor(
    private val resignationRepository: ResignationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var currentUserId by mutableStateOf<String?>(null)
        private set
    var isOwner by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // State Employee
    var activeResignation by mutableStateOf<ResignationEntity?>(null)
        private set
    var reasonInput by mutableStateOf("")
    var proposedLastDayInput by mutableStateOf("")

    // State Owner
    var allResignations by mutableStateOf<List<ResignationEntity>>(emptyList())
        private set
    var selectedResignationToReject by mutableStateOf<ResignationEntity?>(null)
    var rejectionReasonInput by mutableStateOf("")

    init {
        initData()
    }

    fun initData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val userId = authRepository.getCurrentUserId()
            currentUserId = userId

            if (userId != null) {
                when (val roleRes = authRepository.getUserRole(userId)) {
                    is ResultState.Success -> {
                        val role = roleRes.data.lowercase()
                        isOwner = role == "owner" || role == "admin"
                    }
                    is ResultState.Error -> {
                        isOwner = false
                    }
                }

                if (isOwner) {
                    loadAllResignations()
                } else {
                    loadActiveResignation(userId)
                }
            } else {
                errorMessage = "Sesi pengguna tidak ditemukan."
            }
            isLoading = false
        }
    }

    fun refreshData() {
        val userId = currentUserId ?: return
        if (isOwner) {
            loadAllResignations()
        } else {
            loadActiveResignation(userId)
        }
    }

    fun loadActiveResignation(userId: String) {
        resignationRepository.getActiveResignationByUser(userId).onEach { result ->
            when (result) {
                is ResultState.Success -> {
                    activeResignation = result.data
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
        }.launchIn(viewModelScope)
    }

    fun loadAllResignations() {
        resignationRepository.getAllResignations().onEach { result ->
            when (result) {
                is ResultState.Success -> {
                    allResignations = result.data
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
        }.launchIn(viewModelScope)
    }

    fun submitResignation() {
        val userId = currentUserId ?: run {
            errorMessage = "User ID tidak ditemukan"
            return
        }

        val reason = reasonInput.trim()
        val lastDay = proposedLastDayInput.trim()

        if (reason.isEmpty()) {
            errorMessage = "Alasan pengajuan resign wajib diisi."
            return
        }

        if (lastDay.isEmpty()) {
            errorMessage = "Tanggal hari terakhir kerja wajib dipilih."
            return
        }

        val request = CreateResignationRequest(
            user_id = userId,
            reason = reason,
            proposed_last_day = lastDay
        )

        resignationRepository.submitResignation(request).onEach { result ->
            when (result) {
                is ResultState.Success -> {
                    successMessage = "Pengajuan resign berhasil dikirim!"
                    reasonInput = ""
                    proposedLastDayInput = ""
                    loadActiveResignation(userId)
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
        }.launchIn(viewModelScope)
    }

    fun approveResignation(resignationId: String) {
        processResignation(resignationId = resignationId, isApproved = true)
    }

    fun rejectResignation(resignationId: String, rejectionReason: String) {
        if (rejectionReason.isBlank()) {
            errorMessage = "Alasan penolakan wajib diisi."
            return
        }
        processResignation(resignationId = resignationId, isApproved = false, rejectionReason = rejectionReason)
    }

    private fun processResignation(
        resignationId: String,
        isApproved: Boolean,
        rejectionReason: String? = null
    ) {
        val req = ProcessResignationRequest(
            resignation_id = resignationId,
            status = if (isApproved) "approved" else "rejected",
            approved_by = currentUserId,
            rejection_reason = rejectionReason?.ifBlank { null },
            processed_at = LocalDate.now().toString()
        )

        resignationRepository.processResignation(req).onEach { result ->
            when (result) {
                is ResultState.Success -> {
                    successMessage = if (isApproved) "Pengajuan resign disetujui." else "Pengajuan resign ditolak."
                    selectedResignationToReject = null
                    rejectionReasonInput = ""
                    loadAllResignations()
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
        }.launchIn(viewModelScope)
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}
