package id.biz.yuka.performax.ui.screen.splash

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.BuildConfig
import id.biz.yuka.performax.ui.navigation.Screen
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

// ── Light Enterprise Palette (Matching Application Theme) ───────────────────
private val LightBg = WonderBg // Color(0xFFF4F6F8)
private val LightCardBg = Color.White
private val EmeraldGreen = Color(0xFF2E7D32)
private val TextDark = Color(0xFF1E293B)
private val TextMuted = WonderGrey

fun Context.findActivity(): androidx.activity.ComponentActivity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is androidx.activity.ComponentActivity) return context
        context = context.baseContext
    }
    return context as? androidx.activity.ComponentActivity
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashScreen(
    onNavigate: (Screen) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

    // ── 1. Permission Launchers ─────────────────────────────────────────────────
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.checkLocationPermission(context, BuildConfig.VERSION_CODE)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.checkLocationPermission(context, BuildConfig.VERSION_CODE)
    }

    val backgroundLocationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkLocationPermission(context, BuildConfig.VERSION_CODE)
    }

    LaunchedEffect(Unit) {
        // --- 📥 BACA INTENT DARI ACTIVITY (Lebih Robust) ---
        val activity = context.findActivity()
        activity?.intent?.let { intent ->
            val type = intent.getStringExtra("notif_type")
            val id = intent.getStringExtra("notif_id")
            
            // Log untuk debug di Logcat
            android.util.Log.d("SPLASH_NAV", "Intent Terdeteksi - Type: $type | ID: $id")
            
            if (type != null) {
                viewModel.setPendingNotification(type, id)
                // Bersihkan intent agar tidak navigasi ulang saat rotate
                intent.removeExtra("notif_type")
                intent.removeExtra("notif_id")
            }
        }
        
        viewModel.startPeriodicLocationWorker(context)
        if (Build.VERSION.SDK_INT >= 33) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.checkLocationPermission(context, BuildConfig.VERSION_CODE)
            }
        } else {
            viewModel.checkLocationPermission(context, BuildConfig.VERSION_CODE)
        }
    }

    // ── 2. Navigasi ─────────────────────────────────────────────────────────────
    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collect { screen ->
            if (screen is Screen.DeepLink) {
                onNavigate(screen) // Dispatcher di AppNav akan menangani DeepLink
            } else {
                onNavigate(screen)
            }
        }
    }

    // ── 3. APK Installer Functions ─────────────────────────────────────────────
    fun installApk(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
                val state = viewModel.uiState
                if (state is SplashUIState.InstallPermissionDenied) {
                    installApk(state.apkFile)
                }
            }
        }
    }

    fun checkAndInstallApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                viewModel.setInstallPermissionDenied(file)
                return
            }
        }
        installApk(file)
    }

    // ── 4. Update Handler ───────────────────────────────────────────────────────
    LaunchedEffect(uiState) {
        if (uiState is SplashUIState.DownloadComplete) {
            checkAndInstallApk(uiState.apkFile)

        }
    }

    BackHandler(enabled = uiState is SplashUIState.Downloading) { viewModel.skipUpdate() }
    BackHandler(enabled = uiState is SplashUIState.InstallPermissionDenied || uiState is SplashUIState.DownloadComplete || uiState is SplashUIState.UpdateAvailable || uiState is SplashUIState.Error) { viewModel.skipUpdate() }

    // ── 5. ANIMATIONS & STATES (Biometric Node Network Theme) ──────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_infinite")

    // Laser Line Scan Animation (meluncur 0f s/d 1f)
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    // Wave Progress Offset
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // Intro Animations
    val logoScale = remember { Animatable(0.7f) }
    val logoAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    // Reticle Contracting Box Animation (Spring)
    val reticlePadding = remember { Animatable(48f) }

    // Access Granted Flash State
    val isAccessGranted = uiState is SplashUIState.DownloadComplete
    val activeAccentColor by animateColorAsState(
        targetValue = if (isAccessGranted) EmeraldGreen else WonderOrange,
        animationSpec = tween(durationMillis = 400),
        label = "accentColor"
    )

    // Dynamic Telemetry Text (Polos tanpa ikon, Uppercase Clean)
    val telemetryMessages = listOf(
        "INITIALIZING AI BIOMETRIC CORE...",
        "SYNCING GEOLOCATION SENSORS...",
        "CHECKING SYSTEM INTEGRITY & UPDATES...",
        "SECURITY TOKEN VERIFIED"
    )
    var currentTelemetryIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(600))
        }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            reticlePadding.animateTo(
                16f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        delay(400)
        subtitleAlpha.animateTo(1f, tween(600))

        // Sequential Telemetry Loop
        while (true) {
            delay(700)
            if (currentTelemetryIndex < telemetryMessages.size - 1) {
                currentTelemetryIndex++
            }
        }
    }

    // ── 6. UI RENDERER (Clean Light Theme) ──────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg),
        contentAlignment = Alignment.Center
    ) {
        // A. Background Constellation Node Grid (Soft Light Mesh)
        ConstellationNodeGrid(accentColor = activeAccentColor)

        // B. Center Content (Logo + Scanner Reticle + Telemetry)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)
        ) {

            // Scanner Box (Logo + Reticle + Laser Line)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(reticlePadding.value.dp)
                    .wrapContentSize()
            ) {
                // Siku-siku Reticle 4 Sudut (Viewfinder)
                ReticleCornerBrackets(
                    color = activeAccentColor,
                    bracketSize = 20.dp,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.matchParentSize()
                )

                // Laser Scan Line
                Canvas(modifier = Modifier.matchParentSize()) {
                    val laserY = size.height * laserPosition
                    val laserBrush = Brush.verticalGradient(
                        colors = listOf(
                            activeAccentColor.copy(alpha = 0f),
                            activeAccentColor.copy(alpha = 0.75f),
                            activeAccentColor.copy(alpha = 0f)
                        ),
                        startY = maxOf(0f, laserY - 12f),
                        endY = minOf(size.height, laserY + 12f)
                    )
                    drawRect(
                        brush = laserBrush,
                        topLeft = Offset(0f, maxOf(0f, laserY - 6f)),
                        size = androidx.compose.ui.geometry.Size(size.width, 12f)
                    )
                    // Garis inti laser presisi
                    drawLine(
                        color = activeAccentColor,
                        start = Offset(0f, laserY),
                        end = Offset(size.width, laserY),
                        strokeWidth = 2f
                    )
                }

                // Logo Text "performaX" (Warna utama Dark Slate, X Orange/Emerald)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(logoAlpha.value)
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    val annotatedTitle = buildAnnotatedString {
                        withStyle(SpanStyle(color = TextDark)) {
                            append("performa")
                        }
                        withStyle(SpanStyle(color = activeAccentColor)) {
                            append("X")
                        }
                    }

                    Text(
                        text = annotatedTitle,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.5).sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "ENTERPRISE MANAGEMENT SYSTEM",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextMuted,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // C. Live Telemetry Status Text (Polos Tanpa Ikon)
            val activeTelemetryText = when (uiState) {
                is SplashUIState.Downloading -> "DOWNLOADING UPDATE (${(uiState as SplashUIState.Downloading).progress}%)..."
                is SplashUIState.DownloadComplete -> "SECURITY TOKEN VERIFIED"
                is SplashUIState.LocationPermissionRequired,
                is SplashUIState.BackgroundLocationRequired,
                is SplashUIState.InstallPermissionDenied -> "ATTENTION REQUIRED"
                else -> telemetryMessages[currentTelemetryIndex]
            }

            AnimatedContent(
                targetState = activeTelemetryText,
                transitionSpec = { fadeIn(tween(300)) with fadeOut(tween(300)) },
                label = "telemetry"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = if (text.contains("ATTENTION")) WonderOrange else activeAccentColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D. Neon Wave Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.08f))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val waveBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            activeAccentColor,
                            WonderOrange,
                            activeAccentColor,
                            Color.Transparent
                        ),
                        startX = waveOffset - 400f,
                        endX = waveOffset
                    )
                    drawRect(brush = waveBrush)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // E. Interactive State Containers (Clean White Cards)
            Box(modifier = Modifier.fillMaxWidth()) {
                when (uiState) {
                    is SplashUIState.UpdateAvailable -> {
                        LightEnterpriseCard {
                            Text("Pembaruan tersedia: ${viewModel.releaseDetails?.versionName.orEmpty()}", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(viewModel.releaseDetails?.releaseNotes.orEmpty(), modifier = Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState()))
                            Button(onClick = { viewModel.downloadAndSaveApk(context) }) { Text("Unduh & Perbarui") }
                            TextButton(onClick = viewModel::skipUpdate) { Text("Nanti, buka aplikasi") }
                        }
                    }

                    is SplashUIState.LocationPermissionRequired -> {
                        LightEnterpriseCard {
                            Text(
                                "Izin Lokasi Diperlukan",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Aplikasi ini membutuhkan akses lokasi presisi untuk memverifikasi kehadiran Anda secara real-time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Izinkan Lokasi Presisi", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    is SplashUIState.BackgroundLocationRequired -> {
                        LightEnterpriseCard {
                            Text(
                                "Akses Lokasi Latar Belakang",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Agar fitur presensi dapat memverifikasi kehadiran Anda secara valid, harap pilih opsi 'Selalu Izinkan' (Allow all the time) pada pengaturan lokasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    backgroundLocationSettingsLauncher.launch(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Buka Pengaturan Lokasi", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    is SplashUIState.Downloading -> {
                        val downloadState = uiState as SplashUIState.Downloading
                        val currentProgress = downloadState.progress

                        val animatedProgress by animateFloatAsState(
                            targetValue = currentProgress / 100f,
                            animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                            label = "dlProgress"
                        )

                        LightEnterpriseCard {
                            Text("Mengunduh Pembaruan System", fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    color = WonderOrange,
                                    trackColor = Color.Black.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$currentProgress%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WonderOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = downloadState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Unduhan dapat dibatalkan jika koneksi lambat.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = viewModel::skipUpdate) { Text("Batalkan, buka aplikasi") }
                        }
                    }

                    is SplashUIState.InstallPermissionDenied -> {
                        LightEnterpriseCard {
                            Text("Izin Install Aplikasi Diperlukan", fontWeight = FontWeight.Bold, color = TextDark, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aplikasi memerlukan izin untuk memasang pembaruan terkini. Silakan aktifkan izin di Pengaturan.", style = MaterialTheme.typography.bodySmall, color = TextMuted, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = Uri.parse("package:${context.packageName}") }
                                        installPermissionLauncher.launch(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Buka Pengaturan", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            TextButton(onClick = viewModel::skipUpdate) { Text("Lanjut ke aplikasi") }
                        }
                    }

                    is SplashUIState.Error -> {
                        LightEnterpriseCard {
                            Text((uiState as SplashUIState.Error).message)
                            Button(onClick = { viewModel.downloadAndSaveApk(context) }) { Text("Coba Lagi") }
                            TextButton(onClick = viewModel::skipUpdate) { Text("Lanjut ke aplikasi") }
                        }
                    }

                    is SplashUIState.DownloadComplete -> {
                        LightEnterpriseCard {
                            Text("APK siap dipasang", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            Button(onClick = { checkAndInstallApk((uiState as SplashUIState.DownloadComplete).apkFile) }) {
                                Text("Buka pemasang Android")
                            }
                            TextButton(onClick = viewModel::skipUpdate) { Text("Lanjut ke aplikasi") }
                        }
                    }

                    else -> {
                        // Inisialisasi normal
                    }
                }
            }
        }
    }
}

// ── CUSTOM CANVAS COMPONENTS ──────────────────────────────────────────────────

/** Merender 18 Node Titik Cahaya Soft di Latar Belakang Terang */
@Composable
private fun ConstellationNodeGrid(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "nodes")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val nodes = remember {
        List(18) {
            Offset(
                x = Random.nextFloat(),
                y = Random.nextFloat()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val calculatedNodes = nodes.mapIndexed { idx, node ->
            val angle = Math.toRadians((phase + idx * 20).toDouble())
            val dx = (Math.sin(angle) * 15).toFloat()
            val dy = (Math.cos(angle) * 15).toFloat()
            Offset(node.x * w + dx, node.y * h + dy)
        }

        // Gambar Garis Antar Node yang Berdekatan (Soft Light Theme)
        for (i in calculatedNodes.indices) {
            for (j in i + 1 until calculatedNodes.size) {
                val n1 = calculatedNodes[i]
                val n2 = calculatedNodes[j]
                val dist = (n1 - n2).getDistance()
                if (dist < 220f) {
                    val lineAlpha = (1f - (dist / 220f)) * 0.18f
                    drawLine(
                        color = Color(0xFFCBD5E1).copy(alpha = lineAlpha),
                        start = n1,
                        end = n2,
                        strokeWidth = 1f
                    )
                }
            }
        }

        // Gambar Titik Node (Orange Soft & Slate)
        calculatedNodes.forEachIndexed { idx, pos ->
            val dotColor = if (idx % 3 == 0) accentColor.copy(alpha = 0.35f) else Color(0xFF94A3B8).copy(alpha = 0.3f)
            drawCircle(
                color = dotColor,
                radius = 3.5f,
                center = pos
            )
        }
    }
}

/** Merender 4 Siku-siku Viewfinder di Sudut Box Scanner */
@Composable
private fun ReticleCornerBrackets(
    color: Color,
    bracketSize: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = bracketSize.toPx()
        val sw = strokeWidth.toPx()

        // Top Left Bracket
        drawLine(color, Offset(0f, 0f), Offset(s, 0f), sw)
        drawLine(color, Offset(0f, 0f), Offset(0f, s), sw)

        // Top Right Bracket
        drawLine(color, Offset(w - s, 0f), Offset(w, 0f), sw)
        drawLine(color, Offset(w, 0f), Offset(w, s), sw)

        // Bottom Left Bracket
        drawLine(color, Offset(0f, h), Offset(s, h), sw)
        drawLine(color, Offset(0f, h - s), Offset(0f, h), sw)

        // Bottom Right Bracket
        drawLine(color, Offset(w - s, h), Offset(w, h), sw)
        drawLine(color, Offset(w, h - s), Offset(w, h), sw)
    }
}

/** Clean White Enterprise Card Container untuk Dialog Izin & Progress Update */
@Composable
private fun LightEnterpriseCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = LightCardBg,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}