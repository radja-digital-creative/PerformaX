package id.biz.yuka.performax.ui.screen.performa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.DetailPerformaEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderLime
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformaScreen(
    onNavigateBack: () -> Unit,
    viewModel: PerformaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Palet Warna Header (Meniru OwnerAttendance)
    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)

    Scaffold(
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PerformaUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WonderOrange)
                    }
                }
                is PerformaUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message, color = WonderGrey, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val now = java.time.LocalDate.now()
                                viewModel.fetchPerformanceDetail(now.monthValue, now.year)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                is PerformaUiState.Success -> {
                    PerformaDashboardContent(
                        data = state.data,
                        tealHeaderColor = tealHeaderColor,
                        onBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
fun PerformaDashboardContent(
    data: DetailPerformaEntity,
    tealHeaderColor: Color,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ==========================================
        // 1. HEADER OVERLAP DENGAN KARTU KPI
        // ==========================================
        item {
            val bottomOverlapPadding = 48.dp

            Box(modifier = Modifier.fillMaxWidth()) {

                // Background Header (Teal + Bubbles)
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
                    Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.Black) }
                        Text("Rincian Performa", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                        // Placeholder agar title di tengah
                        IconButton(onClick = { }, enabled = false) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Transparent) }
                    }

                    Spacer(modifier = Modifier.height(84.dp)) // Jarak agar kartu overlap naik dengan pas
                }

                // Kartu KPI Hitam (Overlap)
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Skor KPI Saat Ini", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${data.kpi.currentScore}", color = WonderLime, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black))
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val isGrowthUp = data.kpi.growthPercentage >= 0
                            Surface(
                                color = if (isGrowthUp) WonderLime.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (isGrowthUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown, null, tint = if (isGrowthUp) WonderLime else Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${data.kpi.growthPercentage}%", color = if (isGrowthUp) WonderLime else Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bulan lalu: ${data.kpi.lastMonthScore}", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // ==========================================
        // 2. KARTU ANALITIK KEHADIRAN (DENGAN DONUT CHART)
        // ==========================================
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SectionTitle("Analitik Kehadiran")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {

                        // Menghitung Data Chart
                        val onTime = data.attendance.onTime
                        val late = data.attendance.late
                        val earlyLeave = data.attendance.earlyLeave
                        val totalRecorded = onTime + late + earlyLeave

                        // Asumsi Hari Kerja per Bulan = 22 hari jika total belum mencapai
                        val assumedTotalDays = if (totalRecorded > 0) totalRecorded else 22
                        val absent = assumedTotalDays - totalRecorded

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Donut Chart
                            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                                DonutChart(
                                    values = listOf(onTime.toFloat(), late.toFloat(), earlyLeave.toFloat(), absent.toFloat()),
                                    colors = listOf(WonderLime, Color.Red, WonderOrange, Color.LightGray)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalRecorded", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                    Text("Total", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                }
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            // Legend
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LegendItem(color = WonderLime, title = "Tepat Waktu", value = "$onTime")
                                LegendItem(color = Color.Red, title = "Terlambat", value = "$late")
                                LegendItem(color = WonderOrange, title = "Pulang Cepat", value = "$earlyLeave")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

                        // Ekstra Info (Lembur)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NightlightRound, null, tint = Color(0xFF6D81FA), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Total Lembur", style = MaterialTheme.typography.labelMedium, color = WonderGrey, modifier = Modifier.weight(1f))
                            Text("${data.attendance.overtimeDays} Hari", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // ==========================================
        // 3. KARTU TUGAS & IZIN (GRID)
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kolom Tugas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Tugas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WonderGrey)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("${data.tasks.completedOnTime}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = WonderLime)
                        Text("Tepat Waktu", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${data.tasks.lateSubmission} Terlambat", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                    }
                }

                // Kolom Izin & Interaksi
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Izin / Sakit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = WonderGrey)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${data.leaves} Hari", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WonderOrange)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Interaksi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = WonderGrey)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${data.tasks.interactionScore}/100", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF6D81FA))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

// ==========================================
// KOMPONEN PENDUKUNG
// ==========================================

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun LegendItem(color: Color, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = WonderGrey, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun DonutChart(values: List<Float>, colors: List<Color>) {
    val total = values.sum().takeIf { it > 0 } ?: 1f // Hindari bagi nol
    val proportions = values.map { it / total }

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        val strokeWidth = 16.dp.toPx()

        proportions.forEachIndexed { index, proportion ->
            val sweepAngle = proportion * 360f

            // Gambar hanya jika sweepAngle > 0
            if (sweepAngle > 0) {
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false, // false untuk Donut, true untuk Pie
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Beri jarak kecil antar segmen agar terlihat elegan
                startAngle += sweepAngle
            }
        }
    }
}