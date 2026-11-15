package id.biz.yuka.performax.ui.screen.task.settings.urgent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.libperformx.entity.TaskUrgentEntity
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskUrgentDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val urgentId: String = checkNotNull(savedStateHandle["urgentId"])

    var urgentReminder by mutableStateOf<TaskUrgentEntity?>(null)
    var taskTemplate by mutableStateOf<TaskEntity?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val result = taskRepository.getAllTaskUrgentReminders()
            if (result is ResultState.Success) {
                val found = result.data.find { it.id == urgentId }
                if (found != null) {
                    urgentReminder = found
                    
                    val taskRes = taskRepository.getTaskDetail(found.taskId)
                    if (taskRes is ResultState.Success) {
                        taskTemplate = taskRes.data
                    } else if (taskRes is ResultState.Error) {
                        errorMessage = "Gagal memuat template tugas: ${taskRes.message}"
                    }
                } else {
                    errorMessage = "Pengingat tidak ditemukan."
                }
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            
            isLoading = false
        }
    }

    fun toggleReminder(isActive: Boolean) {
        viewModelScope.launch {
            isLoading = true
            val result = taskRepository.toggleTaskUrgentReminder(urgentId, isActive)
            if (result is ResultState.Success) {
                urgentReminder = urgentReminder?.copy(isActive = isActive)
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }
}
