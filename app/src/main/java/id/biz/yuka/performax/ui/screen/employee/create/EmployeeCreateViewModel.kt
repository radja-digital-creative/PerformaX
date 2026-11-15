package id.biz.yuka.performax.ui.screen.employee.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.DeductionComponentEntity // <-- IMPORT ENTITY BARU
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.SalaryRepository // <-- IMPORT SALARY REPO
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.RegisterEmployeeRequest
import id.biz.yuka.libperformx.request.UserStoreRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class StoreAssignmentState(
    val id: String = UUID.randomUUID().toString(),
    var storeId: String = "",
    var storeName: String = "",
    var isPrimary: Boolean = false
)

@HiltViewModel
class EmployeeCreateViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val storeRepository: StoreRepository,
    private val salaryRepository: SalaryRepository // <-- INJECT DI SINI
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    // Form Kredensial & Personal
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var fullName by mutableStateOf("")
    var phone by mutableStateOf("")
    var address by mutableStateOf("")
    var gender by mutableStateOf("male")
    var roleId by mutableStateOf(4)

    // Form Finansial
    var payrollType by mutableStateOf("monthly")
    var payPerDay by mutableStateOf("")
    var basicSalary by mutableStateOf("")
    var transportAllowance by mutableStateOf("")
    var mealAllowance by mutableStateOf("")
    var positionAllowance by mutableStateOf("")

    // State Multi-Store
    var storeAssignments = mutableStateListOf(StoreAssignmentState(isPrimary = true))
    var selectedTabIndex by mutableStateOf(0)
    var availableStores = mutableStateListOf<StoreEntity>()

    // State Multiple Deductions (Gunakan Entity Baru)
    var availableDeductions = mutableStateListOf<DeductionComponentEntity>()
    var selectedDeductionIds = mutableStateListOf<String>()

    init {
        fetchStores()
        fetchDeductions()
    }

    private fun fetchStores() {
        viewModelScope.launch {
            val result = storeRepository.getAllStores()
            if (result is ResultState.Success) {
                availableStores.clear()
                availableStores.addAll(result.data)
            }
        }
    }

    // Fungsi Fetch menggunakan Repository
    private fun fetchDeductions() {
        viewModelScope.launch {
            val result = salaryRepository.getAllDeductionComponents()
            if (result is ResultState.Success) {
                availableDeductions.clear()
                availableDeductions.addAll(result.data)
            } else if (result is ResultState.Error) {
                errorMessage = "Gagal memuat daftar potongan: ${result.message}"
            }
        }
    }

    fun toggleDeduction(id: String) {
        if (selectedDeductionIds.contains(id)) {
            selectedDeductionIds.remove(id)
        } else {
            selectedDeductionIds.add(id)
        }
    }

    // ... (Sisa fungsi addStoreAssignment, removeStoreAssignment, setAsPrimaryStore tetap sama) ...
    fun addStoreAssignment() {
        val isFirst = storeAssignments.isEmpty()
        storeAssignments.add(StoreAssignmentState(isPrimary = isFirst))
        selectedTabIndex = storeAssignments.size - 1
    }

    fun removeStoreAssignment(index: Int) {
        if (storeAssignments.size > 1) {
            val removed = storeAssignments.removeAt(index)
            if (removed.isPrimary && storeAssignments.isNotEmpty()) {
                storeAssignments[0] = storeAssignments[0].copy(isPrimary = true)
            }
            selectedTabIndex = maxOf(0, selectedTabIndex - 1)
        }
    }

    fun setAsPrimaryStore(index: Int) {
        for (i in storeAssignments.indices) {
            storeAssignments[i] = storeAssignments[i].copy(isPrimary = (i == index))
        }
    }

    fun createEmployee() {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            errorMessage = "Email, Password, dan Nama Lengkap wajib diisi!"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val pStores = storeAssignments.filter { it.storeId.isNotBlank() }.map {
                UserStoreRequest(it.storeId, it.isPrimary)
            }

            val dayRate = payPerDay.toDoubleOrNull() ?: 0.0
            val autoBasicSalary = dayRate * 26.0

            val request = RegisterEmployeeRequest(
                email = email.trim(),
                password = password,
                p_role_id = roleId,
                p_full_name = fullName.trim(),
                p_gender = gender,
                p_address = address,
                p_phone_number = phone,
                p_id_card_url = "",
                p_face_reference_url = "",
                p_stores = pStores,
                p_schedules = emptyList(),
                p_payroll_type = payrollType,
                p_pay_per_day = dayRate,
                p_basic_salary = autoBasicSalary,
                p_transport_allowance = transportAllowance.toDoubleOrNull() ?: 0.0,
                p_meal_allowance = mealAllowance.toDoubleOrNull() ?: 0.0,
                p_position_allowance = positionAllowance.toDoubleOrNull() ?: 0.0,
                p_deduction_ids = selectedDeductionIds.toList()
            )

            val result = employeeRepository.createEmployee(request)
            if (result is ResultState.Success) {
                successMessage = "Karyawan berhasil didaftarkan!"
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }
}