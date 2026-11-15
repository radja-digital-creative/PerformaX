package id.biz.yuka.performax.ui.screen.employee

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var employees by mutableStateOf<List<EmployeeEntity>>(emptyList())
    var isLoading by mutableStateOf(false)

    // Hak Akses
    var canViewDetails by mutableStateOf(false)
    var isOwnerOrAdmin by mutableStateOf(false)

    init { loadEmployees() }

    fun loadEmployees() {
        viewModelScope.launch {
            isLoading = true
            val userId = authRepository.getCurrentUserId()

            if (userId != null) {
                // 1. Ambil Role & Daftar Toko secara async
                val roleJob = async { authRepository.getUserRole(userId) }
                val storesJob = async { storeRepository.getMyStores(userId) }
                
                val roleRes = roleJob.await()
                val storesRes = storesJob.await()

                if (roleRes is ResultState.Success) {
                    val role = roleRes.data.lowercase()
                    isOwnerOrAdmin = role == "owner" || role == "admin"
                    canViewDetails = isOwnerOrAdmin || role == "manager"

                    // 2. Jika Owner/Admin -> Ambil Semua. Jika Manager -> Ambil berdasarkan semua toko yang dikelola
                    val result = if (isOwnerOrAdmin) {
                        employeeRepository.getAllEmployees(null)
                    } else if (storesRes is ResultState.Success) {
                        val myStoreIds = storesRes.data.map { it.id }
                        employeeRepository.getEmployeesByStores(myStoreIds)
                    } else {
                        ResultState.Success(emptyList())
                    }

                    if (result is ResultState.Success) {
                        // Urutkan berdasarkan nama
                        employees = result.data.sortedBy { it.fullName }
                    }
                }
            }
            isLoading = false
        }
    }
}