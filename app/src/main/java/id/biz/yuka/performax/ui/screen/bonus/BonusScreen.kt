package id.biz.yuka.performax.ui.screen.bonus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.BonusEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusScreen(
    onBack: () -> Unit,
    onNavigateToCreateManual: () -> Unit, // Untuk Owner jika ingin tambah bonus manual
    viewModel: BonusViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)

    // Bottom Sheet State
    var showDetailSheet by remember { mutableStateOf(false) }
    var selectedBonus by remember { mutableStateOf<BonusEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // LAYER 1: Background Header Statis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .drawBehind {
                    drawRect(color = tealHeaderColor, size = Size(size.width, size.height))
                    drawCircle(color = Color.White.copy(alpha = 0.2f), radius = 250.dp.toPx(), center = Offset(size.width * 1.1f, -50.dp.toPx()))
                    drawCircle(color = Color.White.copy(alpha = 0.1f), radius = 120.dp.toPx(), center = Offset(size.width * 0.9f, size.height * 0.4f))
                }
        )

        // LAYER 2: Scaffold Utama
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadBonuses() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    item {
                        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                        Spacer(modifier = Modifier.height(topPadding + 16.dp))

                        // ROW 1: HEADER NAVIGATION
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.Black) }
                            Text("Bonus & Reward", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))

                            if (viewModel.isOwner) {
                                IconButton(onClick = onNavigateToCreateManual) {
                                    Icon(Icons.Default.AddCircleOutline, "Tambah Bonus", tint = Color.Black)
                                }
                            } else {
                                IconButton(onClick = { }) { Icon(Icons.Default.Description, "", tint = Color.Transparent) }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ROW 2: NAVIGASI BULAN
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, "Bulan Lalu", tint = Color.Black) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(160.dp)) {
                                Text(viewModel.currentMonthStr, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                            }
                            if (viewModel.selectedDate.withDayOfMonth(1).isBefore(LocalDate.now().withDayOfMonth(1))) {
                                IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, "Bulan Depan", tint = Color.Black) }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // KONTEN LIST
                    item {
                        Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // BANNER SUMMARY CARD (Tema Emas/Hijau)
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)), // Hijau Muda
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column {
                                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(if (viewModel.isOwner) "Total Pencairan Bonus" else "Total Bonus Kamu", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(formatRupiah(viewModel.totalBonusAmount), fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.Black)
                                            }
                                            Box(modifier = Modifier.size(56.dp).background(Color(0xFFC8E6C9), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.CardGiftcard, contentDescription = "Bonus", tint = Color(0xFF388E3C), modifier = Modifier.size(32.dp))
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)) {
                                            Text("Bonus akan otomatis diakumulasikan ke dalam sistem Payroll.", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                Text("Riwayat Bonus", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp))

                                // FILTER CHIPS
                                LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val filters = listOf("Semua", "Pending", "Cair")
                                    items(filters) { filterStr ->
                                        val isSelected = viewModel.selectedFilter == filterStr
                                        Surface(shape = CircleShape, color = if (isSelected) WonderOrange else Color.White, border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE0E0E0)), modifier = Modifier.clip(CircleShape).clickable { viewModel.selectedFilter = filterStr }) {
                                            Text(text = filterStr, color = if (isSelected) Color.White else Color(0xFF424242), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontSize = 14.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    if (viewModel.displayedBonuses.isEmpty() && !viewModel.isLoading) {
                        item { Box(modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(48.dp), contentAlignment = Alignment.Center) { Text("Tidak Ada Bonus Bulan Ini.", color = WonderGrey, fontWeight = FontWeight.Bold) } }
                    } else {
                        items(viewModel.displayedBonuses) { bonus ->
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                BonusCard(bonus = bonus, isOwner = viewModel.isOwner, onClick = { selectedBonus = bonus; showDetailSheet = true })
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(120.dp).fillMaxWidth().background(backgroundColor)) }
                }
            }
        }
    }

    if (showDetailSheet && selectedBonus != null) {
        BonusDetailBottomSheet(
            bonus = selectedBonus!!,
            isOwner = viewModel.isOwner,
            onDismiss = { showDetailSheet = false },
            onProcess = {
                viewModel.processBonus(selectedBonus!!.id)
                showDetailSheet = false
            }
        )
    }
}

// ==========================================
// CARD: BONUS CARD UI
// ==========================================
@Composable
fun BonusCard(bonus: BonusEntity, isOwner: Boolean, onClick: () -> Unit) {
    val isProcessed = bonus.isProcessed
    val categoryColor = if (isProcessed) Color(0xFFC8E6C9) else Color(0xFFFFF59D) // Hijau Cair, Kuning Pending
    val statusText = if (isProcessed) "Cair (Payroll)" else "Pending"

    Card(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() },
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.align(Alignment.TopEnd).size(100.dp).clip(RoundedCornerShape(topEnd = 16.dp))) {
                Box(modifier = Modifier.size(80.dp).offset(x = 40.dp, y = (-20).dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.5f)))
                Box(modifier = Modifier.size(50.dp).offset(x = 10.dp, y = 20.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.8f)))
            }

            Column {
                Surface(color = categoryColor, shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    if (isOwner) {
                        Text(text = bonus.userName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = bonus.notes,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isOwner) FontWeight.Normal else FontWeight.SemiBold, color = if (isOwner) WonderGrey else Color(0xFF333333)),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "+ ${formatRupiah(bonus.amount)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF2E7D32)) // Warna Hijau Uang
                    }
                }
            }
        }
    }
}

// ==========================================
// BOTTOM SHEET DETAIL BONUS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusDetailBottomSheet(bonus: BonusEntity, isOwner: Boolean, onDismiss: () -> Unit, onProcess: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)) {
            Text("Detail Bonus & Reward", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))

            if (isOwner) DetailBonusRowItem("Penerima", bonus.userName)
            DetailBonusRowItem("Tanggal Input", bonus.bonusDate)
            DetailBonusRowItem("Nominal Bonus", formatRupiah(bonus.amount), isAmount = true)
            DetailBonusRowItem("Status Pencairan", if (bonus.isProcessed) "Sudah Masuk Payroll" else "Menunggu Diproses")

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Catatan / Keterangan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = bonus.notes, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))

            if (isOwner && !bonus.isProcessed) {
                Button(onClick = onProcess, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C)), shape = RoundedCornerShape(12.dp)) {
                    Text(text = "Proses Pencairan (Masuk Payroll)", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Tutup", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DetailBonusRowItem(label: String, value: String, isAmount: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = if (isAmount) FontWeight.Black else FontWeight.Bold, fontSize = if (isAmount) 16.sp else 14.sp, color = if (isAmount) Color(0xFF2E7D32) else Color.Black)
    }
}

// Utility Formatter Rupiah
fun formatRupiah(amount: Double): String {
    val localeID = Locale("in", "ID")
    val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
    return formatRupiah.format(amount).replace(",00", "")
}