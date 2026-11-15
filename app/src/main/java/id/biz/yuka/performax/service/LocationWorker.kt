package id.biz.yuka.performax.service

import android.annotation.SuppressLint
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
            val location = withTimeoutOrNull(15_000L) {
                fusedLocationClient.fetchFastLocation(applicationContext)
            }

            if (location != null) {
                Log.d("LOCATION_WORKER", "Lokasi berhasil diambil: lat=${location.latitude}, lng=${location.longitude}")
                val repoResult = locationRepository.recordUserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    provider = "passive_workmanager"
                )
                Log.d("LOCATION_WORKER", "Hasil simpan lokasi ke repository: $repoResult")
            } else {
                Log.w("LOCATION_WORKER", "Lokasi GPS bernilai null (timeout atau semua provider gagal).")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("LOCATION_WORKER", "Error di LocationWorker: ${e.message}")
            Result.retry()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun FusedLocationProviderClient.fetchFastLocation(context: Context): Location? {
        return try {
            // Step 1: Check lastLocation cache (~5ms)
            val lastLoc = try { lastLocation.await() } catch (_: Exception) { null }
            if (lastLoc != null) {
                Log.d("LOCATION_WORKER", "Lokasi cepat didapat dari lastLocation: lat=${lastLoc.latitude}, lng=${lastLoc.longitude}")
                return lastLoc
            }

            // Step 2: getCurrentLocation() — API one-shot yang paling reliable
            Log.d("LOCATION_WORKER", "lastLocation null, mencoba getCurrentLocation(HIGH_ACCURACY)...")
            val cts = CancellationTokenSource()
            val currentLoc = try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(10_000L)
                    .setMaxUpdateAgeMillis(5_000L)
                    .build()
                getCurrentLocation(request, cts.token).await()
            } catch (e: Exception) {
                Log.w("LOCATION_WORKER", "getCurrentLocation HIGH_ACCURACY gagal: ${e.message}")
                null
            }
            if (currentLoc != null) {
                Log.d("LOCATION_WORKER", "Lokasi didapat dari getCurrentLocation: lat=${currentLoc.latitude}, lng=${currentLoc.longitude}")
                return currentLoc
            }

            // Step 3: getCurrentLocation() dengan BALANCED_POWER (bisa lebih cepat via cell/wifi)
            Log.d("LOCATION_WORKER", "HIGH_ACCURACY gagal, mencoba BALANCED_POWER_ACCURACY...")
            val cts2 = CancellationTokenSource()
            val balancedLoc = try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setDurationMillis(5_000L)
                    .setMaxUpdateAgeMillis(30_000L)
                    .build()
                getCurrentLocation(request, cts2.token).await()
            } catch (e: Exception) {
                Log.w("LOCATION_WORKER", "getCurrentLocation BALANCED gagal: ${e.message}")
                null
            }
            if (balancedLoc != null) {
                Log.d("LOCATION_WORKER", "Lokasi didapat dari getCurrentLocation(BALANCED): lat=${balancedLoc.latitude}, lng=${balancedLoc.longitude}")
                return balancedLoc
            }

            // Step 4: LocationManager fallback (NETWORK / GPS / PASSIVE)
            Log.d("LOCATION_WORKER", "getCurrentLocation gagal, mencoba LocationManager fallback...")
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val netLoc = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
                if (netLoc != null) {
                    Log.d("LOCATION_WORKER", "Fallback lokasi dari NETWORK_PROVIDER")
                    return netLoc
                }

                val gpsLoc = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
                if (gpsLoc != null) {
                    Log.d("LOCATION_WORKER", "Fallback lokasi dari GPS_PROVIDER")
                    return gpsLoc
                }

                val passiveLoc = try { locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }
                if (passiveLoc != null) {
                    Log.d("LOCATION_WORKER", "Fallback lokasi dari PASSIVE_PROVIDER")
                    return passiveLoc
                }
            }

            Log.w("LOCATION_WORKER", "Semua provider lokasi gagal, return null.")
            null
        } catch (e: Exception) {
            Log.e("LOCATION_WORKER", "Error fetchFastLocation: ${e.message}", e)
            null
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "location_periodic_worker_30min"

        fun schedulePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<LocationWorker>(30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d("LOCATION_WORKER", "Penjadwalan periodik 30 menit berhasil diset ke WorkManager (KEEP).")
        }
    }
}
