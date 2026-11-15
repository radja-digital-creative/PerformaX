package id.biz.yuka.performax.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.AndroidEntryPoint
import id.biz.yuka.libperformx.repository.LocationRepository
import id.biz.yuka.performax.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient

    @Inject
    lateinit var locationRepository: LocationRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "performax_location_service"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Layanan Pemantauan Lokasi",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Memperbarui Lokasi Kerja")
            .setContentText("Mengirim lokasi...")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Guard: jika sudah ada service yang sedang berjalan, skip
        if (isRunning) {
            Log.w("LOCATION_SERVICE", "Service sudah aktif, skip duplikat onStartCommand.")
            return START_NOT_STICKY
        }

        isRunning = true

        serviceScope.launch {
            try {
                Log.d("LOCATION_SERVICE", "Mulai mengambil lokasi cepat on-demand...")

                // Cek status GPS menggunakan LocationManager standar
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
                                   locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!isGpsEnabled) {
                    Log.d("LOCATION_SERVICE", "GPS terdeteksi OFF.")
                }

                // Ambil lokasi cepat dengan total timeout 15 detik
                val location = withTimeoutOrNull(15_000L) {
                    fusedLocationClient.fetchFastLocation(applicationContext)
                }

                if (location != null) {
                    Log.d("LOCATION_SERVICE", "Lokasi cepat ditemukan: lat=${location.latitude}, lng=${location.longitude}")
                    val result = locationRepository.recordUserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        provider = "ondemand_fcm"
                    )
                    Log.d("LOCATION_SERVICE", "Hasil simpan lokasi ke Supabase: $result")
                } else {
                    Log.w("LOCATION_SERVICE", "Gagal mendapatkan lokasi (timeout atau semua provider null).")
                }
            } catch (e: Exception) {
                Log.e("LOCATION_SERVICE", "Error di LocationForegroundService: ${e.message}", e)
            } finally {
                Log.d("LOCATION_SERVICE", "Menghentikan LocationForegroundService...")
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private suspend fun FusedLocationProviderClient.fetchFastLocation(context: Context): Location? {
        return try {
            // Step 1: Check lastLocation cache (~5ms)
            val lastLoc = try { lastLocation.await() } catch (_: Exception) { null }
            if (lastLoc != null) {
                Log.d("LOCATION_SERVICE", "Lokasi cepat didapat dari lastLocation: lat=${lastLoc.latitude}, lng=${lastLoc.longitude}")
                return lastLoc
            }

            // Step 2: getCurrentLocation() — API one-shot yang paling reliable
            Log.d("LOCATION_SERVICE", "lastLocation null, mencoba getCurrentLocation(HIGH_ACCURACY)...")
            val cts = CancellationTokenSource()
            val currentLoc = try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(10_000L)
                    .setMaxUpdateAgeMillis(5_000L)
                    .build()
                getCurrentLocation(request, cts.token).await()
            } catch (e: Exception) {
                Log.w("LOCATION_SERVICE", "getCurrentLocation HIGH_ACCURACY gagal: ${e.message}")
                null
            }
            if (currentLoc != null) {
                Log.d("LOCATION_SERVICE", "Lokasi didapat dari getCurrentLocation: lat=${currentLoc.latitude}, lng=${currentLoc.longitude}")
                return currentLoc
            }

            // Step 3: getCurrentLocation() dengan BALANCED_POWER (bisa lebih cepat via cell/wifi)
            Log.d("LOCATION_SERVICE", "HIGH_ACCURACY gagal, mencoba BALANCED_POWER_ACCURACY...")
            val cts2 = CancellationTokenSource()
            val balancedLoc = try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setDurationMillis(5_000L)
                    .setMaxUpdateAgeMillis(30_000L)
                    .build()
                getCurrentLocation(request, cts2.token).await()
            } catch (e: Exception) {
                Log.w("LOCATION_SERVICE", "getCurrentLocation BALANCED gagal: ${e.message}")
                null
            }
            if (balancedLoc != null) {
                Log.d("LOCATION_SERVICE", "Lokasi didapat dari getCurrentLocation(BALANCED): lat=${balancedLoc.latitude}, lng=${balancedLoc.longitude}")
                return balancedLoc
            }

            // Step 4: LocationManager fallback (NETWORK / GPS / PASSIVE)
            Log.d("LOCATION_SERVICE", "getCurrentLocation gagal, mencoba LocationManager fallback...")
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val netLoc = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
                if (netLoc != null) {
                    Log.d("LOCATION_SERVICE", "Fallback lokasi dari NETWORK_PROVIDER")
                    return netLoc
                }

                val gpsLoc = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
                if (gpsLoc != null) {
                    Log.d("LOCATION_SERVICE", "Fallback lokasi dari GPS_PROVIDER")
                    return gpsLoc
                }

                val passiveLoc = try { locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }
                if (passiveLoc != null) {
                    Log.d("LOCATION_SERVICE", "Fallback lokasi dari PASSIVE_PROVIDER")
                    return passiveLoc
                }
            }

            Log.w("LOCATION_SERVICE", "Semua provider lokasi gagal, return null.")
            null
        } catch (e: Exception) {
            Log.e("LOCATION_SERVICE", "Error fetchFastLocation: ${e.message}", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 8801

        /** Flag volatile agar LocationWorker bisa cek apakah service sedang aktif */
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
