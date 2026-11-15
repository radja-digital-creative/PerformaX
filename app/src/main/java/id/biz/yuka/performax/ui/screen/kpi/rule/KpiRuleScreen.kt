package id.biz.yuka.performax.ui.screen.kpi.rule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Rule
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.KpiPointRuleEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpiRuleScreen(
    onBack: () -> Unit,
    viewModel: KpiRuleViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)

    // State untuk Bottom Sheet Edit
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedRule by remember { mutableStateOf<KpiPointRuleEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // LAYER 1: Background Header Statis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .drawBehind {
                    drawRect(color = tealHeaderColor, size = Size(size.width, size.height))
                    drawCircle(color = Color.White.copy(alpha = 0.2f), radius = 250.dp.toPx(), center = Offset(size.width * 1.1f, -50.dp.toPx()))
                    drawCircle(color = Color.White.copy(alpha = 0.1f), radius = 120.dp.toPx(), center = Offset(size.width * 0.9f, size.height * 0.4f))
                }
        )

        // LAYER 2: Scaffold Transparan
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadRules() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().imePadding()) {
                    item {
                        Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 16.dp))

                        // ROW 1: HEADER NAVIGATION
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.Black) }
                            Text("Aturan KPI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                            IconButton(onClick = { }) { Icon(Icons.Default.Rule, "Aturan", tint = Color.Transparent) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ROW 2: DESKRIPSI HALAMAN
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Poin Performa", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                            Text(
                                text = "Atur bobot poin yang akan didapatkan atau dikurangi dari karyawan berdasarkan aktivitas check-in dan check-out.",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray),
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // INFO KARTU (Menggantikan Tabs)
                        Card(
                            modifier = Modifier
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
                                Text("Total Parameter KPI", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${viewModel.kpiRules.size}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 28.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ROW 4: LIST ITEM (KARTU KONTEN)
                    item { Spacer(modifier = Modifier.fillMaxWidth().height(8.dp).background(backgroundColor)) }

                    if (viewModel.kpiRules.isEmpty() && !viewModel.isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("Aturan KPI belum tersedia.", color = WonderGrey)
                            }
                        }
                    } else {
                        items(viewModel.kpiRules) { rule ->
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                SimplifiedGrowthCard(
                                    rule = rule,
                                    onClick = {
                                        selectedRule = rule
                                        showEditSheet = true
                                    }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.fillMaxWidth().height(120.dp).background(backgroundColor)) }
                    item { Spacer(modifier = Modifier.fillMaxWidth().height(500.dp).background(backgroundColor)) }
                }
            }
        }
    }

    // BOTTOM SHEET UNTUK FORM EDIT
    if (showEditSheet && selectedRule != null) {
        KpiRuleEditBottomSheet(
            initialData = selectedRule!!,
            onDismiss = { showEditSheet = false },
            onSave = { newPoint, newDescription ->
                viewModel.updateRule(selectedRule!!.ruleCode, newPoint, newDescription)
                showEditSheet = false
            }
        )
    }
}

// ==========================================
// SIMPLIFIED GROWTH CARD UNTUK KPI RULE
// ==========================================
@Composable
fun SimplifiedGrowthCard(rule: KpiPointRuleEntity, onClick: () -> Unit) {
    // Penentuan Warna dan Label Kategori secara dinamis
    val isCheckIn = rule.ruleCode.startsWith("IN_")
    val isPenalty = rule.pointValue < 0

    val categoryColor = if (isCheckIn) Color(0xFFB2EBF2) else Color(0xFFE1BEE7) // Biru Muda untuk IN, Ungu Muda untuk OUT
    val badgeLabel = if (isCheckIn) "Check-In" else "Check-Out"
    val pointColor = if (isPenalty) Color(0xFFE57373) else Color(0xFF4DB6AC) // Merah jika minus, Hijau jika plus

    // Terjemahkan Rule Code agar lebih enak dibaca
    val readableTitle = when (rule.ruleCode) {
        "IN_EARLY" -> "Masuk Lebih Awal"
        "IN_ON_TIME" -> "Masuk Tepat Waktu"
        "IN_LATE" -> "Masuk Terlambat"
        "OUT_EARLY" -> "Pulang Lebih Awal"
        "OUT_ON_TIME" -> "Pulang Tepat Waktu"
        "OUT_OVERTIME" -> "Pulang Lembur"
        else -> rule.ruleCode
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            // Hiasan Bubble
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(100.dp)
                    .clip(RoundedCornerShape(topEnd = 16.dp))
            ) {
                Box(modifier = Modifier.size(80.dp).offset(x = 40.dp, y = (-20).dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.6f)))
                Box(modifier = Modifier.size(50.dp).offset(x = 10.dp, y = 20.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.8f)))
            }

            Column {
                // Label Kategori (Kiri Atas)
                Surface(
                    color = categoryColor,
                    shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    // Judul
                    Text(
                        text = readableTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Deskripsi (Kode Asli)
                    Text(
                        text = rule.description ?: "Kode Sistem: ${rule.ruleCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WonderGrey
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nilai Poin (Kanan Bawah)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (rule.pointValue > 0) "+${rule.pointValue} Poin" else "${rule.pointValue} Poin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = pointColor
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// BOTTOM SHEET UNTUK EDIT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpiRuleEditBottomSheet(
    initialData: KpiPointRuleEntity,
    onDismiss: () -> Unit,
    onSave: (newPoint: Int, newDescription: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Rule code tidak bisa diedit, hanya point dan deskripsi
    var pointValue by remember { mutableStateOf(initialData.pointValue.toString()) }
    var description by remember { mutableStateOf(initialData.description ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Text("Edit Poin: ${initialData.ruleCode}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pointValue,
                onValueChange = { pointValue = it },
                label = { Text("Nilai Poin (Gunakan minus (-) untuk penalti)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Keterangan Singkat") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val parsedPoint = pointValue.toIntOrNull() ?: 0
                    onSave(parsedPoint, description.ifBlank { null })
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
            }
        }
    }
}