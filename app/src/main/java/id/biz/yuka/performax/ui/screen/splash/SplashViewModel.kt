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
        viewModelScope.launch {
            try {
                when (val result = appUpdateRepository.getLatestAppVersion()) {
                    is ResultState.Success -> {
                        val latestVersion = result.data
                        if (latestVersion.versionCode > currentVersionCode) {
                            latestApkUrl = latestVersion.apkUrl
                            uiState = SplashUIState.UpdateAvailable
                        } else {
                            checkSession()
                        }
                    }
                    is ResultState.Error -> {
                        checkSession()
                    }
                }
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

        viewModelScope.launch {
            var currentProgress = 0
            uiState = SplashUIState.Downloading("Menyambungkan ke server...", currentProgress)

            val progressJob = launch {
                while (currentProgress < 95) {
                    delay((100..400).random().toLong())
                    currentProgress += (1..4).random()
                    if (currentProgress > 95) currentProgress = 95
                    uiState = SplashUIState.Downloading("Mengunduh pembaruan...", currentProgress)
                }
            }

            when (val result = appUpdateRepository.downloadApk(url)) {
                is ResultState.Success -> {
                    progressJob.cancel()
                    try {
                        uiState = SplashUIState.Downloading("Menyimpan file...", 98)

                        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        if (downloadDir != null && !downloadDir.exists()) downloadDir.mkdirs()

                        val apkFile = File(downloadDir, "performaX_update.apk")
                        val fos = FileOutputStream(apkFile)
                        fos.write(result.data)
                        fos.close()

                        uiState = SplashUIState.Downloading("Selesai!", 100)
                        delay(500)

                        uiState = SplashUIState.DownloadComplete(apkFile)

                    } catch (e: Exception) {
                        uiState = SplashUIState.Error("Gagal menyimpan file: ${e.localizedMessage}")
                        delay(2000)
                        checkSession()
                    }
                }
                is ResultState.Error -> {
                    progressJob.cancel()
                    uiState = SplashUIState.Error("Gagal mengunduh: ${result.message}")
                    delay(2000)
                    checkSession()
                }
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            try {
                SupabaseConfig.client.auth.currentSessionOrNull()
                delay(1500)
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