package id.biz.yuka.performax.ui.screen.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var profile by mutableStateOf<EmployeeEntity?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadMyProfile()
    }

    fun loadMyProfile() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = employeeRepository.getEmployeeById(userId)
                if (result is ResultState.Success) {
                    profile = result.data
                } else if (result is ResultState.Error) {
                    errorMessage = result.message
                }
            } else {
                errorMessage = "Sesi tidak ditemukan. Silakan login kembali."
            }
            isLoading = false
        }
    }
}