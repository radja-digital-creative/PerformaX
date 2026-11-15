package id.biz.yuka.performax.ui.screen.announcement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.AnnouncementEntity
import id.biz.yuka.libperformx.repository.AnnouncementRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementListViewModel @Inject constructor(
    private val announcementRepository: AnnouncementRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var announcements by mutableStateOf<List<AnnouncementEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isOwnerOrAdmin by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            // 1. Cek Role
            val roleRes = authRepository.getUserRole(userId)
            val role = if (roleRes is ResultState.Success) roleRes.data.lowercase() else "employee"
            
            isOwnerOrAdmin = role == "owner" || role == "admin" || role == "manager"
            val isGlobalAdmin = role == "owner" || role == "admin"

            // 2. Ambil Pengumuman
            val result = if (isGlobalAdmin) {
                // Owner/Admin: Get All (tanpa filter store)
                announcementRepository.getAllAnnouncements(null)
            } else {
                // Manager/Employee: Filter berdasarkan store mereka
                val storesRes = storeRepository.getMyStores(userId)
                if (storesRes is ResultState.Success) {
                    val storeIds = storesRes.data.map { it.id }
                    announcementRepository.getAllAnnouncements(storeIds)
                } else {
                    ResultState.Error(storesRes.toString())
                }
            }

            // 3. Proses Hasil
            if (result is ResultState.Success) {
                announcements = result.data
            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }
            
            isLoading = false
        }
    }
}
