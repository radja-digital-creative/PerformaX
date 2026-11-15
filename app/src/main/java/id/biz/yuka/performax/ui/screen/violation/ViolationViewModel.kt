package id.biz.yuka.performax.ui.screen.violation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.ViolationLogEntity
import id.biz.yuka.libperformx.repository.AppealRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRuleRepository
import id.biz.yuka.libperformx.request.CreateAppealRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ViolationViewModel @Inject constructor(
    private val ruleRepository: EmployeeRuleRepository,
    private val authRepository: AuthRepository,
    private val appealRepository: AppealRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Role Dinamis & ID User dari Auth Repo
    var currentUserId by mutableStateOf<String?>(null)
    var isOwner by mutableStateOf(false)

    // Data Pelanggaran
    var violations by mutableStateOf<List<ViolationLogEntity>>(emptyList())

    // Navigasi Bulan
    var selectedDate by mutableStateOf(LocalDate.now())
    var currentMonthStr by mutableStateOf("")

    // Filter Chips ("Semua", "Pending", "Processed")
    var selectedFilter by mutableStateOf("Semua")

    // Filter data yang akan ditampilkan di UI
    val displayedViolations: List<ViolationLogEntity>
        get() = when (selectedFilter) {
            "Pending" -> violations.filter { !it.isProcessedToPayroll }
            "Processed" -> violations.filter { it.isProcessedToPayroll }
            else -> violations
        }

    init {
        // Panggil inisialisasi awal secara berurutan untuk mencegah race condition
        viewModelScope.launch {
            isLoading = true

            // 1. Dapatkan User ID
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                currentUserId = userId
                // 2. Dapatkan Role
                val roleResult = authRepository.getUserRole(userId)
                if (roleResult is ResultState.Success) {
                    isOwner = roleResult.data == "owner"
                }
            }

            // 3. Set Teks Bulan
            updateMonthString()

            // 4. Load Data Pelanggaran sesuai Role
            loadViolationsInternal()
        }
    }

    private fun updateMonthString() {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))
        currentMonthStr = selectedDate.format(formatter)
    }

    fun changeMonth(monthsToAdd: Long) {
        selectedDate = selectedDate.plusMonths(monthsToAdd)
        updateMonthString()
        loadViolations()
    }

    fun loadViolations() {
        viewModelScope.launch {
            loadViolationsInternal()
        }
    }

    // Fungsi internal dipisah agar bisa dipanggil setelah check Role selesai
    private suspend fun loadViolationsInternal() {
        isLoading = true
        errorMessage = null
        violations = emptyList() // Kosongkan list agar UI merespon langsung saat pindah bulan

        // Format tanggal awal dan akhir bulan yang dipilih
        val startOfMonth = selectedDate.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endOfMonth = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth()).format(DateTimeFormatter.ISO_LOCAL_DATE)

        // LOGIKA PENENTUAN PENGAMBILAN DATA BERDASARKAN ROLE
        val result = if (isOwner) {
            ruleRepository.getAllViolationLogs(startDate = startOfMonth, endDate = endOfMonth)
        } else {
            if (currentUserId != null) {
                ruleRepository.getViolationLogsByUserId(
                    userId = currentUserId!!,
                    startDate = startOfMonth,
                    endDate = endOfMonth
                )
            } else {
                null
            }
        }

        if (result is ResultState.Success) {
            violations = result.data.sortedByDescending { it.violationDate }
        } else if (result is ResultState.Error) {
            errorMessage = result.exception?.message
        }

        isLoading = false
    }

    // Fungsi untuk Owner: Proses Pelanggaran
    fun processViolation(logId: String) {
        viewModelScope.launch {
            isLoading = true

            val result = ruleRepository.processViolationLog(logId)

            if (result is ResultState.Success) {
                loadViolationsInternal()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal memproses pelanggaran"
            }

            isLoading = false
        }
    }

    // Fungsi untuk Karyawan: Ajukan Banding
    fun submitAppeal(logId: String, onSuccess: () -> Unit) { // <--- TAMBAHKAN PARAMETER onSuccess
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            currentUserId?.let { uid ->
                // 1. Siapkan Request untuk Create Appeal
                val request = CreateAppealRequest(
                    violation_log_id = logId,
                    employee_id = uid
                )

                // 2. Eksekusi tembak ke API
                val result = appealRepository.createAppeal(request)

                if (result is ResultState.Success) {
                    loadViolationsInternal() // Refresh data tab pelanggaran
                    onSuccess() // Panggil callback agar UI pindah tab otomatis
                } else if (result is ResultState.Error) {
                    errorMessage = result.exception?.message ?: "Gagal mengajukan banding"
                }
            }

            isLoading = false
        }
    }
}