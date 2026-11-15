package id.biz.yuka.performax.ui.screen.task.edit

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.TaskAttachmentEntity
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.request.UpdateTaskRequest
import id.biz.yuka.libperformx.response.TaskLabelResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.ui.screen.task.create.TaskAttachment
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val employeeRepository: EmployeeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    var task by mutableStateOf<TaskEntity?>(null)

    // Validasi Keamanan
    var isAuthorized by mutableStateOf(true)
    var isVerifiedLocked by mutableStateOf(false)

    // Form State
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var deadline by mutableStateOf<LocalDate?>(null)

    // Data Master
    var employees by mutableStateOf<List<EmployeeEntity>>(emptyList())
    var availableLabels by mutableStateOf<List<TaskLabelResponse>>(emptyList())

    // Pilihan Pengguna
    val selectedAssignees = mutableStateListOf<EmployeeEntity>()
    val selectedLabels = mutableStateListOf<TaskLabelResponse>()

    // Manajemen Lampiran (Lama vs Baru)
    val existingAttachments = mutableStateListOf<TaskAttachmentEntity>()
    val newAttachments = mutableStateListOf<TaskAttachment>()

    var isLoading by mutableStateOf(false)
    var isInitialLoading by mutableStateOf(true)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            isInitialLoading = true

            // Tarik Master Data Dulu
            val empResult = employeeRepository.getAllEmployees()
            if (empResult is ResultState.Success) employees = empResult.data

            val labelResult = taskRepository.getAllTaskLabels()
            if (labelResult is ResultState.Success) availableLabels = labelResult.data

            // Tarik Detail Tugas
            val taskResult = taskRepository.getTaskDetail(taskId)
            if (taskResult is ResultState.Success) {
                val currentTask = taskResult.data
                task = currentTask

                // 🔒 CEK OTORISASI (HANYA CREATOR & BELUM VERIFIED)
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                if (currentTask.creatorId != currentUserId) {
                    isAuthorized = false
                }
                if (currentTask.assignees.any { it.status == "verified" }) {
                    isVerifiedLocked = true
                }

                // Petakan Data ke Form Jika Boleh Edit
                if (isAuthorized && !isVerifiedLocked) {
                    title = currentTask.title
                    description = currentTask.description
                    try {
                        deadline = LocalDate.parse(currentTask.dueDate.substring(0, 10))
                    } catch (e: Exception) { }

                    // Petakan Karyawan Lama
                    val oldAssigneeIds = currentTask.assignees.map { it.userId }
                    selectedAssignees.addAll(employees.filter { it.id in oldAssigneeIds })

                    // Petakan Label Lama
                    val oldLabelIds = currentTask.labels.map { it.id }
                    selectedLabels.addAll(availableLabels.filter { it.id in oldLabelIds })

                    // Petakan Lampiran Lama
                    existingAttachments.addAll(currentTask.attachments)
                }
            } else if (taskResult is ResultState.Error) {
                errorMessage = taskResult.message
            }
            isInitialLoading = false
        }
    }

    fun toggleAssignee(employee: EmployeeEntity) {
        if (selectedAssignees.contains(employee)) selectedAssignees.remove(employee)
        else selectedAssignees.add(employee)
    }

    fun toggleLabel(label: TaskLabelResponse) {
        if (selectedLabels.contains(label)) {
            if (selectedLabels.size > 1) selectedLabels.remove(label)
            else errorMessage = "Minimal harus memilih 1 Label Tugas!"
        } else {
            selectedLabels.add(label)
            errorMessage = null
        }
    }

    fun addNewAttachments(context: Context, uris: List<Uri>) {
        uris.forEach { uri ->
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val isImage = mimeType.startsWith("image/")
            var name = "Lampiran_${System.currentTimeMillis()}"
            var size = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: name
                    size = cursor.getLong(sizeIndex)
                }
            }
            if (newAttachments.none { it.uri == uri }) {
                newAttachments.add(TaskAttachment(uri, name, isImage, size))
            }
        }
    }

    fun removeExistingAttachment(attachment: TaskAttachmentEntity) { existingAttachments.remove(attachment) }
    fun removeNewAttachment(attachment: TaskAttachment) { newAttachments.remove(attachment) }

    fun submitUpdate(context: Context) {
        if (title.isBlank() || deadline == null || selectedAssignees.isEmpty() || selectedLabels.isEmpty()) {
            errorMessage = "Judul, Tenggat Waktu, Karyawan, dan Label wajib diisi!"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // 1. Upload Gambar Baru Dulu (Jika Ada)
                val newImageUrls = mutableListOf<String>()
                if (newAttachments.isNotEmpty()) {
                    val imageBytes = newAttachments.mapNotNull { context.contentResolver.openInputStream(it.uri)?.readBytes() }
                    val uploadResult = taskRepository.uploadImages(imageBytes)
                    if (uploadResult is ResultState.Success) {
                        newImageUrls.addAll(uploadResult.data)
                    } else {
                        errorMessage = "Gagal mengunggah lampiran baru."
                        isLoading = false
                        return@launch
                    }
                }

                // 2. Gabungkan URL Lama yang tersisa + URL Baru
                val finalImageUrls = existingAttachments.map { it.imageUrl } + newImageUrls
                val finalLabelIds = selectedLabels.map { it.id }
                val storeId = selectedAssignees.first().stores.firstOrNull()?.id ?: ""
                val userIds = selectedAssignees.map { it.id }
                val dueDateStr = deadline!!.format(DateTimeFormatter.ISO_LOCAL_DATE)

                if (storeId.isBlank()) {
                    errorMessage = "Data Toko tidak valid pada karyawan yang dipilih."
                    isLoading = false
                    return@launch
                }

                // 3. Eksekusi Update
                val request = UpdateTaskRequest(
                    p_task_id = taskId,
                    p_title = title,
                    p_description = description,
                    p_due_date = dueDateStr,
                    p_store_id = storeId,
                    p_assigned_to_ids = userIds,
                    p_label_ids = finalLabelIds,
                    p_image_urls = finalImageUrls
                )

                val result = taskRepository.updateTask(request)
                if (result is ResultState.Success && result.data) {
                    successMessage = "Tugas berhasil diperbarui!"
                } else {
                    errorMessage = "Gagal memperbarui tugas."
                }
            } catch (e: Exception) {
                errorMessage = "Terjadi kesalahan: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}