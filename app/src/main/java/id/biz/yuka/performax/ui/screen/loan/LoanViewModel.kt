package id.biz.yuka.performax.ui.screen.loan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.EmployeeLoanEligibilityEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.LoanRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    var currentUserId by mutableStateOf<String?>(null)
        private set
    var isOwnerOrAdmin by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // State Karyawan Eligible
    var eligibleEmployees by mutableStateOf<List<EmployeeLoanEligibilityEntity>>(emptyList())
        private set

    // State Semua Karyawan (untuk modal kelola status canRequestLoan)
    var allEmployees by mutableStateOf<List<EmployeeEntity>>(emptyList())
        private set
    var isLoadingAllEmployees by mutableStateOf(false)
        private set

    init {
        initData()
    }

    fun initData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            currentUserId = userId

            if (userId != null) {
                val userRes = employeeRepository.getEmployeeById(userId)
                if (userRes is ResultState.Success) {
                    val role = userRes.data.roleName.lowercase()
                    isOwnerOrAdmin = role == "owner" || role == "admin"
                }

                loadEligibleEmployees()
            } else {
                errorMessage = "Sesi pengguna tidak ditemukan."
            }
            isLoading = false
        }
    }

    fun refreshData() {
        loadEligibleEmployees()
    }

    fun loadEligibleEmployees() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = loanRepository.getEligibleLoanEmployees()) {
                is ResultState.Success -> {
                    eligibleEmployees = result.data.sortedBy { it.fullName }
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    fun loadAllEmployees() {
        viewModelScope.launch {
            isLoadingAllEmployees = true
            errorMessage = null
            when (val result = employeeRepository.getAllEmployees()) {
                is ResultState.Success -> {
                    allEmployees = result.data.sortedBy { it.fullName }
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoadingAllEmployees = false
        }
    }

    fun toggleEmployeeLoanEligibility(employee: EmployeeEntity, newStatus: Boolean) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // Jika hendak menonaktifkan, pastikan karyawan tidak memiliki pinjaman aktif
            if (!newStatus) {
                when (val loanRes = loanRepository.getLoansAndInstallmentsByUserId(employee.id)) {
                    is ResultState.Success -> {
                        val hasActive = loanRes.data.any { loan ->
                            val s = loan.status.lowercase()
                            s == "pending" || s == "approved" || s == "active"
                        }
                        if (hasActive) {
                            errorMessage = "Karyawan ${employee.fullName} masih memiliki pinjaman aktif, tidak dapat menonaktifkan status loan."
                            isLoading = false
                            return@launch
                        }
                    }
                    is ResultState.Error -> {
                        errorMessage = "Gagal memverifikasi status pinjaman karyawan: ${loanRes.message}"
                        isLoading = false
                        return@launch
                    }
                }
            }

            when (val res = employeeRepository.updateCanRequestLoan(employee.id, newStatus)) {
                is ResultState.Success -> {
                    successMessage = "Status hak pinjaman ${employee.fullName} berhasil diperbarui!"
                    loadEligibleEmployees()
                    loadAllEmployees()
                }
                is ResultState.Error -> {
                    errorMessage = res.message
                }
            }
            isLoading = false
        }
    }
}
