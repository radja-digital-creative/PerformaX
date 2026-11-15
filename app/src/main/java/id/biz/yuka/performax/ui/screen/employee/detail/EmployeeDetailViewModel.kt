package id.biz.yuka.performax.ui.screen.employee.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.DeductionComponentEntity
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.RoleEntity
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.RoleRepository
import id.biz.yuka.libperformx.repository.SalaryRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.repository.UserRepository
import id.biz.yuka.libperformx.request.UpdateEmployeeRequest
import id.biz.yuka.libperformx.request.UserStoreRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
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
class EmployeeDetailViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val storeRepository: StoreRepository,
    private val salaryRepository: SalaryRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val employeeId: String = savedStateHandle.get<String>("employeeId") ?: ""

    var employee by mutableStateOf<EmployeeEntity?>(null)
    var isLoading by mutableStateOf(false)
    var isOwner by mutableStateOf(false)

    var isEditMode by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    // State untuk dialog konfirmasi hapus karyawan
    var showDeleteDialog by mutableStateOf(false)
    var showRoleSheet by mutableStateOf(false)
    var allRoles = mutableStateListOf<RoleEntity>()
    var selectedRoleId by mutableIntStateOf(0)
    var isUpdatingRole by mutableStateOf(false)

    // Form Kredensial & Personal
    var editEmail by mutableStateOf("")
    var editPassword by mutableStateOf("")
    var editName by mutableStateOf("")
    var editPhone by mutableStateOf("")
    var editAddress by mutableStateOf("")
    var editJoinDate by mutableStateOf("") // ✅ TAMBAHAN: State Join Date

    // Form Finansial
    var editPayrollType by mutableStateOf("monthly")
    var editPayPerDay by mutableStateOf("")
    var editPerMinute by mutableStateOf("")
    var editBasicSalary by mutableStateOf("")
    var editTransportAllowance by mutableStateOf("")
    var editMealAllowance by mutableStateOf("")
    var editPositionAllowance by mutableStateOf("")

    // State Multi-Store
    var storeAssignments = mutableStateListOf<StoreAssignmentState>()
    var selectedTabIndex by mutableStateOf(0)
    var availableStores = mutableStateListOf<StoreEntity>()

    // State Multiple Deductions
    var availableDeductions = mutableStateListOf<DeductionComponentEntity>()
    var selectedDeductionIds = mutableStateListOf<String>()

    init {
        fetchStores()
        fetchDeductions()
        fetchRoles()
        loadDetail()
    }

    private fun fetchRoles() {
        viewModelScope.launch {
            val result = roleRepository.getAllRoles()
            if (result is ResultState.Success) {
                allRoles.clear()
                allRoles.addAll(result.data)
            }
        }
    }

    fun openRoleSheet() {
        val currentEmp = employee ?: return
        val currentRole = allRoles.find { it.roleName.lowercase() == currentEmp.roleName.lowercase() }
        selectedRoleId = currentRole?.id ?: 0
        showRoleSheet = true
    }

    fun updateRole() {
        if (selectedRoleId == 0) return
        val currentEmp = employee ?: return
        viewModelScope.launch {
            isUpdatingRole = true
            val result = userRepository.updateEmployeeRole(currentEmp.id, selectedRoleId)
            if (result is ResultState.Success) {
                successMessage = "Jabatan berhasil diubah!"
                showRoleSheet = false
                loadDetail()
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isUpdatingRole = false
        }
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

    // ... (Sisa fungsi loadDetail, populateEditForm, toggleDeduction, saveChanges TETAP SAMA seperti kode sebelumnya) ...
    fun loadDetail() {
        viewModelScope.launch {
            isLoading = true
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id

            if (currentUserId != null) {
                val meRes = employeeRepository.getEmployeeById(currentUserId)
                if (meRes is ResultState.Success) {
                    val role = meRes.data.roleName.lowercase()
                    isOwner = role == "owner" || role == "admin"
                }
            }

            val result = employeeRepository.getEmployeeById(employeeId)
            if (result is ResultState.Success) {
                employee = result.data
                populateEditForm(result.data)
            }
            isLoading = false
        }
    }

    private fun populateEditForm(data: EmployeeEntity) {
        editEmail = ""
        editPassword = ""

        editName = data.fullName
        editPhone = data.phoneNumber
        editAddress = data.address
        editJoinDate = data.joinDate?.take(10) ?: "" // ✅ POPULATE JOIN DATE

        editPayrollType = data.payrollType.name.lowercase()
        editPayPerDay = data.payPerDay.toLong().toString()
        editPerMinute = data.perMinute.toLong().toString()
        editBasicSalary = data.basicSalary.toLong().toString()
        editTransportAllowance = data.transportAllowance.toLong().toString()
        editMealAllowance = data.mealAllowance.toLong().toString()
        editPositionAllowance = data.positionAllowance.toLong().toString()

        selectedDeductionIds.clear()
        selectedDeductionIds.addAll(data.deductions.map { it.id })

        storeAssignments.clear()
        if (data.stores.isNotEmpty()) {
            data.stores.forEachIndexed { _, store ->
                storeAssignments.add(
                    StoreAssignmentState(
                        storeId = store.id,
                        storeName = store.name,
                        isPrimary = store.isPrimary
                    )
                )
            }
        } else {
            storeAssignments.add(StoreAssignmentState(isPrimary = true))
        }
        selectedTabIndex = 0
    }

    fun toggleDeduction(id: String) {
        if (selectedDeductionIds.contains(id)) {
            selectedDeductionIds.remove(id)
        } else {
            selectedDeductionIds.add(id)
        }
    }

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

    fun saveChanges() {
        val currentEmp = employee ?: return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val roleId = when(currentEmp.roleName.lowercase()) {
                "owner" -> 1; "admin" -> 2; "manager" -> 3; else -> 4
            }

            val pStores = storeAssignments.filter { it.storeId.isNotBlank() }.map {
                UserStoreRequest(it.storeId, it.isPrimary)
            }

            val request = UpdateEmployeeRequest(
                p_user_id = currentEmp.id,
                email = editEmail.takeIf { it.isNotBlank() },
                password = editPassword.takeIf { it.isNotBlank() },
                p_role_id = roleId,
                p_full_name = editName,
                p_gender = currentEmp.gender,
                p_address = editAddress,
                p_phone_number = editPhone,

                p_stores = pStores.ifEmpty { null },
                p_schedules = null,

                p_payroll_type = editPayrollType,
                p_pay_per_day = editPayPerDay.toDoubleOrNull() ?: currentEmp.payPerDay,
                p_per_minute = editPerMinute.toDoubleOrNull() ?: currentEmp.perMinute,
                p_basic_salary = editBasicSalary.toDoubleOrNull() ?: currentEmp.basicSalary,
                p_transport_allowance = editTransportAllowance.toDoubleOrNull() ?: currentEmp.transportAllowance,
                p_meal_allowance = editMealAllowance.toDoubleOrNull() ?: currentEmp.mealAllowance,
                p_position_allowance = editPositionAllowance.toDoubleOrNull() ?: currentEmp.positionAllowance,
                p_deduction_ids = selectedDeductionIds.toList(),
                p_join_date = editJoinDate.takeIf { it.isNotBlank() } // ✅ KIRIM KE REPO
            )

            val result = employeeRepository.updateEmployee(request)
            if (result is ResultState.Success) {
                successMessage = "Data berhasil diperbarui!"
                isEditMode = false
                loadDetail()
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }

    /** SOFT DELETE: Mengubah is_active = false di tabel profiles */
    fun deleteEmployee(onSuccess: () -> Unit) {
        val currentEmp = employee ?: return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                android.util.Log.d("DeleteEmployee", ">>> VM TRY START: userId=${currentEmp.id}, name=${currentEmp.fullName}")
                val result = employeeRepository.deleteEmployee(currentEmp.id)
                if (result is id.biz.yuka.libperformx.utils.ResultState.Success) {
                    android.util.Log.d("DeleteEmployee", ">>> VM TRY SUCCESS: userId=${currentEmp.id}")
                    onSuccess()
                } else if (result is id.biz.yuka.libperformx.utils.ResultState.Error) {
                    android.util.Log.e("DeleteEmployee", ">>> VM RESULT ERROR: userId=${currentEmp.id}, error=${result.message}", result.exception)
                    errorMessage = "Gagal menonaktifkan karyawan: ${result.message}"
                    showDeleteDialog = false
                }
            } catch (e: Exception) {
                android.util.Log.e("DeleteEmployee", ">>> VM UNHANDLED CATCH: ${e.message}", e)
                e.printStackTrace()
                errorMessage = "Terjadi kesalahan: ${e.message}"
                showDeleteDialog = false
            } finally {
                isLoading = false
            }
        }
    }
}