package id.biz.yuka.performax.ui.screen.violation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.AppealCommentEntity
import id.biz.yuka.libperformx.entity.AppealEntity
import id.biz.yuka.libperformx.entity.ViolationLogEntity
import id.biz.yuka.libperformx.repository.AppealRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.EmployeeRuleRepository
import id.biz.yuka.libperformx.request.CreateAppealCommentRequest
import id.biz.yuka.libperformx.request.UpdateAppealStatusRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class AppealViewModel @Inject constructor(
    private val appealRepository: AppealRepository,
    private val ruleRepository: EmployeeRuleRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    var currentUserId by mutableStateOf<String?>(null)
        private set
    var isOwner by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var isSending by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)

    // State Banding
    var ownerActiveAppeals by mutableStateOf<List<AppealEntity>>(emptyList())
        private set
    var selectedAppeal by mutableStateOf<AppealEntity?>(null)
        private set
    var appealedViolation by mutableStateOf<ViolationLogEntity?>(null)
        private set

    // State Chat
    var comments by mutableStateOf<List<AppealCommentEntity>>(emptyList())
        private set
    var commentText by mutableStateOf("")
    var selectedImages by mutableStateOf<List<Uri>>(emptyList())

    // Active Room State (untuk auto-reload / DisposableEffect)
    var activeRoomAppealId by mutableStateOf<String?>(null)
        private set

    init {
        initData()
    }

    fun initData() {
        viewModelScope.launch {
            isLoading = true
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            currentUserId = userId

            if (userId != null) {
                val empRes = employeeRepository.getEmployeeById(userId)
                if (empRes is ResultState.Success) {
                    val role = empRes.data.roleName.lowercase()
                    isOwner = role == "owner" || role == "admin"
                }
                loadActiveAppeals()
            }
            isLoading = false
        }
    }

    fun loadActiveAppeals() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            if (isOwner) {
                when (val result = appealRepository.getAllActiveAppeals()) {
                    is ResultState.Success -> {
                        ownerActiveAppeals = result.data.sortedByDescending { it.createdAt }
                    }
                    is ResultState.Error -> {
                        errorMessage = result.message
                    }
                }
            } else {
                val userId = currentUserId ?: return@launch
                when (val result = appealRepository.getActiveAppealByUser(userId)) {
                    is ResultState.Success -> {
                        result.data?.let { appeal ->
                            selectAppealAndLoadChats(appeal)
                        }
                    }
                    is ResultState.Error -> {
                        errorMessage = result.message
                    }
                }
            }
            isLoading = false
        }
    }

    fun selectAppealAndLoadChats(appeal: AppealEntity) {
        selectedAppeal = appeal
        loadComments(appeal.id)
        loadViolationDetail(appeal.violationLogId)
    }

    fun clearSelectedAppeal() {
        selectedAppeal = null
        appealedViolation = null
        comments = emptyList()
        clearActiveRoom()
    }

    fun setAsActiveRoom() {
        activeRoomAppealId = selectedAppeal?.id
    }

    fun clearActiveRoom() {
        activeRoomAppealId = null
    }

    private fun loadComments(appealId: String) {
        viewModelScope.launch {
            when (val result = appealRepository.getAppealComments(appealId)) {
                is ResultState.Success -> {
                    comments = result.data
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    private fun loadViolationDetail(logId: String) {
        viewModelScope.launch {
            when (val result = ruleRepository.getViolationLogById(logId)) {
                is ResultState.Success -> {
                    appealedViolation = result.data
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    fun addSelectedImage(context: Context, uri: Uri) {
        if (!selectedImages.contains(uri)) {
            selectedImages = selectedImages + uri
        }
    }

    fun handleCameraResult(context: Context, bitmap: Bitmap?) {
        if (bitmap == null) return
        val uri = saveBitmapToTempUri(context, bitmap)
        uri?.let { addSelectedImage(context, it) }
    }

    fun removeSelectedImage(uri: Uri) {
        selectedImages = selectedImages.filter { it != uri }
    }

    private fun saveBitmapToTempUri(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val file = java.io.File.createTempFile("temp_camera_", ".jpg", context.cacheDir)
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sendComment(context: Context) {
        val appeal = selectedAppeal ?: return
        val messageText = commentText.trim()
        val images = selectedImages.toList()

        if (messageText.isEmpty() && images.isEmpty()) return

        val senderTypeStr = if (isOwner) "owner" else "employee"
        val senderIdStr = currentUserId

        viewModelScope.launch {
            isSending = true
            errorMessage = null

            if (images.isNotEmpty()) {
                val bytesList = images.mapNotNull { uri ->
                    getBytesFromUri(context, uri)
                }

                when (val result = appealRepository.addCommentWithImages(
                    appealId = appeal.id,
                    senderId = senderIdStr,
                    senderType = senderTypeStr,
                    message = messageText,
                    imageBytesList = bytesList
                )) {
                    is ResultState.Success -> {
                        commentText = ""
                        selectedImages = emptyList()
                        loadComments(appeal.id)
                    }
                    is ResultState.Error -> {
                        errorMessage = result.message
                    }
                }
            } else {
                val req = CreateAppealCommentRequest(
                    appeal_id = appeal.id,
                    sender_type = senderTypeStr,
                    sender_id = senderIdStr,
                    message = messageText
                )
                when (val result = appealRepository.addComment(req)) {
                    is ResultState.Success -> {
                        commentText = ""
                        loadComments(appeal.id)
                    }
                    is ResultState.Error -> {
                        errorMessage = result.message
                    }
                }
            }
            isSending = false
        }
    }

    private fun getBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val buffer = ByteArrayOutputStream()
            val data = ByteArray(1024)
            var nRead: Int
            while (inputStream?.read(data, 0, data.size).also { nRead = it ?: -1 } != -1) {
                buffer.write(data, 0, nRead)
            }
            buffer.flush()
            buffer.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resolveAppeal(isApproved: Boolean) {
        val appeal = selectedAppeal ?: return
        val newStatus = if (isApproved) "APPROVED" else "REJECTED"

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val req = UpdateAppealStatusRequest(status = newStatus)
            when (val result = appealRepository.updateAppealState(appeal.id, req)) {
                is ResultState.Success -> {
                    clearSelectedAppeal()
                    loadActiveAppeals()
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }
}
