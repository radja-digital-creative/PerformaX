package id.biz.yuka.performax.ui.screen.home.salary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderOrange
import id.biz.yuka.performax.ui.theme.WonderGrey
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeRecapScreen(
    onBack: () -> Unit,
    viewModel: IncomeRecapViewModel = hiltViewModel()
) {
    if (viewModel.isInitialRoleChecking) {
        Box(modifier = Modifier.fillMaxSize().background(WonderBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WonderOrange)
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Penghasilan, 1: Potongan
    var showInfoDialog by remember { mutableStateOf(false) }

    // Colors
    val tealBackground = Color(0xFF38B29C)
    val limeTab = Color(0xFFD4E157)
    val redColor = Color(0xFFE57373)
    val greenColor = Color(0xFF4DB6AC)

    Scaffold(
        containerColor = Color(0xFFF9F9F9),
        topBar = {
            TopAppBar(
                title = { 
                    val title = if (viewModel.isOwner) "Pengeluaran Gaji" else "Rekap penghasilanmu"
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tealBackground)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

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
                // PERIODE
                item {
                    Text("Periode", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text(viewModel.periodString, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (viewModel.isOwner) {
                    // VIEW OWNER: REKAP AGREGAT & LIST KARYAWAN
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Pengeluaran Gaji (Berjalan)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(viewModel.takeHomePay.formatRupiah(), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Total Bruto", style = MaterialTheme.typography.labelSmall)
                                        Text(viewModel.totalIncome.formatRupiah(), fontWeight = FontWeight.Bold, color = greenColor)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.LightGray))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Total Potongan", style = MaterialTheme.typography.labelSmall)
                                        Text(viewModel.totalDeduction.formatRupiah(), fontWeight = FontWeight.Bold, color = redColor)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        Text(
                            text = "Daftar Gaji Karyawan",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(viewModel.employeeRecaps) { recap ->
                        EmployeeRecapCard(recap)
                    }
                } else {
                    // VIEW KARYAWAN (EXISTING)
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Rincian Penghasilan & Potongan", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // KARTU REKAP UTAMA & TAB (Karyawan)
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Pemasukan & Pengeluaran
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(greenColor))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pendapatan", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Text(viewModel.totalIncome.formatRupiah(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))

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

                                // Take Home Pay
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Take Home Pay Estimasi", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.HelpOutline,
                                            contentDescription = "Info",
                                            modifier = Modifier.size(16.dp).clickable { showInfoDialog = true },
                                            tint = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = viewModel.takeHomePay.formatRupiah(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Custom Pill Tab Bar
                                Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(50)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                        Box(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (selectedTab == 0) limeTab else Color.Transparent).clickable { selectedTab = 0 }.padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) { Text("Penghasilan", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                                        Box(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (selectedTab == 1) limeTab else Color.Transparent).clickable { selectedTab = 1 }.padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) { Text("Potongan", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // RINCIAN KATEGORI & DONUT CHART (Karyawan)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Rincian kategori", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(24.dp))

                            val activeList = if (selectedTab == 0) viewModel.incomeComponents else viewModel.deductionComponents
                            val activeTotal = if (selectedTab == 0) viewModel.totalIncome else viewModel.totalDeduction

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                DonutChart(data = activeList, total = activeTotal)
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            if (activeList.isEmpty() && selectedTab == 1) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada potongan (Potongan Rp 0)", color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    val activeList = if (selectedTab == 0) viewModel.incomeComponents else viewModel.deductionComponents
                    val activeTotal = if (selectedTab == 0) viewModel.totalIncome else viewModel.totalDeduction

                    items(activeList) { item ->
                        val percentage = if (activeTotal > 0) (item.amount / activeTotal) * 100 else 0.0
                        CategoryListItem(item = item, percentage = percentage)
                    }
                }
            }
        }
        // ... (ShowInfoDialog)
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Take Home Pay", fontWeight = FontWeight.Bold) },
                text = { Text("Ini adalah total perkiraan gaji (Gaji Pokok, Tunjangan, & Bonus) setelah dikurangi total potongan (Master, Alfa, & Sanksi Pelanggaran) yang dihitung proporsional hingga hari ini.") },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Paham", color = tealBackground, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

// =====================================
// KOMPONEN CUSTOM: DONUT CHART
// =====================================
@Composable
fun DonutChart(data: List<ChartItemData>, total: Double) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animationPlayed = false
        delay(100)
        animationPlayed = true
    }

    val animatedSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "sweep_anim"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val strokeWidth = 50.dp.toPx()

        if (data.isEmpty() || total <= 0.0) {
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = 0f,
                sweepAngle = 360f * animatedSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
        } else {
            var startAngle = -90f
            for (item in data) {
                val sweepAngle = ((item.amount / total) * 360f).toFloat() * animatedSweep
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }
    }
}

// =====================================
// KOMPONEN CUSTOM: LIST ITEM
// =====================================
@Composable
fun CategoryListItem(item: ChartItemData, percentage: Double) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(item.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(item.color))
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.amount.formatRupiah(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Text(String.format(Locale.US, "%.1f%%", percentage), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFFEEEEEE))
    }
}

@Composable
fun EmployeeRecapCard(item: EmployeeRecapItem) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(Color(0xFFF0F0F0))) {
                if (item.employee.profileUrl.isNotBlank()) {
                    AsyncImage(model = item.employee.profileUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.employee.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(item.employee.roleName, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Column {
                        Text("Bruto", fontSize = 9.sp, color = Color.Gray)
                        Text(item.income.formatRupiah(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Potongan", fontSize = 9.sp, color = Color.Gray)
                        Text(item.deduction.formatRupiah(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Netto", fontSize = 10.sp, color = Color.Gray)
                Text(item.net.formatRupiah(), fontWeight = FontWeight.Black, color = Color(0xFF38B29C))
            }
        }
    }
}

fun Double.formatRupiah(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(this).replace("Rp", "Rp ").substringBefore(",")
}