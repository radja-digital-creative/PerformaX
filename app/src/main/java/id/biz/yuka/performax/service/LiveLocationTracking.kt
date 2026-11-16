package id.biz.yuka.performax.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import id.biz.yuka.libperformx.SupabaseConfig
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Automatic visible tracking while signed in; Android location permissions still apply. */
object LiveLocationTracking {
    private val mutableStatus = MutableStateFlow("Lokasi realtime belum aktif")
    val status = mutableStatus.asStateFlow()
    fun report(message: String) { mutableStatus.value = message }
    private fun prefs(context: Context) = context.getSharedPreferences("live_location_tracking", Context.MODE_PRIVATE)
    fun enabledUser(context: Context): String? = prefs(context).getString("active_user", null)

    fun startFromHome(context: Context, userId: String, role: String) {
        if (role.lowercase() == "owner") return
        prefs(context).edit().putString("active_user", userId).apply()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            report("Aktifkan izin lokasi presisi untuk realtime"); return
        }
        try {
            ContextCompat.startForegroundService(context, Intent(context, LocationForegroundService::class.java))
        } catch (_: Exception) { report("Lokasi belum aktif. Buka kembali aplikasi untuk mencoba lagi.") }
    }

    fun resumeIfEnabled(context: Context) {
        val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
        if (enabledUser(context) == userId) startFromHome(context, userId, "employee")
    }

    fun stopForLogout(context: Context) {
        prefs(context).edit().remove("active_user").apply()
        report("Lokasi realtime belum aktif")
        context.stopService(Intent(context, LocationForegroundService::class.java))
    }
}
