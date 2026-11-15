package id.biz.yuka.performax.ui.screen.task.history

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
class HistoryTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    var tasks by mutableStateOf<List<TaskEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadHistoryTasks()
    }

    fun loadHistoryTasks() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id

            if (currentUserId == null) {
                errorMessage = "Sesi tidak ditemukan. Silakan login ulang."
                isLoading = false
                return@launch
            }

            val result = taskRepository.getTasks()

            when (result) {
                is ResultState.Success -> {
                    // Filter tugas:
                    // 1. Status sudah tidak aktif (completed, verified, canceled)
                    // 2. User adalah pembuat (creator) ATAU ditugaskan (assignee)
                    tasks = result.data.filter { task ->
                        val isFinished = task.assignees.any { it.status in listOf("completed", "verified", "canceled") }
                        val isCreator = task.creatorId == currentUserId
                        val isAssignee = task.assignees.any { it.userId == currentUserId }

                        isFinished && (isCreator || isAssignee)
                    }
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }
}