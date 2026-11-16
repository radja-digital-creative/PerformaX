package id.biz.yuka.performax.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.LocationRepository
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.MainActivity
import id.biz.yuka.performax.R
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class LocationForegroundService : Service() {
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var locationRepository: LocationRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trackingJob: Job? = null
    private val fixes = Channel<Location>(Channel.CONFLATED)
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) { result.lastLocation?.let { fixes.trySend(it) } }
        override fun onLocationAvailability(availability: LocationAvailability) {
            if (!availability.isLocationAvailable) status("Menunggu GPS • lokasi belum diperbarui")
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(message: String): android.app.Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Lokasi realtime karyawan", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 8801, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pause = PendingIntent.getService(this, 8802, Intent(this, LocationForegroundService::class.java).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher_round).setContentTitle("PerformaX • lokasi realtime aktif")
            .setContentText(message).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(0, "Jeda lokasi", pause).build()
    }
    private fun status(message: String) {
        LiveLocationTracking.report(message)
        if (isRunning) (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification(message))
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PAUSE) { LiveLocationTracking.pause(this); return START_NOT_STICKY }
        if (trackingJob?.isActive == true) return START_STICKY
        val enabledId = LiveLocationTracking.enabledUser(this)
        if (enabledId == null || LiveLocationTracking.isPaused(this, enabledId)) { stopSelf(); return START_NOT_STICKY }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification("Menunggu GPS…"), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } catch (_: Exception) {
            LiveLocationTracking.report("Izin lokasi belum tersedia. Buka aplikasi untuk mengaktifkan.")
            stopSelf(); return START_NOT_STICKY
        }
        isRunning = true
        status("Lokasi realtime aktif • menunggu GPS")
        trackingJob = scope.launch {
            try {
                withTimeoutOrNull(5_000) {
                    SupabaseConfig.client.auth.sessionStatus.first { it !is SessionStatus.LoadingFromStorage }
                }
                if (SupabaseConfig.client.auth.currentUserOrNull()?.id != enabledId) return@launch
                val gate = LocationDeliveryGate()
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LocationDeliveryGate.INTERVAL_MS)
                    .setMinUpdateIntervalMillis(10_000).setMaxUpdateDelayMillis(15_000).setMaxUpdateAgeMillis(0).build()
                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper()).await()
                for (location in fixes) {
                    if (SupabaseConfig.client.auth.currentUserOrNull()?.id != enabledId || LiveLocationTracking.isPaused(this@LocationForegroundService, enabledId)) break
                    val now = SystemClock.elapsedRealtime()
                    if (!gate.shouldUpload(now, location.elapsedRealtimeNanos / 1_000_000, location.latitude, location.longitude)) continue
                    val result = withTimeoutOrNull(15_000) {
                        locationRepository.recordUserLocation(location.latitude, location.longitude, "realtime_foreground",
                            Instant.ofEpochMilli(location.time).toString(), expectedUserId = enabledId)
                    }
                    if (result is ResultState.Success) {
                        gate.markSuccess(now)
                        val time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Asia/Jakarta")).format(Instant.now())
                        status("Lokasi realtime aktif • terkirim $time WIB")
                    } else status("Menunggu koneksi • lokasi belum terkirim")
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { status("Lokasi terhenti. Periksa izin/GPS lalu buka aplikasi.")
            } finally {
                fusedLocationClient.removeLocationUpdates(callback)
                isRunning = false
                val previousStatus = LiveLocationTracking.status.value
                if (previousStatus.startsWith("Lokasi realtime aktif") || previousStatus.startsWith("Menunggu")) {
                    LiveLocationTracking.report("Lokasi realtime berhenti • buka aplikasi untuk mengaktifkan")
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }
    override fun onDestroy() {
        scope.cancel()
        fusedLocationClient.removeLocationUpdates(callback)
        fixes.close()
        isRunning = false
        super.onDestroy()
    }
    companion object {
        private const val CHANNEL = "performax_location_service"
        private const val NOTIFICATION_ID = 8801
        const val ACTION_PAUSE = "id.biz.yuka.performax.PAUSE_LOCATION"
        @Volatile var isRunning = false
            private set
    }
}
