package id.biz.yuka.performax.ui.screen.violation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.AppealCommentEntity
import id.biz.yuka.libperformx.entity.AppealEntity
import id.biz.yuka.libperformx.entity.ViolationLogEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.repository.AppealRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRuleRepository
import id.biz.yuka.libperformx.request.CreateAppealCommentRequest
import id.biz.yuka.libperformx.request.UpdateAppealStatusRequest
import id.biz.yuka.libperformx.response.AppealResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import id.biz.yuka.performax.utils.ChatEventBus // IMPORT EVENT BUS
import id.biz.yuka.performax.utils.ChatRoomObserver // IMPORT KONTRAK OBSERVER
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppealViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appealRepository: AppealRepository,
    private val ruleRepository: EmployeeRuleRepository
) : ViewModel(), ChatRoomObserver { // <-- 1. IMPLEMENTASI INTERFACE OBSERVER

    var isLoading by mutableStateOf(false)
    var isSending by mutableStateOf(false)

    var isOwner by mutableStateOf(false)
    var currentUserId by mutableStateOf<String?>(null)

    // Data untuk Owner (List)
    var ownerActiveAppeals by mutableStateOf<List<AppealEntity>>(emptyList())

    // Data untuk Karyawan (Single Active)
    var myActiveAppeal by mutableStateOf<AppealEntity?>(null)

    // Data untuk Ruang Chat
    var selectedAppeal by mutableStateOf<AppealEntity?>(null)
    var appealedViolation by mutableStateOf<ViolationLogEntity?>(null)
    var comments by mutableStateOf<List<AppealCommentEntity>>(emptyList())
    var commentText by mutableStateOf("")

    // List Preview Gambar/Dokumen
    val selectedImages = mutableStateListOf<Uri>()

    init {
        // 2. JALANKAN OBSERVER SAAT VIEWMODEL DIBUAT
        observeIncomingFCMChats()

        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                currentUserId = userId
                val roleResult = authRepository.getUserRole(userId)
                if (roleResult is ResultState.Success) {
                    isOwner = roleResult.data == "owner"
                }
            }
            loadActiveAppeals()
        }
    }

    // ==========================================
    // IMPLEMENTASI KONTRAK CHAT ROOM OBSERVER
    // ==========================================

    override fun observeIncomingFCMChats() {
        viewModelScope.launch {
            ChatEventBus.incomingEvents.collect { event ->
                // Filter hanya bereaksi jika tipenya "appeal" dan ID-nya cocok dengan yang sedang dibuka
                val currentAppealId = selectedAppeal?.id
                if (event.type == "appeal" && currentAppealId != null && event.id == currentAppealId) {
                    loadComments(currentAppealId) // Refresh chat saja, tanpa perlu loading seluruh halaman
                }
            }
        }
    }

    override fun setAsActiveRoom() {
        selectedAppeal?.id?.let { appealId ->
            ChatEventBus.setActiveRoom("appeal", appealId)
        }
    }

    override fun clearActiveRoom() {
        ChatEventBus.clearActiveRoom("appeal")
    }

    // ==========================================

    fun loadActiveAppeals() {
        viewModelScope.launch {
            isLoading = true
            Log.d("AppealViewModel", "Memulai loadActiveAppeals untuk User: $currentUserId")

            if (isOwner) {
                // Menggunakan fungsi rapi dari Repository
                val result = appealRepository.getAllActiveAppeals()
                if (result is ResultState.Success) {
                    ownerActiveAppeals = result.data
                    Log.d("AppealViewModel", "Owner: Berhasil load ${result.data.size} banding.")
                } else if (result is ResultState.Error) {
                    Log.e("AppealViewModel", "Gagal memuat banding manajemen: ${result.message}")
                }
            } else {
                currentUserId?.let { uid ->
                    // Menggunakan fungsi rapi dari Repository
                    val result = appealRepository.getActiveAppealByUser(uid)
                    if (result is ResultState.Success) {
                        val activeAppeal = result.data

                        if (activeAppeal != null) {
                            Log.d("AppealViewModel", "Karyawan: Menemukan banding aktif! ID: ${activeAppeal.id}")
                            myActiveAppeal = activeAppeal
                            // FORCE SELECT
                            selectAppealAndLoadChats(activeAppeal)
                        } else {
                            Log.d("AppealViewModel", "Karyawan: Tidak ada banding aktif ditemukan.")
                        }
                    } else if (result is ResultState.Error) {
                        Log.e("AppealViewModel", "Gagal mengambil data banding: ${result.message}")
                    }
                }
            }
            isLoading = false
        }
    }

    fun selectAppealAndLoadChats(appeal: AppealEntity) {
        selectedAppeal = appeal
        loadComments(appeal.id)
        loadAppealedViolation(appeal.violationLogId)

        // Daftarkan room secara otomatis begitu appeal dipilih
        setAsActiveRoom()
    }

    private fun loadAppealedViolation(logId: String) {
        viewModelScope.launch {
            val result = ruleRepository.getViolationLogById(logId)
            if (result is ResultState.Success) {
                appealedViolation = result.data
            }
        }
    }

    fun clearSelectedAppeal() {
        // Hapus status aktif room terlebih dahulu sebelum datanya di null-kan
        clearActiveRoom()

        selectedAppeal = null
        appealedViolation = null
        comments = emptyList()
        selectedImages.clear()
    }

    fun loadComments(appealId: String) {
        viewModelScope.launch {
            val response = appealRepository.getAppealComments(appealId)
            if (response is ResultState.Success) {
                comments = response.data
            }
        }
    }

    // ==========================================
    // LOGIKA GAMBAR / ATTACHMENT
    // ==========================================

    fun addSelectedImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = File(context.cacheDir, "appeal_${UUID.randomUUID()}.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()

                    val safeLocalUri = Uri.fromFile(file)
                    withContext(Dispatchers.Main) {
                        selectedImages.add(safeLocalUri)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppealViewModel", "Gagal menyalin lampiran: ${e.message}")
            }
        }
    }

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
        val appealId = selectedAppeal?.id ?: return
        if (commentText.isBlank() && selectedImages.isEmpty()) return

        viewModelScope.launch {
            isSending = true
            val senderType = if (isOwner) "owner" else "employee"

            // JIKA TANPA GAMBAR (HANYA TEKS)
            if (selectedImages.isEmpty()) {
                val request = CreateAppealCommentRequest(
                    appeal_id = appealId,
                    sender_type = senderType,
                    sender_id = currentUserId,
                    message = commentText
                )
                val result = appealRepository.addComment(request)
                if (result is ResultState.Success) {
                    commentText = ""
                    loadComments(appealId)
                }
            }
            // JIKA DENGAN GAMBAR
            else {
                // Konversi URI menjadi ByteArray untuk diupload
                val imageBytes = selectedImages.mapNotNull { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        Log.e("AppealViewModel", "Gagal membaca byte gambar: ${e.message}")
                        null
                    }
                }

                if (imageBytes.isNotEmpty()) {
                    val messageToSend = commentText

                    // 1 Hit Eksekusi (Komentar -> Upload Storage -> Media)
                    val result = appealRepository.addCommentWithImages(
                        appealId = appealId,
                        senderId = currentUserId,
                        senderType = senderType,
                        message = messageToSend,
                        imageBytesList = imageBytes
                    )

                    if (result is ResultState.Success) {
                        commentText = ""
                        selectedImages.clear()
                        loadComments(appealId)
                    }
                } else {
                    Log.e("AppealViewModel", "Gagal memproses gambar. Membatalkan pengiriman.")
                }
            }

            isSending = false
        }
    }

    // ==========================================
    // AKSI OWNER (TERIMA / TOLAK)
    // ==========================================

    fun resolveAppeal(isApproved: Boolean) {
        val appeal = selectedAppeal ?: return
        viewModelScope.launch {
            isLoading = true
            val newStatus = if (isApproved) "APPROVED" else "REJECTED"

            appealRepository.updateAppealState(
                appealId = appeal.id,
                request = UpdateAppealStatusRequest(status = newStatus)
            )

            clearSelectedAppeal()
            loadActiveAppeals()
            isLoading = false
        }
    }
}