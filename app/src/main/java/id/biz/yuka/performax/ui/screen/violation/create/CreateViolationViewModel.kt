package id.biz.yuka.performax.ui.screen.violation.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.EmployeeRuleEntity
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.EmployeeRuleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.CreateViolationLogRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateViolationViewModel @Inject constructor(
    private val ruleRepository: EmployeeRuleRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository,
    private val employeeRepository: EmployeeRepository // INJECT EMPLOYEE REPOSITORY
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)

    // Data Sumber Dropdown
    var misconductRules by mutableStateOf<List<EmployeeRuleEntity>>(emptyList())
    var stores by mutableStateOf<List<StoreEntity>>(emptyList())
    var employees by mutableStateOf<List<EmployeeEntity>>(emptyList()) // GANTI TIPE KE ENTITY ASLI

    init {
        loadFormOptions()
    }

    private fun loadFormOptions() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // EKSEKUSI PENGAMBILAN DATA SECARA PARALEL (LEBIH CEPAT)
            val rulesDeferred = async { ruleRepository.getAllRules() }
            val storesDeferred = async { storeRepository.getAllStores() }
            val employeesDeferred = async { employeeRepository.getAllEmployees() } // AMBIL DATA KARYAWAN

            val rulesResult = rulesDeferred.await()
            val storesResult = storesDeferred.await()
            val employeesResult = employeesDeferred.await()

            if (rulesResult is ResultState.Success) {
                // FILTER: Hanya ambil aturan yang berjenis pelanggaran manual (misconduct)
                misconductRules = rulesResult.data.filter { it.triggerType == "misconduct" }
            } else if (rulesResult is ResultState.Error) {
                errorMessage = rulesResult.exception?.message
            }

            if (storesResult is ResultState.Success) {
                stores = storesResult.data
            } else if (storesResult is ResultState.Error) {
                errorMessage = storesResult.exception?.message
            }

            if (employeesResult is ResultState.Success) {
                employees = employeesResult.data // SIMPAN DATA KARYAWAN KE STATE
            } else if (employeesResult is ResultState.Error) {
                errorMessage = employeesResult.exception?.message
            }

            isLoading = false
        }
    }

    fun submitViolation(
        userId: String,
        storeId: String,
        ruleId: String,
        notes: String,
        appliedPenalty: String
    ) {
        viewModelScope.launch {
            isLoading = true

            // MENGAMBIL USER ID YANG SEDANG LOGIN (Owner/Manager yang melaporkan)
            val currentOwnerId = authRepository.getCurrentUserId()

            if (currentOwnerId == null) {
                errorMessage = "Sesi login tidak valid. Tidak bisa menyimpan teguran."
                isLoading = false
                return@launch
            }
            val targetEmployee = employees.find { it.id == userId }
            val userName = targetEmployee?.fullName ?: "Unknown"
            val request = CreateViolationLogRequest(
                user_id = userId,
                user_name = userName,
                store_id = storeId,
                rule_id = ruleId,
                reported_by = currentOwnerId,
                violation_notes = notes.ifBlank { null },
                applied_penalty = appliedPenalty,
                violation_date = java.time.LocalDate.now().toString()
            )

            val result = ruleRepository.addViolationLog(request)

            if (result is ResultState.Success) {
                isSuccess = true
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal menyimpan teguran"
            }

            isLoading = false
        }
    }
}