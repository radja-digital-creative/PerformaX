package id.biz.yuka.performax.ui.screen.splash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AppUpdateRepository
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.data.SessionManager
import id.biz.yuka.performax.ui.navigation.Screen
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

sealed class SplashUIState {
    object Checking : SplashUIState()
    object LocationPermissionRequired : SplashUIState()
    object BackgroundLocationRequired : SplashUIState()
    object UpdateAvailable : SplashUIState()
    data class InstallPermissionDenied(val apkFile: File) : SplashUIState()
    data class Downloading(val message: String, val progress: Int) : SplashUIState()
    data class DownloadComplete(val apkFile: File) : SplashUIState()
    data class Error(val message: String) : SplashUIState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val appUpdateRepository: AppUpdateRepository
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<Screen>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    var uiState by mutableStateOf<SplashUIState>(SplashUIState.Checking)
        private set

    private var latestApkUrl: String? = null
    var releaseDetails by mutableStateOf<id.biz.yuka.libperformx.entity.AppVersionEntity?>(null)
        private set
    private var downloadJob: kotlinx.coroutines.Job? = null
    private var versionCheckJob: kotlinx.coroutines.Job? = null
    private var navigating = false

    fun skipUpdate() {
        downloadJob?.cancel()
        checkSession()
    }


    // Deep Linking / Notification Navigation
    private var pendingNotifType: String? = null
    private var pendingNotifId: String? = null

    fun setPendingNotification(type: String?, id: String?) {
        pendingNotifType = type
        pendingNotifId = id
    }



    /**
     * Dipanggil dari SplashScreen setelah cek Notifikasi selesai.
     * Cek apakah location permission sudah di-grant "Allow all the time".
     */
    fun checkLocationPermission(context: Context, currentVersionCode: Int) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
            uiState = SplashUIState.LocationPermissionRequired
            return
        }

        // Android 10+ (Q): cek ACCESS_BACKGROUND_LOCATION untuk "Allow all the time"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackgroundLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackgroundLocation) {
                uiState = SplashUIState.BackgroundLocationRequired
                return
            }
        }

        // Semua location permission OK, lanjut cek app version
        checkAppVersion(currentVersionCode)
    }

    fun startPeriodicLocationWorker(context: Context) {
        id.biz.yuka.performax.service.LocationWorker.schedulePeriodicWork(context)
    }

    fun checkAppVersion(currentVersionCode: Int) {
        if (versionCheckJob?.isActive == true || navigating) return
        versionCheckJob = viewModelScope.launch {
            try {
                val result = id.biz.yuka.performax.update.UpdatePolicy.checkWithin {
                    appUpdateRepository.getLatestAppVersion()
                }
                when (result) {
                    is ResultState.Success -> {
                        val latestVersion = result.data
                        if (latestVersion.versionCode > currentVersionCode) {
                            releaseDetails = latestVersion
                            latestApkUrl = latestVersion.apkUrl
                            uiState = SplashUIState.UpdateAvailable
                        } else {
                            checkSession()
                        }
                    }
                    is ResultState.Error, null -> checkSession()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                checkSession()
            }
        }
    }

    fun setInstallPermissionDenied(file: File) {
        uiState = SplashUIState.InstallPermissionDenied(file)
    }



    fun downloadAndSaveApk(context: Context) {
        val url = latestApkUrl ?: return
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            uiState = SplashUIState.Downloading("Menyambungkan ke server…", 0)
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val partial = File(directory, "performaX_update.pending.apk")
            try {
                when (val result = appUpdateRepository.downloadApk(url, partial) { received, total ->
                    val progress = if (total > 0) ((received * 100 / total).toInt()).coerceIn(0, 100) else 0
                    val message = if (total > 0) "Mengunduh pembaruan…" else "Terunduh ${received / 1024} KB"
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { uiState = SplashUIState.Downloading(message, progress) }
                }) {
                    is ResultState.Success -> {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val pm = context.packageManager
                            val apk = pm.getPackageArchiveInfo(partial.absolutePath, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                                ?: error("File pembaruan bukan APK yang valid.")
                            val installed = pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                            fun signers(info: android.content.pm.PackageInfo) = info.signingInfo?.apkContentsSigners
                                ?.map { it.toCharsString() }?.toSet().orEmpty()
                            require(id.biz.yuka.performax.update.UpdatePolicy.canInstall(
                                apk.packageName, context.packageName, apk.longVersionCode, installed.longVersionCode,
                                signers(apk), signers(installed))) {
                                "Pembaruan tidak cocok dengan aplikasi terpasang. Hubungi admin; jangan hapus aplikasi lama."
                            }
                            require(apk.longVersionCode == releaseDetails?.versionCode?.toLong()) { "Versi APK tidak sesuai informasi pembaruan." }
                        }
                        val apkFile = File(directory, "performaX_update.apk")
                        if (apkFile.exists()) apkFile.delete()
                        check(partial.renameTo(apkFile)) { "Gagal menyimpan APK pembaruan." }
                        uiState = SplashUIState.DownloadComplete(apkFile)
                    }
                    is ResultState.Error -> uiState = SplashUIState.Error("Gagal mengunduh: ${result.message}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = SplashUIState.Error(e.message ?: "Pembaruan gagal. Silakan coba lagi.")
            } finally { partial.delete() }
        }
    }

    private fun checkSession() {
        if (navigating) return
        navigating = true
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2_000) {
                    SupabaseConfig.client.auth.sessionStatus.first {
                        it !is io.github.jan.supabase.gotrue.SessionStatus.LoadingFromStorage
                    }
                }
                val isLoggedIn = sessionManager.isLoggedIn.first()
                val supabaseSession = SupabaseConfig.client.auth.currentSessionOrNull()

                if (isLoggedIn && supabaseSession != null) {
                    // Cek apakah ada navigasi tertunda dari notifikasi
                    val targetScreen = when (pendingNotifType) {
                        "task" -> {
                            if (!pendingNotifId.isNullOrBlank()) {
                                Screen.TaskDetail.createRoute(pendingNotifId!!)
                            } else {
                                android.util.Log.e("SPLASH_NAV", "Navigasi task dibatalkan karena ID kosong")
                                null
                            }
                        }
                        "leave_request" -> {
                            if (!pendingNotifId.isNullOrBlank()) {
                                Screen.ReviewLeave.route
                            } else {
                                android.util.Log.e("SPLASH_NAV", "Navigasi izin dibatalkan karena ID kosong")
                                null
                            }
                        }
                        "reminder_in", "reminder_out" -> Screen.Attendance.route
                        else -> null
                    }
                    
                    if (targetScreen != null) {
                        android.util.Log.d("SPLASH_NAV", "Navigasi ke DeepLink: $targetScreen")
                        _navigationEvent.emit(Screen.DeepLink(targetScreen))
                    } else {
                        _navigationEvent.emit(Screen.Home)
                    }
                } else {
                    _navigationEvent.emit(Screen.Login)
                }
            } catch (e: Exception) {
                _navigationEvent.emit(Screen.Login)
            }
        }
    }
}