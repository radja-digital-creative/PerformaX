package id.biz.yuka.performax.ui.screen.location

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.UserLocationDetailEntity
import id.biz.yuka.libperformx.repository.LocationRepository
import id.biz.yuka.libperformx.utils.LocationRefreshHelper
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.performax.ui.components.StoreMapMarker
import javax.inject.Inject

@HiltViewModel
class LiveLocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val locationRefreshHelper: LocationRefreshHelper,
    private val storeRepository: StoreRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var locationList by mutableStateOf<List<UserLocationDetailEntity>>(emptyList())
    var storeList by mutableStateOf<List<StoreMapMarker>>(emptyList())
    var isShowStores by mutableStateOf(false)
    var selectedUserId by mutableStateOf<String?>(null)
    var toastMessage by mutableStateOf<String?>(null)
    var refreshingUserId by mutableStateOf<String?>(null)

    init {
        loadLocations()
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            val storesRes = storeRepository.getAllStores()
            val shiftsRes = scheduleRepository.getShifts(null)

            if (storesRes is ResultState.Success) {
                val activeStores = storesRes.data.filter { it.isActive && it.latitude != 0.0 && it.longitude != 0.0 }
                val allShifts = (shiftsRes as? ResultState.Success)?.data ?: emptyList()

                val maxRadiusByStore = allShifts
                    .groupBy { it.storeId }
                    .mapValues { entry ->
                        entry.value.maxOfOrNull { it.attendanceRadius }?.toDouble() ?: 50.0
                    }

                storeList = activeStores.map { store ->
                    val radius = maxRadiusByStore[store.id] ?: 50.0
                    StoreMapMarker(
                        storeId = store.id,
                        name = store.name,
                        lat = store.latitude,
                        lng = store.longitude,
                        radiusInMeters = radius
                    )
                }
            }
        }
    }

    fun loadLocations() {
        viewModelScope.launch {
            isLoading = true
            when (val res = locationRepository.getLatestUserLocations()) {
                is ResultState.Success -> {
                    locationList = res.data
                    res.data.forEach { 
                        android.util.Log.d("LiveLocationVM", "User: ${it.fullName}, Lat: ${it.latitude}, Lng: ${it.longitude}")
                    }
                }
                is ResultState.Error -> {
                    toastMessage = res.message
                }
            }
            isLoading = false
        }
    }

    fun refreshAllLocations() {
        viewModelScope.launch {
            isRefreshing = true
            when (val res = locationRefreshHelper.requestAllLocations()) {
                is ResultState.Success -> {
                    toastMessage = "✅ Permintaan lokasi terkirim ke ${res.data} karyawan"
                    // Beri jeda 3 detik agar device karyawan sempat update koordinat ke Supabase
                    delay(3000)
                    loadLocations()
                }
                is ResultState.Error -> {
                    toastMessage = "❌ ${res.message}"
                }
            }
            isRefreshing = false
        }
    }

    fun refreshSingleLocation(userId: String) {
        viewModelScope.launch {
            refreshingUserId = userId
            when (val res = locationRefreshHelper.requestSingleLocation(userId)) {
                is ResultState.Success -> {
                    toastMessage = "✅ Permintaan lokasi terkirim ke karyawan"
                    delay(3000)
                    loadLocations()
                }
                is ResultState.Error -> {
                    toastMessage = "❌ ${res.message}"
                }
            }
            refreshingUserId = null
        }
    }

    fun selectUser(userId: String) {
        selectedUserId = userId
    }

    fun clearToast() {
        toastMessage = null
    }

    fun formatRelativeTime(recordedAtStr: String): String {
        if (recordedAtStr.isBlank()) return "Waktu tidak diketahui"
        return try {
            val odt = OffsetDateTime.parse(recordedAtStr)
            val recordedInstant = odt.toInstant()
            val nowInstant = Instant.now()

            val duration = Duration.between(recordedInstant, nowInstant)
            val minutes = duration.toMinutes()
            val hours = duration.toHours()

            if (minutes < 1) {
                "Baru saja"
            } else if (minutes < 60) {
                "$minutes menit lalu"
            } else if (hours < 24) {
                "$hours jam lalu"
            } else {
                val zdt = odt.atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
                zdt.format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale("id", "ID"))) + " WIB"
            }
        } catch (e: Exception) {
            recordedAtStr
        }
    }
}
