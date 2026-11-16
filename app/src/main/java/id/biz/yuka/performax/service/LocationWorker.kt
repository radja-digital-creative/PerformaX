package id.biz.yuka.performax.service

import android.annotation.SuppressLint
import id.biz.yuka.performax.utils.fetchFreshLocation
import kotlinx.coroutines.CancellationException
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import id.biz.yuka.libperformx.SupabaseConfig
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
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
        val ready = withTimeoutOrNull(5_000) { SupabaseConfig.client.auth.sessionStatus.first { it !is SessionStatus.LoadingFromStorage } }
        if (ready == null) return Result.retry()
        val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return Result.success()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return Result.success()
        if (android.os.Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) return Result.success()

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

                val repoResult = withTimeoutOrNull(15_000) { locationRepository.recordUserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    provider = "passive_workmanager",
                    recordedAt = Instant.ofEpochMilli(location.time).toString(), expectedUserId = userId
                ) }
                if (repoResult !is id.biz.yuka.libperformx.utils.ResultState.Success) return Result.retry()
            } else {
                return Result.retry()
            }

            Result.success()
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Log.e("LOCATION_WORKER", "Error di LocationWorker: ${e.javaClass.simpleName}")
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "location_periodic_worker_30min"

        fun requestImmediate(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("location_on_demand", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<LocationWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .apply { if (android.os.Build.VERSION.SDK_INT >= 31) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) }.build())
        }

        fun schedulePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d("LOCATION_WORKER", "Penjadwalan cadangan 15 menit (UPDATE).")
        }
    }
}
