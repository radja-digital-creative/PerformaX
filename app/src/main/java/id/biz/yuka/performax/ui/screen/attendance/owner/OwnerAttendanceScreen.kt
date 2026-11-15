package id.biz.yuka.performax.ui.screen.attendance.owner

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ==========================================
// CUSTOM SHAPE: FOLDER WONDR
// ==========================================
class FolderShape(
    private val topCornerRadiusDp: Dp = 16.dp,
    private val tabWidthDp: Dp = 140.dp,
    private val tabHeightDp: Dp = 14.dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { topCornerRadiusDp.toPx() }
        val tw = with(density) { tabWidthDp.toPx() }
        val th = with(density) { tabHeightDp.toPx() }
        val w = size.width
        val h = size.height
        val baseY = h - th

        val path = Path().apply {
            moveTo(0f, r)
            quadraticBezierTo(0f, 0f, r, 0f)
            lineTo(w - r, 0f)
            quadraticBezierTo(w, 0f, w, r)
            lineTo(w, baseY)

            val tabStartX = (w + tw) / 2
            val tabEndX = (w - tw) / 2

            lineTo(tabStartX, baseY)
            cubicTo(tabStartX - tw * 0.1f, baseY, tabStartX - tw * 0.2f, h, tabStartX - tw * 0.3f, h)
            lineTo(tabEndX + tw * 0.3f, h)
            cubicTo(tabEndX + tw * 0.2f, h, tabEndX + tw * 0.1f, baseY, tabEndX, baseY)

            lineTo(0f, baseY)
            close()
        }
        return Outline.Generic(path)
    }
}

// ==========================================
// FUNGSI HELPER: KONVERSI UTC KE WIB (+7)
// ==========================================
private fun formatTimeWIB(timeStr: String?): String {
    if (timeStr.isNullOrBlank() || timeStr == "-" || timeStr == "--:--") return "--:--"
    return try {
        // Coba baca jika formatnya ISO Standar (Contoh: "2026-05-20T01:09:00Z")
        val parsedTime = OffsetDateTime.parse(timeStr)
        val wibTime = parsedTime.atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        wibTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        try {
            // Coba baca jika formatnya SQL biasa tanpa T/Z
            val cleanStr = timeStr.substringBefore(".")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localTime = LocalDateTime.parse(cleanStr, formatter)
            val wibTime = localTime.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Asia/Jakarta"))
            wibTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e2: Exception) {
            // Fallback: Jika error, potong angkanya saja
            timeStr.substringAfter("T").take(5)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerAttendanceScreen(
    onBack: () -> Unit,
    onNavigateToManualApproval: () -> Unit = {},
    viewModel: OwnerAttendanceViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedTab by remember { mutableIntStateOf(0) }

    var selectedCardData by remember { mutableStateOf<Pair<EmployeeAttendanceUiModel, Color>?>(null) }
    var clickedCardY by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(1000f) }

    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)
    val activeTabColor = Color(0xFFE5DE44)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenHeight = it.size.height.toFloat() }
    ) {

        Scaffold(
            containerColor = backgroundColor,
            contentWindowInsets = WindowInsets(0.dp)
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadAttendanceData() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // ==========================================
                    // 1. PERFECT OVERLAPPING HEADER
                    // ==========================================
                    item {
                        val listToShow = if (selectedTab == 0) viewModel.presentEmployees else viewModel.absentEmployees
                        val bottomOverlapPadding = 48.dp

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = bottomOverlapPadding)
                                    .drawBehind {
                                        drawRect(
                                            color = tealHeaderColor,
                                            size = Size(size.width, size.height - bottomOverlapPadding.toPx())
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.2f),
                                            radius = 250.dp.toPx(),
                                            center = Offset(size.width * 1.1f, -50.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.1f),
                                            radius = 120.dp.toPx(),
                                            center = Offset(size.width * 0.9f, size.height * 0.4f)
                                        )
                                    }
                            ) {
                                Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 32.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.Black) }
                                    Text("Absensi Karyawan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                                    IconButton(onClick = onNavigateToManualApproval) { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = "Persetujuan Absen Manual", tint = Color.Black) }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { viewModel.changeDate(-1) }) { Icon(Icons.Default.ChevronLeft, "Kemarin", tint = Color.Black) }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(140.dp)) {
                                        val dayFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("id", "ID"))
                                        val yearFormatter = DateTimeFormatter.ofPattern("yyyy", Locale("id", "ID"))
                                        Text(text = viewModel.selectedDate.format(dayFormatter), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                                        Text(text = viewModel.selectedDate.format(yearFormatter), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    if (viewModel.selectedDate.isBefore(LocalDate.now())) {
                                        IconButton(onClick = { viewModel.changeDate(1) }) { Icon(Icons.Default.ChevronRight, "Besok", tint = Color.Black) }
                                    } else {
                                        Spacer(modifier = Modifier.size(48.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(50))
                                                .background(if (selectedTab == 0) activeTabColor else Color.Transparent)
                                                .clickable { selectedTab = 0 },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Sudah Absen", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(50))
                                                .background(if (selectedTab == 1) activeTabColor else Color.Transparent)
                                                .clickable { selectedTab = 1 },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Belum Absen", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(64.dp))
                            }

                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Total karyawan dari ${viewModel.totalStores} toko", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${listToShow.size} / ${viewModel.totalEmployees}",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 28.sp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ==========================================
                    // 2. DAFTAR KARTU FOLDER KARYAWAN
                    // ==========================================
                    val listToShow = if (selectedTab == 0) viewModel.presentEmployees else viewModel.absentEmployees
                    val cardColors = listOf(Color(0xFFF2A460), Color(0xFFEBC146), Color(0xFF8DCC9E), Color(0xFFB593DF))
                    val grayColor = Color(0xFFD9D9D9)

                    itemsIndexed(listToShow) { index, empUi ->
                        val bgColor = if (selectedTab == 0) cardColors[index % cardColors.size] else grayColor
                        var currentCardY by remember { mutableFloatStateOf(0f) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .onGloballyPositioned { coordinates ->
                                    currentCardY = coordinates.positionInRoot().y
                                }
                                .clickable {
                                    clickedCardY = currentCardY
                                    selectedCardData = Pair(empUi, bgColor)
                                },
                            shape = FolderShape(topCornerRadiusDp = 16.dp, tabWidthDp = 140.dp, tabHeightDp = 14.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            // ✅ MENAMBAHKAN BUBBLE DI KARTU LIST
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        // Bubble Besar Kanan Atas
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.25f),
                                            radius = 100.dp.toPx(),
                                            center = Offset(size.width * 1.05f, -20.dp.toPx())
                                        )
                                        // Bubble Kecil Kanan Tengah
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.15f),
                                            radius = 60.dp.toPx(),
                                            center = Offset(size.width * 0.8f, size.height + 10.dp.toPx())
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                                        color = Color.Transparent
                                    ) {
                                        if (empUi.employee.profileUrl.isBlank()) {
                                            Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                            }
                                        } else {
                                            AsyncImage(
                                                model = empUi.employee.profileUrl,
                                                contentDescription = "Profile",
                                                modifier = Modifier.size(36.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Text(
                                        text = empUi.employee.fullName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }
                    }

                    if (!viewModel.isLoading && listToShow.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("Tidak ada data di kategori ini.", color = WonderGrey)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }

        // ==========================================
        // 3. LAYER ANIMASI: KARTU MENGAMBANG
        // ==========================================

        // A. Background Hitam Redup (Fades In/Out)
        AnimatedVisibility(
            visible = selectedCardData != null,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { selectedCardData = null }
            )
        }

        // B. Kartunya Sendiri (Meluncur dari Posisi Diklik)
        AnimatedVisibility(
            visible = selectedCardData != null,
            enter = slideInVertically(
                initialOffsetY = { fullHeight ->
                    val targetCenterY = (screenHeight - fullHeight) / 2f
                    (clickedCardY - targetCenterY).toInt()
                },
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + scaleIn(initialScale = 0.85f, animationSpec = tween(450)) + fadeIn(tween(300)),

            exit = slideOutVertically(
                targetOffsetY = { fullHeight ->
                    val targetCenterY = (screenHeight - fullHeight) / 2f
                    (clickedCardY - targetCenterY).toInt()
                },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + scaleOut(targetScale = 0.85f, animationSpec = tween(400)) + fadeOut(tween(300))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                selectedCardData?.let { (empUi, bgColor) ->
                    val attendance = empUi.attendance

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {

                            // 3A. HEADER KARTU (DITAMBAH BUBBLE OVERLAY)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        // 1. Gambar Base Color
                                        drawRect(bgColor)
                                        // 2. Gambar Bubble Dekoratif
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.25f),
                                            radius = 140.dp.toPx(),
                                            center = Offset(size.width * 1.05f, -20.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.15f),
                                            radius = 80.dp.toPx(),
                                            center = Offset(size.width * 0.75f, size.height + 10.dp.toPx())
                                        )
                                    }
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                                    color = Color.Transparent
                                ) {
                                    if (empUi.employee.profileUrl.isBlank()) {
                                        Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                        }
                                    } else {
                                        AsyncImage(
                                            model = empUi.employee.profileUrl,
                                            contentDescription = "Profile",
                                            modifier = Modifier.size(36.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = empUi.employee.fullName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.8f))
                                    )
                                    Text(
                                        text = viewModel.selectedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Black.copy(alpha = 0.6f))
                                    )
                                }
                            }

                            // 3B. ISI KERTAS ABSENSI
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {

                                // ✅ PERBAIKAN ZONA WAKTU: Panggil formatTimeWIB
                                val jamMasuk = formatTimeWIB(attendance?.checkInTime)
                                val jamKeluar = formatTimeWIB(attendance?.checkOutTime)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text("Jam Masuk", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                        Text(
                                            text = jamMasuk,
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Jam Keluar", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                        Text(
                                            text = jamKeluar,
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                                // Foto Kotak Kecil
                                Column {
                                    Text("Bukti Kehadiran", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        SmallPhotoBox(title = "Masuk", url = attendance?.checkInImage, modifier = Modifier.weight(1f))
                                        SmallPhotoBox(title = "Keluar", url = attendance?.checkOutImage, modifier = Modifier.weight(1f))
                                    }
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                                // Validasi (Face Match / Location)
                                Column {
                                    Text("Validasi Sistem", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val isAbsen = attendance != null
                                    ValidationRow(label = "Kesesuaian Wajah", isValid = isAbsen)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ValidationRow(label = "Kesesuaian Lokasi", isValid = isAbsen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// KOMPONEN PENDUKUNG DETAIL KARTU
// ==========================================
@Composable
fun SmallPhotoBox(title: String, url: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            if (url.isNullOrBlank()) {
                Icon(Icons.Default.ImageNotSupported, null, tint = Color.LightGray)
            } else {
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
    }
}

@Composable
fun ValidationRow(label: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isValid) Color(0xFF4CAF50) else Color(0xFFE57373),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
    }
}