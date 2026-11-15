package id.biz.yuka.performax.ui.screen.leave

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.LeaveEntity
import id.biz.yuka.libperformx.repository.LeaveRepository
import id.biz.yuka.libperformx.repository.UserRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var leaves by mutableStateOf<List<LeaveEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isOwnerOrManager by mutableStateOf(false)

    init { loadLeaves() }

    fun loadLeaves() {
        viewModelScope.launch {
            isLoading = true
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch

            // Cek Role User
            val profileRes = userRepository.getUserProfile(userId)
            if (profileRes is ResultState.Success) {
                val role = profileRes.data.role.lowercase()
                isOwnerOrManager = role == "owner" || role == "manager" || role == "admin"
            }

            val result = leaveRepository.getLeaveRequests()
            if (result is ResultState.Success) {
                // Filter: Owner melihat semua, Karyawan melihat miliknya saja
                leaves = if (isOwnerOrManager) {
                    result.data
                } else {
                    result.data.filter { it.userId == userId }
                }
            }
            isLoading = false
        }
    }
}