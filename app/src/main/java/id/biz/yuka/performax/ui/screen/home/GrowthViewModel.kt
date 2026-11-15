package id.biz.yuka.performax.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GrowthViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var myUserId by mutableStateOf("")
    var tasksList by mutableStateOf<List<TaskEntity>>(emptyList())

    init { fetchGrowthData() }

    fun fetchGrowthData() {
        viewModelScope.launch {
            isLoading = true
            myUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch

            val taskRes = taskRepository.getTasks()
            if (taskRes is ResultState.Success) {
                tasksList = taskRes.data
            }
            isLoading = false
        }
    }
}