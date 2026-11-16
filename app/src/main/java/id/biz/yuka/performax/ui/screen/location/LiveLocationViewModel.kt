package id.biz.yuka.performax.ui.screen.location

import android.os.SystemClock
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
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

    private var lastAutomaticPingAt: Long? = null
    private val locationMutex = kotlinx.coroutines.sync.Mutex()
    var refreshedAt by mutableStateOf(Instant.now())
        private set

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

    suspend fun refreshWhileVisible() = kotlinx.coroutines.coroutineScope {
        launch {
            while (isActive) {
                // Keep lifecycle resumes from repeatedly waking all employee devices.
                val elapsed = SystemClock.elapsedRealtime()
                val lastPing = lastAutomaticPingAt
                val remaining = if (lastPing == null) 0L else 300_000L - (elapsed - lastPing)
                if (remaining > 0L) {
                    delay(remaining)
                    continue
                }
                lastAutomaticPingAt = elapsed
                locationRefreshHelper.requestAllLocations()
                // Continuous tracking supplies updates; ping is only a fallback.
                delay(300_000L)
            }
        }
        while (isActive) {
            // Age cached observations even when the next fetch fails.
            refreshedAt = Instant.now()
            fetchLocations(false)
            delay(10_000L)
        }
    }

    private suspend fun fetchLocations(showLoading: Boolean) {
        if (!locationMutex.tryLock()) return
        try {
            if (showLoading) isLoading = true
            when (val res = locationRepository.getLatestUserLocations()) {
                is ResultState.Success -> { locationList = res.data; refreshedAt = Instant.now() }
                is ResultState.Error -> if (showLoading) toastMessage = res.message
            }
        } finally { isLoading = false; locationMutex.unlock() }
    }

    fun loadLocations() {
        viewModelScope.launch { fetchLocations(true) }
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

    fun locationStatus(recordedAt: String?): LocationFreshness =
        locationFreshness(recordedAt, refreshedAt)
}
