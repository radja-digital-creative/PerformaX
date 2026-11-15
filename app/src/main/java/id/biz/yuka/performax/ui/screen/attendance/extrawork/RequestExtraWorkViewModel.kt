package id.biz.yuka.performax.ui.screen.attendance.extrawork

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.ShiftEntity
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.CreateExtraWorkRequest
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.ui.components.MapMarker
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class RequestExtraWorkViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val scheduleRepository: ScheduleRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    var availableStores by mutableStateOf<List<StoreEntity>>(emptyList())
    var selectedStore by mutableStateOf<StoreEntity?>(null)

    var availableShifts by mutableStateOf<List<ShiftEntity>>(emptyList())
    var selectedShift by mutableStateOf<ShiftEntity?>(null)

    var reason by mutableStateOf("")
    var capturedImage by mutableStateOf<Bitmap?>(null)
    var userLocation by mutableStateOf<MapMarker?>(null)

    var isLoading by mutableStateOf(false)
    var isSubmitting by mutableStateOf(false)
    var isShiftsLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init {
        loadStores()
    }

    private fun loadStores() {
        viewModelScope.launch {
            isLoading = true
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                when (val myStoresRes = storeRepository.getMyStores(userId)) {
                    is ResultState.Success -> {
                        availableStores = myStoresRes.data
                        if (availableStores.isNotEmpty()) {
                            onStoreSelected(availableStores.first())
                        }
                    }
                    else -> {
                        when (val allStoresRes = storeRepository.getAllStores()) {
                            is ResultState.Success -> {
                                availableStores = allStoresRes.data
                                if (availableStores.isNotEmpty()) {
                                    onStoreSelected(availableStores.first())
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
            isLoading = false
        }
    }

    fun onStoreSelected(store: StoreEntity) {
        selectedStore = store
        selectedShift = null
        loadShiftsForStore(store.id)
    }

    fun onShiftSelected(shift: ShiftEntity) {
        selectedShift = shift
    }

    private fun loadShiftsForStore(storeId: String) {
        viewModelScope.launch {
            isShiftsLoading = true
            when (val res = scheduleRepository.getShifts(storeId)) {
                is ResultState.Success -> {
                    availableShifts = res.data
                    if (availableShifts.isNotEmpty()) {
                        selectedShift = availableShifts.first()
                    }
                }
                is ResultState.Error -> {
                    availableShifts = emptyList()
                }
            }
            isShiftsLoading = false
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        userLocation = MapMarker(lat, lng, "Posisi Anda")
    }

    fun submitRequest() {
        if (selectedStore == null || selectedShift == null || reason.isBlank() || capturedImage == null || userLocation == null) {
            errorMessage = "Mohon lengkapi semua data: Toko, Shift, Alasan, Foto Bukti, dan Lokasi GPS."
            return
        }

        viewModelScope.launch {
            isSubmitting = true
            errorMessage = null
            try {
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    errorMessage = "Sesi login tidak valid. Silakan login kembali."
                    return@launch
                }

                val stream = ByteArrayOutputStream()
                capturedImage!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val byteArray = stream.toByteArray()

                val uploadResult = attendanceRepository.uploadAttendanceImage(byteArray, userId)
                if (uploadResult !is ResultState.Success) {
                    errorMessage = "Gagal mengunggah foto bukti: ${(uploadResult as ResultState.Error).message}"
                    return@launch
                }

                val today = LocalDate.now().toString()
                val nowTime = LocalTime.now(ZoneId.of("Asia/Jakarta")).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val checkInIso = "${today}T${nowTime}Z"

                val request = CreateExtraWorkRequest(
                    user_id = userId,
                    store_id = selectedStore!!.id,
                    shift_id = selectedShift!!.id,
                    work_date = today,
                    check_in_time = checkInIso,
                    check_in_latitude = userLocation!!.lat,
                    check_in_longitude = userLocation!!.lng,
                    check_in_image_url = uploadResult.data,
                    reason = reason
                )

                when (val result = attendanceRepository.submitExtraWorkRequest(request)) {
                    is ResultState.Success -> {
                        successMessage = "Pengajuan masuk kerja berhasil dikirim! Menunggu persetujuan Manager."
                    }
                    is ResultState.Error -> {
                        errorMessage = "Gagal mengirim pengajuan: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Terjadi kesalahan: ${e.message}"
            } finally {
                isSubmitting = false
            }
        }
    }
}
