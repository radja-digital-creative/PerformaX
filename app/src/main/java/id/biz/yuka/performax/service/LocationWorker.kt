package id.biz.yuka.performax.service

import android.annotation.SuppressLint
import id.biz.yuka.performax.utils.fetchFreshLocation
import kotlinx.coroutines.CancellationException
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.biz.yuka.libperformx.repository.LocationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

@HiltWorker
class LocationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val locationRepository: LocationRepository
) : CoroutineWorker(context, workerParams) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        Log.d("LOCATION_WORKER", "==> doWork() DIPANGGIL OLEH WORKMANAGER (Periodik 30 Menit) <==")

        // Guard: jika LocationForegroundService sedang aktif (on-demand FCM), skip worker ini
        if (LocationForegroundService.isRunning) {
            Log.d("LOCATION_WORKER", "LocationForegroundService sedang aktif, skip periodik worker.")
            return Result.success()
        }

        return try {
            // Cek status GPS menggunakan LocationManager standar
            val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
                               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled) {
                Log.d("LOCATION_WORKER", "GPS terdeteksi OFF.")
            }

            Log.d("LOCATION_WORKER", "Mengambil lokasi latar belakang...")
            val location = withTimeoutOrNull(28_000L) {
                fusedLocationClient.fetchFreshLocation()
            }

            if (location != null) {
                Log.d("LOCATION_WORKER", "Lokasi berhasil diambil: lat=${location.latitude}, lng=${location.longitude}")
                val repoResult = locationRepository.recordUserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    provider = "passive_workmanager"
                )
                if (repoResult is id.biz.yuka.libperformx.utils.ResultState.Error) return Result.retry()
            } else {
                return Result.retry()
            }

            Result.success()
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Log.e("LOCATION_WORKER", "Error di LocationWorker: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "location_periodic_worker_30min"

        fun schedulePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d("LOCATION_WORKER", "Penjadwalan periodik 30 menit berhasil diset ke WorkManager (KEEP).")
        }
    }
}
