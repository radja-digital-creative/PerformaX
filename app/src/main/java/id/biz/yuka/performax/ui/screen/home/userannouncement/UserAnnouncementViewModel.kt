package id.biz.yuka.performax.ui.screen.home.userannouncement

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
class UserAnnouncementViewModel @Inject constructor(
    private val announcementRepository: AnnouncementRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var announcements by mutableStateOf<List<AnnouncementEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        fetchAnnouncements()
    }

    fun fetchAnnouncements() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val userId = authRepository.getCurrentUserId() ?: return@launch
            val roleRes = authRepository.getUserRole(userId)
            
            val isOwnerOrAdmin = if (roleRes is ResultState.Success) {
                val role = roleRes.data.lowercase()
                role == "owner" || role == "admin"
            } else false

            val result = if (isOwnerOrAdmin) {
                // Owner/Admin: Fetch all
                announcementRepository.getAllAnnouncements(null)
            } else {
                // Employee/Manager: Fetch by their stores
                val storesRes = storeRepository.getMyStores(userId)
                if (storesRes is ResultState.Success) {
                    val storeIds = storesRes.data.map { it.id }
                    announcementRepository.getAllAnnouncements(storeIds)
                } else {
                    ResultState.Error(storesRes.toString())
                }
            }

            when (result) {
                is ResultState.Success -> {
                    // Only show active ones for users? Or all? 
                    // Usually users only see active ones.
                    // For now, let's filter active ones locally or use repo's getActiveAnnouncements
                    announcements = result.data.filter { it.isActive }
                }
                is ResultState.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }
}
