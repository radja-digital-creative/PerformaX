package id.biz.yuka.performax.ui.screen.store.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.CreateStoreRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreCreateViewModel @Inject constructor(
    private val storeRepository: StoreRepository
) : ViewModel() {

    // Form State Utama
    var storeName by mutableStateOf("")
    var storeAddress by mutableStateOf("")
    var storePhone by mutableStateOf("")

    // Form State Koordinat & Waktu
    var latitude by mutableStateOf("")
    var longitude by mutableStateOf("")
    var openBeforeMinutes by mutableStateOf("60")
    var lateToleranceMinutes by mutableStateOf("15")
    var overtimeThresholdMinutes by mutableStateOf("60")
    var dailyCloseTime by mutableStateOf("22:00:00")

    // UI State
    var isLoading by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    fun createNewStore() {
        if (storeName.isBlank()) {
            errorMessage = "Nama toko tidak boleh kosong!"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // ✅ Menyesuaikan dengan parameter CreateStoreRequest terbaru
                val request = CreateStoreRequest(
                    p_name = storeName,
                    p_address = storeAddress.ifBlank { null },
                    p_phone_number = storePhone.ifBlank { null },
                    p_logo_url = null, // Opsional untuk pengembangan masa depan
                    p_latitude = latitude.toDoubleOrNull(),
                    p_longitude = longitude.toDoubleOrNull(),
                    p_open_before_minutes = openBeforeMinutes.toIntOrNull() ?: 60,
                    p_late_tolerance_minutes = lateToleranceMinutes.toIntOrNull() ?: 15,
                    p_overtime_threshold_minutes = overtimeThresholdMinutes.toIntOrNull() ?: 60,
                    p_daily_close_time = dailyCloseTime.ifBlank { "22:00:00" }
                )

                when (val result = storeRepository.createStore(request)) {
                    is ResultState.Success -> {
                        successMessage = "Toko berhasil didaftarkan!"
                    }
                    is ResultState.Error -> errorMessage = result.message
                }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
}