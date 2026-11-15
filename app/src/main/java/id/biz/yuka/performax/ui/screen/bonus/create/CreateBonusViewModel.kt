package id.biz.yuka.performax.ui.screen.bonus.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.BonusRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.request.CreateBonusRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CreateBonusViewModel @Inject constructor(
    private val bonusRepository: BonusRepository,
    private val employeeRepository: EmployeeRepository, // Inject Repo Karyawan
    private val authRepository: AuthRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var isSuccess by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Data dropdown karyawan
    var employees by mutableStateOf<List<EmployeeEntity>>(emptyList())

    init {
        loadEmployees()
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = employeeRepository.getAllEmployees()
            if (result is ResultState.Success) {
                // Tampilkan hanya karyawan yang masih aktif
                employees = result.data.filter { it.isActive }
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message
            }

            isLoading = false
        }
    }

    fun submitBonus(
        selectedEmployee: EmployeeEntity?,
        amountStr: String,
        notes: String
    ) {
        viewModelScope.launch {
            if (selectedEmployee == null) {
                errorMessage = "Pilih karyawan terlebih dahulu."
                return@launch
            }

            // Bersihkan format angka (jika ada titik/koma) lalu ubah ke Double
            val amount = amountStr.replace("[^\\d]".toRegex(), "").toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                errorMessage = "Nominal bonus tidak valid."
                return@launch
            }

            if (notes.isBlank()) {
                errorMessage = "Catatan bonus tidak boleh kosong."
                return@launch
            }

            isLoading = true
            errorMessage = null

            // Dapatkan ID Owner yang sedang login
            val currentOwnerId = authRepository.getCurrentUserId()
            if (currentOwnerId == null) {
                errorMessage = "Sesi login tidak valid."
                isLoading = false
                return@launch
            }

            // Susun Request
            val request = CreateBonusRequest(
                user_id = selectedEmployee.id,
                user_name = selectedEmployee.fullName, // Menggunakan fullName dari EmployeeEntity
                granted_by = currentOwnerId,
                amount = amount,
                notes = notes,
                bonus_date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) // Set ke hari ini
            )

            // Eksekusi API
            val result = bonusRepository.addBonus(request)

            if (result is ResultState.Success) {
                isSuccess = true
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal menyimpan bonus"
            }

            isLoading = false
        }
    }
}