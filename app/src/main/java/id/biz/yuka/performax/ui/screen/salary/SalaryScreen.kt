package id.biz.yuka.performax.ui.screen.salary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.screen.home.salary.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(
    onBack: () -> Unit,
    viewModel: SalaryViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Tampilkan pesan PDF via Snackbar
    LaunchedEffect(viewModel.pdfMessage) {
        viewModel.pdfMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.pdfMessage = null
        }
    }

    // Colors
    val tealBackground = Color(0xFF38B29C)
    val limeTab = Color(0xFFD4E157)
    val redColor = Color(0xFFE57373)
    val greenColor = Color(0xFF4DB6AC)
    val thinLineColor = Color.Black.copy(alpha = 0.2f)

    Scaffold(
        containerColor = Color(0xFFF9F9F9),
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = viewModel.employeeName.ifBlank { "Slip Gaji Resmi" }
                    Text(titleText, fontWeight = FontWeight.Bold, color = Color.White) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White) 
                    }
                },
                actions = {
                    // Tombol Download Slip PDF
                    if (viewModel.isPdfGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.generateAndSavePdf(context) },
                            enabled = viewModel.selectedSalary != null && !viewModel.isLoading
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Download Slip PDF",
                                tint = if (viewModel.selectedSalary != null) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tealBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.loadMySalaries() },
            state = pullToRefreshState,

            modifier = Modifier.padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // BACKGROUND BUBBLES
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(tealBackground)
                        .drawBehind {
                            drawCircle(color = Color.White.copy(alpha = 0.08f), radius = 100.dp.toPx(), center = Offset(size.width * 0.1f, size.height * 0.2f))
                            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = 140.dp.toPx(), center = Offset(size.width * 0.9f, size.height * 0.8f))
                            drawCircle(color = Color.White.copy(alpha = 0.06f), radius = 50.dp.toPx(), center = Offset(size.width * 0.5f, size.height * -0.1f))
                        }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ✅ NAVIGASI BULAN & PERIODE
                    item {
                        if (viewModel.availableMonths.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = limeTab,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (viewModel.selectedMonthIndex < viewModel.availableMonths.size - 1) {
                                        IconButton(onClick = { viewModel.previousMonth() }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ChevronLeft, "Previous", tint = Color.Black)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(viewModel.selectedMonth, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text(viewModel.periodString, fontSize = 10.sp, color = Color.Black.copy(alpha = 0.7f))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))

                                    if (viewModel.selectedMonthIndex > 0) {
                                        IconButton(onClick = { viewModel.nextMonth() }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ChevronRight, "Next", tint = Color.Black)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        } else if (!viewModel.isLoading) {
                            Text("Tidak ada riwayat slip gaji", color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
                        }
                    }

                    // ✅ KARTU REKAP TAKE HOME PAY
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val isWeekly = viewModel.activePayrollType == id.biz.yuka.libperformx.entity.PayrollType.WEEKLY
                                    Surface(
                                        color = if (isWeekly) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (isWeekly) "Sistem Penggajian: Mingguan (Weekly)" else "Sistem Penggajian: Bulanan (Monthly)",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isWeekly) Color(0xFFE65100) else Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(greenColor))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pemasukan", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Text(viewModel.totalIncome.formatRupiah(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Box(modifier = Modifier.width(0.5.dp).height(40.dp).background(thinLineColor))

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(redColor))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Potongan", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Text(viewModel.totalDeduction.formatRupiah(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Take Home Pay Bersih", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = viewModel.finalTakeHomePay.formatRupiah(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "${viewModel.totalPresent} Hadir | ${viewModel.totalAlfa} Alfa | ${viewModel.totalSchedules} Jadwal",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // ✅ GRAFIK DAN RINCIAN
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Pemasukan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                SalaryDonutChart(data = viewModel.incomeComponents, total = viewModel.totalIncome, size = 110.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = thinLineColor)
                                if (viewModel.incomeComponents.isEmpty()) {
                                    Text("Tidak ada data", color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    viewModel.incomeComponents.forEach { MiniCategoryListItem(it) }
                                }
                            }

                            VerticalDivider(thickness = 0.5.dp, color = thinLineColor, modifier = Modifier.padding(horizontal = 4.dp))

                            Column(
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Potongan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                SalaryDonutChart(data = viewModel.deductionComponents, total = viewModel.totalDeduction, size = 110.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = thinLineColor)
                                if (viewModel.deductionComponents.isEmpty()) {
                                    Text("Tidak ada potongan", color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    viewModel.deductionComponents.forEach { MiniCategoryListItem(it) }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SalaryDonutChart(data: List<SalaryChartItemData>, total: Double, size: Dp = 110.dp) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animationPlayed = false
        kotlinx.coroutines.delay(100)
        animationPlayed = true
    }
    val animatedSweep by animateFloatAsState(targetValue = if (animationPlayed) 1f else 0f, animationSpec = tween(1000), label = "sweep")
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = (size.value * 0.25f).dp.toPx()
        if (data.isEmpty() || total <= 0.0) {
            drawArc(Color(0xFFE0E0E0), 0f, 360f * animatedSweep, false, style = Stroke(strokeWidth, cap = StrokeCap.Butt))
        } else {
            var startAngle = -90f
            for (item in data) {
                val sweepAngle = ((item.amount / total) * 360f).toFloat() * animatedSweep
                drawArc(item.color, startAngle, sweepAngle, false, style = Stroke(strokeWidth, cap = StrokeCap.Butt))
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun MiniCategoryListItem(item: SalaryChartItemData) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(item.color))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(item.name, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.amount.formatRupiah(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
