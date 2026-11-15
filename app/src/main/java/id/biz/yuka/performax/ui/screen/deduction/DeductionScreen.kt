package id.biz.yuka.performax.ui.screen.deduction

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.DeductionEntity
import id.biz.yuka.libperformx.request.CreateDeductionRequest
import id.biz.yuka.libperformx.request.UpdateDeductionRequest
import id.biz.yuka.performax.ui.theme.WonderGrey
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeductionScreen(
    onBack: () -> Unit,
    viewModel: DeductionViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    // WARNA TEMA
    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)
    val activeTabColor = Color(0xFFE5DE44)

    // STATE
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Daftar, 1 = Buat Baru
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedDeduction by remember { mutableStateOf<DeductionEntity?>(null) }

    // [ANALISIS: STRUKTUR LAYER SEPERTI OWNER ATTENDANCE]
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // [LAYER 1: BACKGROUND HEADER STATIS DI BELAKANG]
        // Ini memastikan warna teal & bubble SELALU tembus ke atas Status Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp) // Ketinggian statis menutupi bagian atas layar
                .drawBehind {
                    drawRect(
                        color = tealHeaderColor,
                        size = Size(size.width, size.height)
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
        )

        // [LAYER 2: KONTEN YANG BISA DI-SCROLL & BERLATAR BELAKANG TRANSPARAN]
        Scaffold(
            containerColor = Color.Transparent, // [PENTING] Harus transparan agar Layer 1 terlihat
            contentWindowInsets = WindowInsets(0.dp) // [PENTING] Matikan Inset agar Scaffold menabrak batas layar
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadDeductions() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {

                    // ==========================================
                    // 1. HEADER KONTEN (Tanpa drawBehind)
                    // ==========================================
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // [PENTING] Padding ini mendorong teks ke bawah Status Bar
                            Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 16.dp))

                            // Header Bar
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.Black) }
                                Text("Deduction", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                                IconButton(onClick = { /* Menu Tambahan */ }) { Icon(Icons.Default.Description, "Log", tint = Color.Transparent) } // Spacer
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Catatan Teks
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Potongan Finansial",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                )
                                Text(
                                    text = "Ini merupakan aturan potongan finasial yang dibebankan ke karyawan setiap bulannnya. Bukan untuk aturan potongan yang disebabkan oleh pelanggaran.",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray),
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // TABS: Daftar vs Buat Baru
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
                                        Text("Daftar Aturan", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal))
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
                                        Text("Buat Baru", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp)) // Jarak bawah header
                        }
                    }

                    // ==========================================
                    // 2. KONTEN BERDASARKAN TAB
                    // ==========================================

                    // [LAYER 3: BACKGROUND ABU-ABU UNTUK KONTEN LIST]
                    // Karena Scaffold sekarang transparan, kita buat "Background Palsu" untuk item-item di bawah header
                    item {
                        // Tidak ada konten, tapi item ini bisa dipakai sebagai pembatas visual jika diperlukan
                    }

                    if (selectedTab == 0) {
                        // TAB DAFTAR
                        if (viewModel.deductions.isEmpty() && !viewModel.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor) // Berikan warna abu-abu
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Belum ada aturan potongan dibuat.", color = WonderGrey)
                                }
                            }
                        } else {
                            item {
                                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp).background(backgroundColor))
                            }
                            items(viewModel.deductions) { deduction ->
                                Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                    DeductionGrowthCard(
                                        deduction = deduction,
                                        onClick = {
                                            selectedDeduction = deduction
                                            showEditSheet = true
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // TAB BUAT BARU
                        item {
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                CreateDeductionForm(
                                    onSave = { request ->
                                        viewModel.addDeduction(request)
                                        selectedTab = 0
                                    }
                                )
                            }
                        }
                    }

                    // SPACER BAWAH
                    item {
                        Spacer(modifier = Modifier.fillMaxWidth().height(120.dp).background(backgroundColor))
                    }

                    // EXTRA PADDING UNTUK MENUTUP RUANG KOSONG DI BAWAH LAZYCOLUMN
                    item {
                        Spacer(modifier = Modifier.fillMaxWidth().height(500.dp).background(backgroundColor))
                    }
                }
            }
        }
    }

    // ==========================================
    // 3. BOTTOM SHEET EDIT
    // ==========================================
    if (showEditSheet && selectedDeduction != null) {
        DeductionEditBottomSheet(
            initialData = selectedDeduction!!,
            onDismiss = { showEditSheet = false },
            onSave = { request ->
                viewModel.updateDeduction(selectedDeduction!!.id, request)
                showEditSheet = false
            },
            onDelete = {
                viewModel.deleteDeduction(selectedDeduction!!.id)
                showEditSheet = false
            }
        )
    }
}

// ==========================================
// CARD: GAYA GROWTH TASK DENGAN BADGE YANG DISEMPURNAKAN
// ==========================================
@Composable
fun DeductionGrowthCard(deduction: DeductionEntity, onClick: () -> Unit) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatter.maximumFractionDigits = 0

    val isFixed = deduction.calculationType == "fixed"
    val categoryColor = if (isFixed) Color(0xFFFFCCBC) else Color(0xFFC8E6C9)

    // Teks Kategori (Pojok Kiri Atas)
    val badgeLabel = if (isFixed) "Fixed (Rp)" else "Persentase (%)"

    // Nominal tanpa tanda minus
    val amountText = if (isFixed) {
        formatter.format(deduction.fixedAmount)
    } else {
        "${deduction.percentageValue}%"
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

            // Hiasan Pojok Kanan Atas (Bubble Transparan)
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
                // TAG KATEGORI (Pojok Kiri Atas)
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
                    // JUDUL UTAMA (Tanpa badge di sebelahnya)
                    Text(
                        text = deduction.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // DESKRIPSI
                    Text(
                        text = deduction.description ?: "Tidak ada deskripsi rinci.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WonderGrey,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // NOMINAL (Di Kanan Bawah, Kecil, Hitam, Tanpa Minus)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = amountText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// KOMPONEN FORM BUAT BARU DI DALAM TAB
// ==========================================
@Composable
fun CreateDeductionForm(onSave: (CreateDeductionRequest) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var calculationType by remember { mutableStateOf("fixed") }
    var amountInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp)) {
        Text("Rincian Aturan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Potongan (Cth: BPJS, Asuransi, dll)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Deskripsi (Opsional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Metode Potongan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = calculationType == "fixed",
                onClick = { calculationType = "fixed"; amountInput = "" },
                label = { Text("Rupiah (Fixed)") }
            )
            FilterChip(
                selected = calculationType == "percentage",
                onClick = { calculationType = "percentage"; amountInput = "" },
                label = { Text("Persentase (%)") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text(if (calculationType == "fixed") "Nominal (Rp)" else "Persentase (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && parsedAmount >= 0) {
                    onSave(
                        CreateDeductionRequest(
                            name = name,
                            description = description.ifBlank { null },
                            calculation_type = calculationType,
                            fixed_amount = if (calculationType == "fixed") parsedAmount else 0.0,
                            percentage_value = if (calculationType == "percentage") parsedAmount else 0.0
                        )
                    )
                    // Reset Form
                    name = ""; description = ""; amountInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C))
        ) {
            Text("Simpan Aturan Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ==========================================
// BOTTOM SHEET EDIT KETIKA KARTU DI KLIK
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeductionEditBottomSheet(
    initialData: DeductionEntity,
    onDismiss: () -> Unit,
    onSave: (UpdateDeductionRequest) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialData.name) }
    var description by remember { mutableStateOf(initialData.description ?: "") }
    var calculationType by remember { mutableStateOf(initialData.calculationType) }
    var amountInput by remember {
        mutableStateOf(if (initialData.calculationType == "percentage") initialData.percentageValue.toString() else initialData.fixedAmount.toLong().toString())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Menghindari tombol tersendat navigasi bawah
                .imePadding() // Menyesuaikan agar naik saat keyboard aktif
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Text("Edit Aturan Potongan", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Metode Potongan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                FilterChip(selected = calculationType == "fixed", onClick = { calculationType = "fixed"; amountInput = "" }, label = { Text("Rupiah (Fixed)") })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = calculationType == "percentage", onClick = { calculationType = "percentage"; amountInput = "" }, label = { Text("Persentase (%)") })
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Nilai") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Delete di Pojok Kiri Bawah BottomSheet
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFE57373))
                }

                // Tombol Simpan
                Button(
                    onClick = {
                        val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
                        onSave(UpdateDeductionRequest(
                            name = name,
                            description = description.ifBlank { null },
                            calculation_type = calculationType,
                            fixed_amount = if (calculationType == "fixed") parsedAmount else 0.0,
                            percentage_value = if (calculationType == "percentage") parsedAmount else 0.0
                        ))
                    },
                    modifier = Modifier.weight(1f).padding(start = 16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}