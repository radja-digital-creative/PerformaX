package id.biz.yuka.performax.ui.screen.announcement.create

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.AnnouncementRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CreateAnnouncementViewModel @Inject constructor(
    private val announcementRepository: AnnouncementRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var title by mutableStateOf("")
    var type by mutableStateOf("text") // text, image
    var content by mutableStateOf("")
    var actionUrl by mutableStateOf("")
    var startDate by mutableStateOf(LocalDate.now())
    var endDate by mutableStateOf(LocalDate.now().plusDays(7))

    var selectedImageUri by mutableStateOf<Uri?>(null)
    
    var availableStores = mutableStateListOf<StoreEntity>()
    val selectedStoreIds = mutableStateListOf<String>()

    var isLoading by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadAvailableStores()
    }

    private fun loadAvailableStores() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val roleRes = authRepository.getUserRole(userId)
            
            val storesRes = if (roleRes is ResultState.Success && (roleRes.data == "owner" || roleRes.data == "admin")) {
                storeRepository.getAllStores()
            } else {
                storeRepository.getMyStores(userId)
            }

            if (storesRes is ResultState.Success) {
                availableStores.clear()
                availableStores.addAll(storesRes.data)
                // Default select all
                selectedStoreIds.addAll(storesRes.data.map { it.id })
            }
        }
    }

    fun toggleStoreSelection(storeId: String) {
        if (selectedStoreIds.contains(storeId)) {
            selectedStoreIds.remove(storeId)
        } else {
            selectedStoreIds.add(storeId)
        }
    }

    fun submit(context: Context) {
        if (title.isBlank()) { errorMessage = "Judul wajib diisi"; return }
        if (selectedStoreIds.isEmpty()) { errorMessage = "Pilih minimal 1 toko"; return }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val userId = authRepository.getCurrentUserId()
            val imageBytes = selectedImageUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }

            val result = announcementRepository.createAnnouncement(
                title = title,
                type = if (selectedImageUri != null) "image" else "text",
                content = content.ifBlank { null },
                imageByteArray = imageBytes,
                actionUrl = actionUrl.ifBlank { null },
                startAt = startDate.toString() + "T00:00:00Z",
                endAt = endDate.toString() + "T23:59:59Z",
                createdBy = userId,
                targetStoreIds = selectedStoreIds.toList()
            )

            if (result is ResultState.Success) {
                successMessage = "Pengumuman berhasil diterbitkan!"
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            
            isLoading = false
        }
    }
}
