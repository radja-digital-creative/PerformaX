package id.biz.yuka.performax.ui.screen.loan.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.EmployeeLoanEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.LoanRepository
import id.biz.yuka.libperformx.request.ApproveLoanRpcRequest
import id.biz.yuka.libperformx.request.CreateLoanRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val loanRepository: LoanRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    val navUserId: String? = savedStateHandle.get<String>("userId")
    val isSelfView: Boolean = navUserId.isNullOrBlank() || navUserId == "me" || navUserId == "self"

    var currentUserId by mutableStateOf<String?>(null)
        private set
    var isOwnerOrAdmin by mutableStateOf(false)
        private set
    var canRequestLoan by mutableStateOf(true)
        private set

    var targetEmployee by mutableStateOf<EmployeeEntity?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    var loanList by mutableStateOf<List<EmployeeLoanEntity>>(emptyList())
        private set

    val hasActiveLoan: Boolean
        get() = loanList.any { loan ->
            val s = loan.status.lowercase()
            s == "pending" || s == "approved" || s == "active" || s == "waiting_employee_approval"
        }

    init {
        initData()
    }

    fun initData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val loggedInId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            currentUserId = loggedInId

            if (loggedInId != null) {
                val currentUserRes = employeeRepository.getEmployeeById(loggedInId)
                if (currentUserRes is ResultState.Success) {
                    val role = currentUserRes.data.roleName.lowercase()
                    isOwnerOrAdmin = role == "owner" || role == "admin"
                    canRequestLoan = currentUserRes.data.canRequestLoan
                }

                val effectiveTargetId = if (!navUserId.isNullOrBlank() && navUserId != "me" && navUserId != "self") {
                    navUserId
                } else {
                    loggedInId
                }

                if (effectiveTargetId != loggedInId) {
                    val targetRes = employeeRepository.getEmployeeById(effectiveTargetId)
                    if (targetRes is ResultState.Success) {
                        targetEmployee = targetRes.data
                    }
                }

                loadLoans(effectiveTargetId)
            } else {
                errorMessage = "Sesi pengguna tidak ditemukan."
            }
            isLoading = false
        }
    }

    fun refreshData() {
        val loggedInId = currentUserId ?: return
        val effectiveTargetId = if (!navUserId.isNullOrBlank() && navUserId != "me" && navUserId != "self") {
            navUserId
        } else {
            loggedInId
        }
        loadLoans(effectiveTargetId)
    }

    private fun loadLoans(targetId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = loanRepository.getLoansAndInstallmentsByUserId(targetId)) {
                is ResultState.Success -> {
                    loanList = result.data.sortedByDescending { it.createdAt }
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    fun submitLoanRequest(
        requestedAmount: Double,
        tenorMonths: Int,
        reason: String,
        onSuccess: () -> Unit
    ) {
        val userId = currentUserId ?: run {
            errorMessage = "User ID tidak valid"
            return
        }

        if (hasActiveLoan) {
            errorMessage = "Anda sudah memiliki 1 pinjaman yang sedang aktif / dalam proses pengajuan."
            return
        }

        if (requestedAmount <= 0) {
            errorMessage = "Nominal pinjaman harus lebih dari 0."
            return
        }

        if (tenorMonths <= 0) {
            errorMessage = "Tenor pinjaman minimal 1 bulan."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val req = CreateLoanRequest(
                user_id = userId,
                requested_amount = requestedAmount,
                requested_tenor_months = tenorMonths,
                reason = reason.ifBlank { null }
            )

            when (val result = loanRepository.createLoanRequest(req)) {
                is ResultState.Success -> {
                    successMessage = "Pengajuan pinjaman berhasil dikirim!"
                    loadLoans(userId)
                    onSuccess()
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    fun approveLoan(
        loanId: String,
        approvedAmount: Double,
        approvedTenor: Int,
        startDate: String? = null,
        onSuccess: () -> Unit
    ) {
        val hrdId = currentUserId ?: run {
            errorMessage = "User ID pengesah tidak valid"
            return
        }

        if (approvedAmount <= 0) {
            errorMessage = "Nominal persetujuan harus lebih dari 0."
            return
        }

        if (approvedTenor <= 0) {
            errorMessage = "Tenor persetujuan minimal 1 bulan."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val request = ApproveLoanRpcRequest(
                p_loan_id = loanId,
                p_hrd_id = hrdId,
                p_approved_amount = approvedAmount,
                p_approved_tenor = approvedTenor,
                p_start_date = startDate
            )

            when (val result = loanRepository.approveLoan(request)) {
                is ResultState.Success -> {
                    successMessage = "Pinjaman berhasil disetujui!"
                    val effectiveTargetId = if (!navUserId.isNullOrBlank() && navUserId != "me" && navUserId != "self") {
                        navUserId
                    } else {
                        hrdId
                    }
                    loadLoans(effectiveTargetId)
                    onSuccess()
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    fun respondLoanByEmployee(loanId: String, isApproved: Boolean) {
        val userId = currentUserId ?: run {
            errorMessage = "User ID tidak valid"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            when (val result = loanRepository.respondLoanByEmployee(loanId, isApproved)) {
                is ResultState.Success -> {
                    successMessage = if (isApproved) "Pengajuan pinjaman berhasil Anda setujui!" else "Pengajuan pinjaman berhasil ditolak."
                    val effectiveTargetId = if (!navUserId.isNullOrBlank() && navUserId != "me" && navUserId != "self") {
                        navUserId
                    } else {
                        userId
                    }
                    loadLoans(effectiveTargetId)
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }
}
