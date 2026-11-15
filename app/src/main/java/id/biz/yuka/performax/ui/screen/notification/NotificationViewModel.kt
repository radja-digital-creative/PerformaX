package id.biz.yuka.performax.ui.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.NotificationEntity
import id.biz.yuka.libperformx.repository.NotificationRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Ambil ID User yang sedang login
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id

            if (userId != null) {
                when (val result = repository.getNotifications(userId)) {
                    is ResultState.Success -> {
                        _notifications.value = result.data
                    }
                    is ResultState.Error -> {
                        _errorMessage.value = result.message
                    }
                }
            } else {
                _errorMessage.value = "Sesi pengguna tidak ditemukan."
            }
            _isLoading.value = false
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            // Optimistic UI Update: Ubah state lokal dulu agar UI langsung merespon
            _notifications.value = _notifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }

            // Eksekusi ke server di latar belakang
            repository.markAsRead(notificationId)
        }
    }
}