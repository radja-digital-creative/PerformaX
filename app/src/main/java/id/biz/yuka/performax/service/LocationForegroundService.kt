package id.biz.yuka.performax.service

import android.annotation.SuppressLint
import id.biz.yuka.performax.utils.fetchFreshLocation
import kotlinx.coroutines.CancellationException
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
                val location = withTimeoutOrNull(28_000L) {
                    fusedLocationClient.fetchFreshLocation()
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
            } catch (e: CancellationException) { throw e
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
