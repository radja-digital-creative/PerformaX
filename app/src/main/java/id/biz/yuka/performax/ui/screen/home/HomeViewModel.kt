package id.biz.yuka.performax.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.UserProfileEntity
import id.biz.yuka.libperformx.entity.AnnouncementEntity
import id.biz.yuka.libperformx.repository.*
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.data.SessionManager
import id.biz.yuka.performax.service.FcmManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val announcementRepository: AnnouncementRepository,
    private val storeRepository: StoreRepository,
    private val sessionManager: SessionManager,
    private val fcmManager: FcmManager
) : ViewModel() {

    var userProfile by mutableStateOf<UserProfileEntity?>(null)
    var isLoading by mutableStateOf(false)

    // Announcement Popup States
    var activeAnnouncements by mutableStateOf<List<AnnouncementEntity>>(emptyList())
    var showAnnouncementPopup by mutableStateOf(false)

    data class WarningState(val hasWarning: Boolean, val message: String)
    var currentWarning by mutableStateOf<WarningState?>(null)

    private val _logoutSuccess = MutableSharedFlow<Unit>()
    val logoutSuccess = _logoutSuccess.asSharedFlow()

    init { loadGlobalData() }

    fun loadGlobalData() {
        viewModelScope.launch {
            isLoading = true
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val profileRes = userRepository.getUserProfile(userId)
                if (profileRes is ResultState.Success) {
                    userProfile = profileRes.data
                    fcmManager.syncTokenToSupabase()
                    checkPriorityWarnings(userId)
                    fetchActiveAnnouncements(userId, profileRes.data.role)
                }
            }
            isLoading = false
        }
    }

    private suspend fun fetchActiveAnnouncements(userId: String, role: String?) {
        val isGlobalAdmin = role?.lowercase() == "owner" || role?.lowercase() == "admin"
        
        val result = if (isGlobalAdmin) {
            announcementRepository.getActiveAnnouncements(null)
        } else {
            val storesRes = storeRepository.getMyStores(userId)
            if (storesRes is ResultState.Success) {
                val storeIds = storesRes.data.map { it.id }
                announcementRepository.getActiveAnnouncements(storeIds)
            } else {
                ResultState.Error("Gagal mengambil data toko")
            }
        }

        if (result is ResultState.Success && result.data.isNotEmpty()) {
            activeAnnouncements = result.data
            showAnnouncementPopup = true
        }
    }

    private suspend fun checkPriorityWarnings(userId: String) {
        val profile = userProfile ?: return

        // Warning 1: Face Data
        if (profile.faceEmbedding.isBlank()) {
            currentWarning = WarningState(true, "Data profil wajah Anda belum lengkap! Segera lengkapi di menu Edit Profil.")
            return
        }

        // Warning 2: Jadwal Bulan Depan
        val nextMonth = LocalDate.now().plusMonths(1)
        val startOfNextMonth = nextMonth.withDayOfMonth(1).toString()
        val endOfNextMonth = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth()).toString()

        val nextScheduleRes = attendanceRepository.getMonthlyScheduleCount(userId, startOfNextMonth, endOfNextMonth)
        if (nextScheduleRes is ResultState.Success && nextScheduleRes.data == 0) {
            val nextMonthName = nextMonth.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("id", "ID"))
            currentWarning = WarningState(true, "Jadwal bulan $nextMonthName belum diterbitkan admin. Hubungi atasan Anda.")
            return
        }

        currentWarning = null
    }

    fun logout() {
        viewModelScope.launch {
            SupabaseConfig.client.auth.signOut()
            sessionManager.clearSession()
            fcmManager.clearTokenOnLogout()
            _logoutSuccess.emit(Unit)
        }
    }
}