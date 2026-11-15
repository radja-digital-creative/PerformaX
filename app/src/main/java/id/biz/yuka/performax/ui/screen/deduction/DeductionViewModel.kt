package id.biz.yuka.performax.ui.screen.deduction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.DeductionEntity
import id.biz.yuka.libperformx.request.CreateDeductionRequest
import id.biz.yuka.libperformx.request.UpdateDeductionRequest
import id.biz.yuka.libperformx.repository.DeductionRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeductionViewModel @Inject constructor(
    private val repository: DeductionRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var deductions by mutableStateOf<List<DeductionEntity>>(emptyList())

    init {
        loadDeductions()
    }

    fun loadDeductions() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            when (val result = repository.getAllDeductions()) {
                is ResultState.Success -> {
                    deductions = result.data
                }
                is ResultState.Error -> {
                    errorMessage = result.exception?.message ?: "Terjadi kesalahan saat memuat data"
                }
            }
            isLoading = false
        }
    }

    fun addDeduction(request: CreateDeductionRequest) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.createDeduction(request)
            if (result is ResultState.Success) {
                loadDeductions() // Refresh data otomatis
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal menambah data"
                isLoading = false
            }
        }
    }

    fun updateDeduction(id: String, request: UpdateDeductionRequest) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.updateDeduction(id, request)
            if (result is ResultState.Success) {
                loadDeductions()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal mengubah data"
                isLoading = false
            }
        }
    }

    fun deleteDeduction(id: String) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.deleteDeduction(id)
            if (result is ResultState.Success) {
                loadDeductions()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal menghapus data"
                isLoading = false
            }
        }
    }
}