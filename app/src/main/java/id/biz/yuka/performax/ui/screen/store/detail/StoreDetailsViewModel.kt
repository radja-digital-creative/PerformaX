package id.biz.yuka.performax.ui.screen.store.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.UpdateAttendanceSettingRequest
import id.biz.yuka.libperformx.request.UpdateStoreRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreDetailsViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val storeId: String = checkNotNull(savedStateHandle["storeId"])
    var userRole by mutableStateOf("employee")
    val isEditable: Boolean
        get() = userRole == "owner" || userRole == "admin"
    // Form State: Data Utama Toko
    var storeName by mutableStateOf("")
    var storeAddress by mutableStateOf("")
    var storePhone by mutableStateOf("")
    var latitude by mutableStateOf("")
    var longitude by mutableStateOf("")
    var isActive by mutableStateOf(true)

    // Form State: Pengaturan Absensi
    var openBeforeMinutes by mutableStateOf("60")
    var lateToleranceMinutes by mutableStateOf("15")
    var overtimeThresholdMinutes by mutableStateOf("60")
    var dailyCloseTime by mutableStateOf("22:00:00")

    // UI State
    var isLoading by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadStoreDetail()
        loadUserRole()
    }

    fun loadStoreDetail() {
        viewModelScope.launch {
            isLoading = true
            when (val result = storeRepository.getStoreDetail(storeId)) {
                is ResultState.Success -> {
                    val detail = result.data

                    // ✅ SEKARANG SUDAH FLATTENED (Sesuai dengan StoreEntity.kt Anda)
                    storeName = detail.name
                    storeAddress = detail.address
                    storePhone = detail.phone
                    latitude = if (detail.latitude == 0.0) "" else detail.latitude.toString()
                    longitude = if (detail.longitude == 0.0) "" else detail.longitude.toString()
                    isActive = detail.isActive

                    openBeforeMinutes = detail.openBeforeMinutes.toString()
                    lateToleranceMinutes = detail.lateToleranceMinutes.toString()
                    overtimeThresholdMinutes = detail.overtimeThresholdMinutes.toString()
                    dailyCloseTime = detail.closeTime
                }
                is ResultState.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
            when (val roleRes = authRepository.getUserRole(currentUserId)) {
                is ResultState.Success -> userRole = roleRes.data
                is ResultState.Error -> userRole = "employee"
            }
        }
    }

    fun updateStoreInfo() {
        viewModelScope.launch {
            isSaving = true
            val request = UpdateStoreRequest(
                name = storeName,
                address = storeAddress.ifBlank { null },
                phone_number = storePhone.ifBlank { null },
                logo_url = null,
                latitude = latitude.toDoubleOrNull(),
                longitude = longitude.toDoubleOrNull(),
                is_active = isActive
            )

            when (val result = storeRepository.updateStore(storeId, request)) {
                is ResultState.Success -> successMessage = "Info utama toko berhasil diperbarui!"
                is ResultState.Error -> errorMessage = result.message
            }
            isSaving = false
        }
    }

    fun updateAttendanceSettings() {
        viewModelScope.launch {
            isSaving = true
            val request = UpdateAttendanceSettingRequest(
                open_before_minutes = openBeforeMinutes.toIntOrNull(),
                late_tolerance_minutes = lateToleranceMinutes.toIntOrNull(),
                overtime_threshold_minutes = overtimeThresholdMinutes.toIntOrNull(),
                daily_close_time = dailyCloseTime.ifBlank { null }
            )

            when (val result = storeRepository.updateAttendanceSetting(storeId, request)) {
                is ResultState.Success -> successMessage = "Pengaturan batas waktu absensi berhasil disimpan!"
                is ResultState.Error -> errorMessage = result.message
            }
            isSaving = false
        }
    }
}