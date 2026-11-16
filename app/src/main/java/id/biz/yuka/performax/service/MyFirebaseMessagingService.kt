package id.biz.yuka.performax.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import id.biz.yuka.performax.MainActivity
import id.biz.yuka.performax.R
import id.biz.yuka.performax.utils.ChatEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Notifikasi Baru"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        var isUserInActiveChatRoom = false
        var incomingId: String? = null
        var incomingType: String? = null

        if (message.data.containsKey("type")) {
            incomingType = message.data["type"]
            val idKey = "${incomingType}_id"
            incomingId = message.data[idKey] ?: message.data["id"]
        }

        if (message.data.containsKey("payload")) {
            try {
                val jsonObject = JSONObject(message.data["payload"]!!)
                if (incomingType == null) incomingType = jsonObject.optString("type")
                
                val idKey = "${incomingType}_id"
                if (incomingId == null) {
                    incomingId = jsonObject.optString(idKey).takeIf { it.isNotBlank() }
                        ?: jsonObject.optString("id").takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                Log.e("FCM_DEBUG", "Gagal parsing JSON payload: ${e.message}")
            }
        }
        if (incomingType == "REQUEST_LIVE_LOCATION" || incomingType == "REQUEST_LOCATION") {

            if (LocationForegroundService.isRunning) {
                return
            }

            try {
                val serviceIntent = Intent(this, LocationForegroundService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: Exception) {
                Log.w("LOCATION_REFRESH", "Layanan lokasi ditolak Android: ${e.javaClass.simpleName}")
            }
            return
        }

        if (incomingType != null && incomingId != null) {
            if (ChatEventBus.isRoomActive(incomingType, incomingId)) {
                isUserInActiveChatRoom = true

                CoroutineScope(Dispatchers.Main).launch {
                    ChatEventBus.emitNewEvent(incomingType, incomingId)
                }
            }
        }

        if (!isUserInActiveChatRoom) {
            showNotification(title, body, incomingType, incomingId)
        }
    }

    private fun showNotification(title: String, message: String, type: String? = null, id: String? = null) {
        val channelId = "performax_general_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "General Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notif_type", type)
            putExtra("notif_id", id)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 
            System.currentTimeMillis().toInt(), 
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // ✅ SET PENDING INTENT

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
