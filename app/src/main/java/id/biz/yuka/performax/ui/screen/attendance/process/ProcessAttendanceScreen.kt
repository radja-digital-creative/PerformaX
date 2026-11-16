package id.biz.yuka.performax.ui.screen.attendance.process

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SweepGradient
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.performax.ui.components.LeafletMap
import id.biz.yuka.performax.ui.components.MapMarker
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

import id.biz.yuka.performax.ui.components.GlobalErrorPopup

@Composable
fun ProcessAttendanceScreen(
    onBack: () -> Unit,
    onNavigateToRequestExtraWork: () -> Unit = {},
    viewModel: ProcessAttendanceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // A. LOADING
            viewModel.attendanceState == "LOADING" -> {
                Box(modifier = Modifier.fillMaxSize().background(WonderBg), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = WonderOrange)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Memuat data absensi...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }

            // B. DILUAR JENDELA WAKTU / SERVER ERROR
            viewModel.attendanceState == "TIME_ERROR" -> {
                Column(
                    modifier = Modifier.fillMaxSize().background(WonderBg).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Info, null, tint = WonderOrange, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tidak Dapat Absen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(viewModel.timeErrorMessage, textAlign = TextAlign.Center, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.retryLoadData() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Coba Lagi")
                        }
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(12.dp)) {
                            Text("Kembali ke Beranda")
                        }
                    }

                    if (!viewModel.hasPendingRequestToday) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Ajukan Masuk Kerja Hari Ini",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToRequestExtraWork() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // C. SUDAH ABSEN MASUK
            viewModel.attendanceState == "CHECKED_IN_ONLY" -> {
                Column(
                    modifier = Modifier.fillMaxSize().background(WonderBg).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Anda sudah absen masuk!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Selamat bekerja, ${viewModel.employeeName}.\nNanti silakan lakukan absen pulang mulai jam ${viewModel.expectedCheckOutTime} WIB.", color = Color.DarkGray, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                        Text("Kembali ke Beranda")
                    }
                }
            }

            // D. SUKSES
            viewModel.attendanceState == "COMPLETED" || viewModel.uploadSuccess -> {
                val successMsg = if (viewModel.uploadSuccess) {
                    if (viewModel.currentAttendanceType == "check_in") "Absen Masuk Berhasil!" else "Absen Pulang Berhasil!"
                } else "Anda sudah absen hari ini!"

                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(120.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(successMsg, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                    Text("Semangat bekerja, ${viewModel.employeeName}", color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                        Text("Tutup Halaman")
                    }
                }
            }

            // E. TASK REVIEW
            viewModel.attendanceState == "TASK_REVIEW" -> {
                var selectedTaskIds by remember { mutableStateOf(viewModel.pendingTasks.map { it.id }.toSet()) }
                var isCompletedTasksExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxSize().background(WonderBg).padding(20.dp).statusBarsPadding().navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFFFF3E0)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, null, tint = WonderOrange, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Daily Task Report", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFF8E1), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📌 Catatan: Anda tidak bisa absen pulang sebelum melaporkan daily task report.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100), lineHeight = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(viewModel.pendingTasks) { index, task ->
                            MinimalTaskReviewCard(index + 1, task, selectedTaskIds.contains(task.id)) { checked ->
                                selectedTaskIds = if (checked) selectedTaskIds + task.id else selectedTaskIds - task.id
                            }
                        }
                        if (viewModel.completedTasks.isNotEmpty()) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { isCompletedTasksExpanded = !isCompletedTasksExpanded }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("✓ ${viewModel.completedTasks.size} Tugas Selesai", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                    Icon(if (isCompletedTasksExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF2E7D32))
                                }
                            }
                            if (isCompletedTasksExpanded) {
                                itemsIndexed(viewModel.completedTasks) { index, task -> CompletedTaskReviewCard(index + 1, task) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(onClick = { viewModel.proceedToCameraFromTaskReview() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)) {
                        Text("Laporkan dan Absen", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                        Text("Kembali ke Beranda", color = Color.White)
                    }
                }
            }

            // F. CAMERA INTERFACE (Default)
            else -> {
                CameraAttendanceInterface(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    viewModel = viewModel
                )
            }
        }

        // Dialog Peringatan Absen Manual (Wajib Baca 10 Detik)
        if (viewModel.showManualWarningDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissManualWarningDialog() },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WonderOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Peringatan Absen Manual",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Absensi manual memerlukan verifikasi & approval dari Manager.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }

                        Text(
                            "• Jika Manager menolak atau tidak memverifikasi, absensi Anda hari ini TIDAK dianggap masuk.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        Text(
                            "• Kami sangat menyarankan Anda untuk TETAP menggunakan AI Autonomous Absensi yang memverifikasi secara otomatis & instan.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        Text(
                            "• Jika tetap memilih Absen Manual, SEGERA hubungi Manager Anda setelah pengajuan berhasil.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        Text(
                            "• Catatan: Riwayat absen manual TIDAK AKAN muncul di daftar riwayat sebelum disetujui oleh Manager.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmManualWarningAndSwitch() },
                        enabled = viewModel.manualWarningCountdown == 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WonderOrange,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewModel.manualWarningCountdown > 0) {
                            Text(
                                "Saya Mengerti (${viewModel.manualWarningCountdown}s)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Saya Mengerti & Lanjutkan",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { viewModel.dismissManualWarningDialog() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batal & Gunakan AI Absen", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }

        // Global Error Popup untuk menampilkan dialog kesalahan jika ada
        GlobalErrorPopup(
            errorMessage = viewModel.globalErrorMessage,
            onDismiss = { viewModel.clearGlobalError() }
        )
    }
}

@Composable
private fun CameraAttendanceInterface(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    viewModel: ProcessAttendanceViewModel
) {
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var currentTimeString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale("id", "ID"))) + " WIB"
            delay(1000)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if ((viewModel.requirePhoto && !hasCameraPermission) || !hasLocationPermission) {
            val permissions = if (viewModel.requirePhoto) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionLauncher.launch(permissions)
        }
    }

    DisposableEffect(hasLocationPermission) {
        var locationCallback: LocationCallback? = null
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        if (id.biz.yuka.performax.utils.AttendancePolicy.isFreshLocation(android.os.SystemClock.elapsedRealtime(), it.elapsedRealtimeNanos / 1_000_000L)) {
                            viewModel.updateLocation(it.latitude, it.longitude)
                        }
                    }
                }
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
        }
        onDispose { locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) } }
    }

    val isError = viewModel.overlayMessage.contains("❌") || viewModel.isFaceTimeout
    val isProcessing = viewModel.livenessStep in 2..3
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val angle by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)))
    
    val borderBrush = remember(angle, isError, isProcessing) {
        if (isError) androidx.compose.ui.graphics.SolidColor(Color.Red)
        else if (isProcessing) object : ShaderBrush() {
            override fun createShader(size: androidx.compose.ui.geometry.Size): android.graphics.Shader {
                val shader = SweepGradient(size.width/2f, size.height/2f, intArrayOf(WonderOrange.toArgb(), Color.Transparent.toArgb(), WonderOrange.toArgb()), null)
                val matrix = Matrix(); matrix.postRotate(angle, size.width/2f, size.height/2f); shader.setLocalMatrix(matrix)
                return shader
            }
        } else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).border(4.dp, borderBrush, RectangleShape)) {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            Box(modifier = Modifier.weight(0.75f).fillMaxWidth()) {
                if (!viewModel.requirePhoto) {
                    Column(Modifier.fillMaxSize().padding(top = 140.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Absen tanpa foto", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Button(onClick = viewModel::submitWithoutPhoto, enabled = viewModel.isLocationReady && !viewModel.isUploadLoading && !viewModel.uploadSuccess) {
                            Text(if (viewModel.currentAttendanceType == "check_in") "Absen Masuk" else "Absen Pulang")
                        }
                        if (!hasLocationPermission) Text("Izinkan lokasi presisi untuk melakukan absen.", color = Color.White)
                    }
                } else if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val pv = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                            startCamera(ctx, lifecycleOwner, pv, viewModel) { det, blink, bmp ->
                                viewModel.onFaceDataReceived(det, blink, bmp)
                            }
                            pv
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                if (viewModel.isUploadLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WonderOrange, strokeWidth = 4.dp, modifier = Modifier.size(64.dp))
                    }
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (viewModel.currentAttendanceType == "check_in") "ABSEN MASUK" else "ABSEN PULANG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (viewModel.hasApprovedShiftBypass) {
                                Surface(color = WonderOrange.copy(0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("BYPASS SHIFT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WonderOrange, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (viewModel.hasApprovedOffsite) {
                                Surface(color = Color(0xFF2E7D32).copy(0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("OFFSITE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(viewModel.overlayMessage, textAlign = TextAlign.Center, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                        // ✅ TOMBOL FALLBACK MANUAL
                        if (viewModel.showManualFallback && !viewModel.isManualMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.openManualWarningDialog() },
                                colors = ButtonDefaults.textButtonColors(contentColor = WonderOrange)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mengalami kendala? Coba Absen Manual", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ✅ TOMBOL KAMERA MANUAL (FAB)
                if (viewModel.isManualMode && !viewModel.isUploadLoading) {
                    FloatingActionButton(
                        onClick = { viewModel.captureManualPhoto() },
                        containerColor = WonderOrange,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 56.dp)
                            .size(64.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Ambil Foto Manual", modifier = Modifier.size(32.dp))
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(0.6f)).padding(16.dp, 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(viewModel.storeName, color = WonderOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(viewModel.employeeName, color = Color.White, fontSize = 10.sp)
                        }
                        Text("powered by performaX", color = Color.White.copy(0.5f), fontSize = 8.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(currentTimeString, color = WonderOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.5f", viewModel.userLocation?.lat ?: 0.0)}, ${String.format("%.5f", viewModel.userLocation?.lng ?: 0.0)}", color = Color.White, fontSize = 8.sp)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(0.25f).fillMaxWidth()) {
                LeafletMap(
                    modifier = Modifier.fillMaxSize(),
                    markers = listOfNotNull(viewModel.storeLocation, viewModel.userLocation),
                    circleCenter = viewModel.storeLocation,
                    radiusInMeters = viewModel.maxRadiusMeters,
                    zoomLevel = 18
                )
            }
        }
    }
}

class FaceAnalyzer(
    private val viewModel: ProcessAttendanceViewModel,
    private val onFaceDataReceived: (isFaceDetected: Boolean, isBlinking: Boolean, bitmap: Bitmap?) -> Unit
) : ImageAnalysis.Analyzer {
    private val options = FaceDetectorOptions.Builder().setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build()
    private val detector = FaceDetection.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image).addOnSuccessListener { faces ->
                val isFaceDetected = faces.isNotEmpty()
                var isBlinking = false
                if (isFaceDetected) {
                    val face = faces.first()
                    isBlinking = (face.leftEyeOpenProbability ?: 1f) < 0.2f && (face.rightEyeOpenProbability ?: 1f) < 0.2f
                }
                var bitmap: Bitmap? = null
                if ((viewModel.needsFaceFrames && isFaceDetected) || viewModel.isManualMode) {
                    val raw = imageProxy.toBitmap()
                    val matrix = Matrix()
                    if (imageProxy.imageInfo.rotationDegrees != 0) matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    matrix.postScale(-1f, 1f)
                    bitmap = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                }
                onFaceDataReceived(isFaceDetected, isBlinking, bitmap)
            }.addOnCompleteListener { imageProxy.close() }
        } else imageProxy.close()
    }
}

private fun startCamera(ctx: Context, lo: androidx.lifecycle.LifecycleOwner, pv: PreviewView, vm: ProcessAttendanceViewModel, cb: (Boolean, Boolean, Bitmap?) -> Unit) {
    val cf = ProcessCameraProvider.getInstance(ctx)
    cf.addListener({
        val cp = cf.get()
        val p = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
        val ia = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
            it.setAnalyzer(Executors.newSingleThreadExecutor(), FaceAnalyzer(vm, cb))
        }
        try { cp.unbindAll(); cp.bindToLifecycle(lo, CameraSelector.DEFAULT_FRONT_CAMERA, p, ia) } 
        catch (e: Exception) { Log.e("Absensi", "Error CameraX: ${e.message}") }
    }, ContextCompat.getMainExecutor(ctx))
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun MinimalTaskReviewCard(idx: Int, task: TaskEntity, isChecked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onChecked(!isChecked) }, shape = RoundedCornerShape(10.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(if (isChecked) 1.5.dp else 0.5.dp, if (isChecked) WonderOrange else Color(0xFFE0E0E0))) {
        Column(modifier = Modifier.padding(12.dp, 10.dp)) {
            Text("$idx. ${task.title}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("Deadline: ${task.dueDate.take(10)}", fontSize = 11.sp, color = Color(0xFFD32F2F))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(isChecked, onChecked, colors = CheckboxDefaults.colors(checkedColor = WonderOrange))
                Text("Tandai untuk kerjakan besok", fontSize = 11.sp, color = if (isChecked) WonderOrange else Color.Gray)
            }
        }
    }
}

@Composable
private fun CompletedTaskReviewCard(idx: Int, task: TaskEntity) {
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)), shape = RoundedCornerShape(10.dp), color = Color(0xFFF5F5F5), border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE0E0E0))) {
        Row(modifier = Modifier.padding(12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$idx. ${task.title}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF424242), maxLines = 1)
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) { Text("Selesai", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(6.dp, 2.dp)) }
        }
    }
}