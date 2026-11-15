package id.biz.yuka.performax.ui.screen.task.detail

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.utils.ChatEventBus
import id.biz.yuka.performax.utils.ChatRoomObserver
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel(), ChatRoomObserver {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    var task by mutableStateOf<TaskEntity?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    var userRole by mutableStateOf("employee")

    var commentText by mutableStateOf("")
    var isSendingComment by mutableStateOf(false)

    var isDeleted by mutableStateOf(false)
    var urgentId by mutableStateOf<String?>(null)

    // State for image previews (URIs from gallery or camera temp files)
    val selectedImages = mutableStateListOf<Uri>()

    val isCreator: Boolean
        get() {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            return task?.creatorId == currentUserId
        }

    val isAssignee: Boolean
        get() {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            return task?.assignees?.any { it.userId == currentUserId } == true
        }

    val isManager: Boolean
        get() = userRole.lowercase() == "manager" || userRole.lowercase() == "owner" || userRole.lowercase() == "admin"

    val canComment: Boolean
        get() = isCreator || isAssignee || isManager

    init {
        loadTaskDetail()
        observeIncomingFCMChats()
    }

    // 1. Ubah pengamat (Observer)
    override fun observeIncomingFCMChats() {
        viewModelScope.launch {
            ChatEventBus.incomingEvents.collect { event ->
                // Filter hanya bereaksi jika tipenya "task" dan ID-nya cocok
                if (event.type == "task" && event.id == taskId) {
                    loadTaskDetail()
                }
            }
        }
    }

    // 2. Ubah fungsi penanda Room Aktif
    override fun setAsActiveRoom() {
        ChatEventBus.setActiveRoom("task", taskId)
    }

    // 3. Ubah fungsi penghapus Room Aktif
    override fun clearActiveRoom() {
        ChatEventBus.clearActiveRoom("task")
    }

    fun loadTaskDetail() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                val roleRes = authRepository.getUserRole(currentUserId)
                if (roleRes is ResultState.Success) {
                    userRole = roleRes.data.lowercase()
                }
            }

            val result = taskRepository.getTaskDetail(taskId)
            when (result) {
                is ResultState.Success -> {
                    task = result.data
                    
                    // Cek jika ada pengingat urgent untuk tugas ini
                    val urgentRes = taskRepository.getAllTaskUrgentReminders()
                    if (urgentRes is ResultState.Success) {
                        urgentId = urgentRes.data.find { it.taskId == taskId }?.id
                    }
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    // ✅ PERBAIKAN: CACHING FILE (Untuk mengatasi masalah izin Galeri hilang)
    fun addSelectedImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Baca data dari Galeri
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // 2. Buat file baru di dalam memori internal aplikasi
                    val file = File(context.cacheDir, "gallery_${UUID.randomUUID()}.jpg")
                    val outputStream = FileOutputStream(file)

                    // 3. Pindahkan data ke dalam aplikasi
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()

                    // 4. Masukkan URL LOKAL ke dalam state agar 100% aman dikirim
                    val safeLocalUri = Uri.fromFile(file)
                    withContext(Dispatchers.Main) {
                        selectedImages.add(safeLocalUri)
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskDetailViewModel", "Gagal menyalin gambar dari galeri: ${e.message}")
            }
        }
    }

    // Kamera sudah menggunakan sistem Cache sejak awal, jadi tidak diubah
    fun handleCameraResult(context: Context, bitmap: Bitmap?) {
        if (bitmap == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "cam_${UUID.randomUUID()}.jpg")
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                val uri = Uri.fromFile(file)
                withContext(Dispatchers.Main) {
                    selectedImages.add(uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeSelectedImage(uri: Uri) {
        selectedImages.remove(uri)
    }

    fun sendComment(context: Context) {
        if (!canComment) {
            errorMessage = "Anda tidak memiliki izin untuk memberikan komentar pada tugas ini."
            return
        }
        if (commentText.isBlank() && selectedImages.isEmpty()) return

        Log.d("TaskDetailViewModel", "Sending comment: $commentText dengan ${selectedImages.size} gambar")
        viewModelScope.launch {
            isSendingComment = true
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""

            val result = if (selectedImages.isEmpty()) {
                taskRepository.addComment(taskId, currentUserId, commentText)
            } else {
                val imageBytes = selectedImages.mapNotNull { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        Log.e("TaskDetailViewModel", "Gagal membaca byte gambar: ${e.message}")
                        null
                    }
                }

                if (imageBytes.isNotEmpty()) {
                    taskRepository.addCommentWithImages(taskId, currentUserId, commentText, imageBytes)
                } else {
                    Log.e("TaskDetailViewModel", "Semua gambar gagal diproses menjadi byte. Mengirim teks saja.")
                    taskRepository.addComment(taskId, currentUserId, commentText)
                }
            }

            when (result) {
                is ResultState.Success -> {
                    commentText = ""
                    selectedImages.clear()
                    loadTaskDetail()
                }
                is ResultState.Error -> {
                    errorMessage = "Gagal mengirim komentar: ${result.message}"
                    Log.d("TaskDetailViewModel", "Gagal mengirim komentar: ${result.message}")
                }
            }
            isSendingComment = false
        }
    }

    fun updateAssignmentStatus(assignmentId: String, newStatus: String) {
        viewModelScope.launch {
            isLoading = true
            val result = taskRepository.updateAssignmentStatus(assignmentId, newStatus)
            if (result is ResultState.Success) {
                loadTaskDetail()
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            isLoading = true
            val result = taskRepository.deleteTask(taskId)
            if (result is ResultState.Success) {
                isDeleted = true
            } else if (result is ResultState.Error) {
                errorMessage = "Gagal menghapus tugas: ${result.message}"
            }
            isLoading = false
        }
    }
}