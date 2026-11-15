package id.biz.yuka.performax.ui.screen.announcement.detail

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.AnnouncementEntity
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.AnnouncementRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.UpdateAnnouncementRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class AnnouncementDetailViewModel @Inject constructor(
    private val announcementRepository: AnnouncementRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val announcementId: String = checkNotNull(savedStateHandle["announcementId"])

    var announcement by mutableStateOf<AnnouncementEntity?>(null)
    var isLoading by mutableStateOf(false)
    var isEditMode by mutableStateOf(false)
    var isOwnerOrAdmin by mutableStateOf(false)

    // Edit Form States
    var editTitle by mutableStateOf("")
    var editContent by mutableStateOf("")
    var editStartDate by mutableStateOf(LocalDate.now())
    var editEndDate by mutableStateOf(LocalDate.now())
    var editIsActive by mutableStateOf(true)
    var newImageUri by mutableStateOf<Uri?>(null)
    
    var availableStores = mutableStateListOf<StoreEntity>()
    val selectedStoreIds = mutableStateListOf<String>()

    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadDetail()
        loadAvailableStores()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            isLoading = true
            val result = announcementRepository.getAllAnnouncements()
            if (result is ResultState.Success) {
                val found = result.data.find { it.id == announcementId }
                if (found != null) {
                    announcement = found
                    populateForm(found)
                    checkPermission(found)
                } else {
                    errorMessage = "Pengumuman tidak ditemukan"
                }
            }
            isLoading = false
        }
    }

    private fun checkPermission(data: AnnouncementEntity) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val roleRes = authRepository.getUserRole(userId)
            if (roleRes is ResultState.Success) {
                val role = roleRes.data.lowercase()
                // Owner/Admin bisa edit apa saja, Manager hanya jika dia yang buat atau di tokonya
                isOwnerOrAdmin = role == "owner" || role == "admin" || role == "manager"
            }
        }
    }

    private fun loadAvailableStores() {
        viewModelScope.launch {
            val result = storeRepository.getAllStores()
            if (result is ResultState.Success) {
                availableStores.clear()
                availableStores.addAll(result.data)
            }
        }
    }

    private fun populateForm(data: AnnouncementEntity) {
        editTitle = data.title
        editContent = data.content ?: ""
        editIsActive = data.isActive
        
        try {
            editStartDate = OffsetDateTime.parse(data.startAt).toLocalDate()
            editEndDate = OffsetDateTime.parse(data.endAt).toLocalDate()
        } catch (e: Exception) { }

        selectedStoreIds.clear()
        selectedStoreIds.addAll(data.storeIds)
    }

    fun toggleStoreSelection(storeId: String) {
        if (selectedStoreIds.contains(storeId)) {
            selectedStoreIds.remove(storeId)
        } else {
            selectedStoreIds.add(storeId)
        }
    }

    fun updateAnnouncement(context: Context) {
        viewModelScope.launch {
            isLoading = true
            val newImageBytes = newImageUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }

            val request = UpdateAnnouncementRequest(
                title = editTitle,
                content = editContent,
                is_active = editIsActive,
                start_at = editStartDate.toString() + "T00:00:00Z",
                end_at = editEndDate.toString() + "T23:59:59Z"
            )

            val result = announcementRepository.updateAnnouncement(
                announcementId = announcementId,
                request = request,
                newImageByteArray = newImageBytes,
                updatedStoreIds = selectedStoreIds.toList()
            )

            if (result is ResultState.Success) {
                successMessage = "Pengumuman berhasil diperbarui!"
                isEditMode = false
                loadDetail()
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun deleteAnnouncement(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = announcementRepository.deleteAnnouncement(announcementId)
            if (result is ResultState.Success) {
                onSuccess()
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }
}
