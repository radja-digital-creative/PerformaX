package id.biz.yuka.performax.ui.screen.manualdeductions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.data.remote.dto.CreateManualDeductionRequest
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.ManualDeductionEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.ManualDeductionRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManualDeductionsUiState(
    val isLoading: Boolean = false,
    val deductions: List<ManualDeductionEntity> = emptyList(),
    val employees: List<EmployeeEntity> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSubmitting: Boolean = false,
    val showAddSheet: Boolean = false
)

@HiltViewModel
class ManualDeductionsViewModel @Inject constructor(
    private val repository: ManualDeductionRepository,
    private val employeeRepository: EmployeeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val userSalaryId: String = savedStateHandle.get<String>("userSalaryId") ?: "all"

    var uiState by mutableStateOf(ManualDeductionsUiState())
        private set

    var selectedEmployee by mutableStateOf<EmployeeEntity?>(null)
    var titleInput by mutableStateOf("")
    var amountInput by mutableStateOf("")
    var notesInput by mutableStateOf("")

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            
            // Load Employees
            val empRes = employeeRepository.getAllEmployees()
            val employeeList = if (empRes is ResultState.Success) empRes.data else emptyList()

            // Load Deductions
            when (val result = repository.getManualDeductionsByUserSalary(userSalaryId)) {
                is ResultState.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        employees = employeeList,
                        deductions = result.data
                    )
                }
                is ResultState.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        employees = employeeList,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun openAddSheet() {
        if (uiState.employees.isEmpty()) {
            loadData()
        }
        uiState = uiState.copy(showAddSheet = true)
    }

    fun closeAddSheet() {
        uiState = uiState.copy(showAddSheet = false)
        selectedEmployee = null
        titleInput = ""
        amountInput = ""
        notesInput = ""
    }

    fun submitManualDeduction() {
        val targetEmployee = selectedEmployee ?: run {
            uiState = uiState.copy(errorMessage = "Pilih karyawan terlebih dahulu.")
            return
        }

        val title = titleInput.trim()
        val amount = amountInput.toDoubleOrNull() ?: 0.0

        if (title.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Judul potongan tidak boleh kosong.")
            return
        }

        if (amount <= 0) {
            uiState = uiState.copy(errorMessage = "Nominal potongan harus lebih besar dari 0.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, errorMessage = null)
            val request = CreateManualDeductionRequest(
                userSalaryId = targetEmployee.id,
                title = title,
                amount = amount,
                notes = notesInput.ifBlank { null }
            )
            when (val result = repository.createManualDeduction(request)) {
                is ResultState.Success -> {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        successMessage = "Potongan manual untuk ${targetEmployee.fullName} berhasil disimpan!"
                    )
                    closeAddSheet()
                    loadData()
                }
                is ResultState.Error -> {
                    uiState = uiState.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
    }

    fun deleteManualDeduction(id: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            when (val result = repository.deleteManualDeduction(id)) {
                is ResultState.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = "Potongan manual berhasil dihapus!"
                    )
                    loadData()
                }
                is ResultState.Error -> {
                    uiState = uiState.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(errorMessage = null, successMessage = null)
    }
}
