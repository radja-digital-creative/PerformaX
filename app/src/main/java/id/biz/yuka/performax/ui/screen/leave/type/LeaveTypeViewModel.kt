package id.biz.yuka.performax.ui.screen.leave.type

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.LeaveTypeEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.repository.LeaveRepository
import id.biz.yuka.libperformx.request.CreateLeaveTypeRequest
import id.biz.yuka.libperformx.request.UpdateLeaveTypeRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaveTypeViewModel @Inject constructor(
    private val repository: LeaveRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var leaveTypes by mutableStateOf<List<LeaveTypeEntity>>(emptyList())

    init {
        loadLeaveTypes()
    }

    fun loadLeaveTypes() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            when (val result = repository.getLeaveTypes()) {
                is ResultState.Success -> {
                    leaveTypes = result.data.map { it.toEntity() }
                }
                is ResultState.Error -> {
                    errorMessage = result.exception?.message ?: "Terjadi kesalahan"
                }
            }
            isLoading = false
        }
    }

    fun addLeaveType(request: CreateLeaveTypeRequest) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.createLeaveType(request)
            if (result is ResultState.Success) {
                loadLeaveTypes()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal menambah data"
                isLoading = false
            }
        }
    }

    fun updateLeaveType(id: String, request: UpdateLeaveTypeRequest) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.updateLeaveType(id, request)
            if (result is ResultState.Success) {
                loadLeaveTypes()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal mengubah data"
                isLoading = false
            }
        }
    }

    fun deleteLeaveType(id: String) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.deleteLeaveType(id)
            if (result is ResultState.Success) {
                loadLeaveTypes()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal menghapus data"
                isLoading = false
            }
        }
    }
}