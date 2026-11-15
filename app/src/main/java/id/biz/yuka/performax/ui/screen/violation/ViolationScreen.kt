package id.biz.yuka.performax.ui.screen.violation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.ViolationLogEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.utils.PermissionUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViolationScreen(
    onBack: () -> Unit,
    onNavigateToCreateManual: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ViolationViewModel = hiltViewModel(),
    appealViewModel: AppealViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)
    val activeTabColor = Color(0xFFE5DE44)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var selectedViolation by remember { mutableStateOf<ViolationLogEntity?>(null) }
    var pendingAction by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { appealViewModel.addSelectedImage(context, it) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        appealViewModel.handleCameraResult(context, bitmap)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            when (pendingAction) {
                "gallery" -> galleryLauncher.launch("*/*")
                "camera" -> cameraLauncher.launch()
            }
        } else {
            Toast.makeText(context, "Izin diperlukan untuk fitur lampiran.", Toast.LENGTH_SHORT).show()
        }
        pendingAction = ""
    }

    LaunchedEffect(appealViewModel.isOwner) {
        appealViewModel.loadActiveAppeals()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    Spacer(modifier = Modifier.height(topPadding + 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.Black) }
                        Text("Log Pelanggaran", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))

                        Row {
                            IconButton(onClick = onNavigateToHistory) {
                                Icon(Icons.Default.History, "Riwayat Banding", tint = Color.Black)
                            }
                            if (viewModel.isOwner) {
                                IconButton(onClick = onNavigateToCreateManual) {
                                    Icon(Icons.Default.PostAdd, "Tambah Manual", tint = Color.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
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

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.5f)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (selectedTab == 0) activeTabColor else Color.Transparent).clickable {
                                    selectedTab = 0
                                    appealViewModel.clearSelectedAppeal()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Pelanggaran", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal))
                            }
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (selectedTab == 1) activeTabColor else Color.Transparent).clickable {
                                    selectedTab = 1
                                    appealViewModel.loadActiveAppeals()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Banding", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
            bottomBar = {
                // Mendelegasikan Input Bar ke file komponen baru jika sedang dalam chat
                if (selectedTab == 1 && appealViewModel.selectedAppeal != null) {
                    AppealInputBar(
                        text = appealViewModel.commentText,
                        selectedImages = appealViewModel.selectedImages,
                        onTextChange = { appealViewModel.commentText = it },
                        onSend = { appealViewModel.sendComment(context) },
                        onRemoveImage = { appealViewModel.removeSelectedImage(it) },
                        onAttachGallery = {
                            if (PermissionUtils.hasGalleryPermission(context)) {
                                galleryLauncher.launch("*/*")
                            } else {
                                pendingAction = "gallery"
                                permissionLauncher.launch(PermissionUtils.GALLERY_PERMISSIONS)
                            }
                        },
                        onAttachCamera = {
                            if (PermissionUtils.hasCameraPermission(context)) {
                                cameraLauncher.launch()
                            } else {
                                pendingAction = "camera"
                                permissionLauncher.launch(arrayOf(PermissionUtils.CAMERA_PERMISSION))
                            }
                        },
                        isSending = appealViewModel.isSending
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (selectedTab == 0) {
                    PullToRefreshBox(
                        isRefreshing = viewModel.isLoading,
                        onRefresh = { viewModel.loadViolations() },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                                        Column {
                                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Power by AI HR Disciplinary Agent", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Total sanksi diproses bulan ini", fontSize = 11.sp, color = Color(0xFFD32F2F))
                                                    Text("${viewModel.violations.size} Kasus", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Black)
                                                }
                                                Icon(Icons.Default.Gavel, contentDescription = "AI", tint = Color(0xFFE57373), modifier = Modifier.size(48.dp))
                                            }
                                        }
                                    }

                                    Text("Riwayat Pelanggaran", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp))
                                    LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val filters = listOf("Semua", "Pending", "Processed")
                                        items(filters) { filterStr ->
                                            val isSelected = viewModel.selectedFilter == filterStr
                                            Surface(shape = CircleShape, color = if (isSelected) Color(0xFF6ED4C8) else Color.White, border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE0E0E0)), modifier = Modifier.clip(CircleShape).clickable { viewModel.selectedFilter = filterStr }) {
                                                Text(text = filterStr, color = if (isSelected) Color.Black else Color(0xFF424242), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontSize = 14.sp)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            if (viewModel.displayedViolations.isEmpty() && !viewModel.isLoading) {
                                item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Text("Tidak Ada Pelanggaran.", color = WonderGrey, fontWeight = FontWeight.Bold) } }
                            } else {
                                items(viewModel.displayedViolations) { violation ->
                                    ViolationGrowthCard(violation = violation, isOwner = viewModel.isOwner, onClick = { selectedViolation = violation; showDetailSheet = true })
                                }
                            }
                            item { Spacer(modifier = Modifier.height(120.dp)) }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
                        // Memanggil UI Tab Banding dari file terpisah
                        AppealTabContent(appealViewModel, pullToRefreshState)
                    }
                }
            }
        }
    }

    if (showDetailSheet && selectedViolation != null) {
        ViolationDetailBottomSheet(
            violation = selectedViolation!!,
            isOwner = viewModel.isOwner,
            onDismiss = { showDetailSheet = false },
            onProcessOrAppeal = {
                if (viewModel.isOwner) {
                    viewModel.processViolation(selectedViolation!!.id)
                } else {
                    viewModel.submitAppeal(
                        logId = selectedViolation!!.id,
                        onSuccess = {
                            selectedTab = 1
                            appealViewModel.loadActiveAppeals()
                        }
                    )
                }
                showDetailSheet = false
            }
        )
    }
}

@Composable
fun ViolationGrowthCard(violation: ViolationLogEntity, isOwner: Boolean, onClick: () -> Unit) {
    // Tentukan warna, warna teks, dan label berdasarkan status dari database
    val statusName = violation.status ?: "PENDING" // Asumsi fallback ke PENDING

    val (statusText, categoryColor, textColor) = when (statusName.uppercase()) {
        "APPEAL_PROCESS" -> Triple("Appeal Process", Color(0xFFBBDEFB), Color(0xFF0D47A1)) // Biru Muda
        "WIN_AN_APPEAL" -> Triple("Win an Appeal", Color(0xFFFFF9C4), Color(0xFFF57F17))   // Kuning Emas
        "PROCESSED" -> Triple("Processed (Payroll)", Color(0xFFC8E6C9), Color(0xFF1B5E20))   // Hijau
        else -> Triple("Pending", Color(0xFFFFCCBC), Color(0xFFBF360C))                      // Oranye (Default)
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
            // Ornamen Lingkaran di Pojok Kanan Atas
            Box(modifier = Modifier.align(Alignment.TopEnd).size(100.dp).clip(RoundedCornerShape(topEnd = 16.dp))) {
                Box(modifier = Modifier.size(80.dp).offset(x = 40.dp, y = (-20).dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.6f)))
                Box(modifier = Modifier.size(50.dp).offset(x = 10.dp, y = 20.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.8f)))
            }

            Column {
                // Badge Status
                Surface(color = categoryColor, shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = textColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Teks
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    if (isOwner) {
                        Text(
                            text = violation.userName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = violation.violationNotes ?: "Tercatat di sistem pada ${violation.violationDate.take(10)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isOwner) FontWeight.Normal else FontWeight.SemiBold,
                            color = if (isOwner) WonderGrey else Color(0xFF333333)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sanksi: ${violation.appliedPenalty}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViolationDetailBottomSheet(violation: ViolationLogEntity, isOwner: Boolean, onDismiss: () -> Unit, onProcessOrAppeal: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. Ambil status saat ini
    val statusName = violation.status?.uppercase() ?: "PENDING"

    // 2. Terjemahkan status untuk ditampilkan di UI
    val statusDisplay = when (statusName) {
        "APPEAL_PROCESS" -> "Sedang Banding (AI)"
        "WIN_AN_APPEAL" -> "Banding Diterima / Lolos"
        "PROCESSED" -> "Sudah Diproses (Payroll)"
        else -> "Menunggu Keputusan (Pending)"
    }

    // 3. Tentukan apakah tombol aksi boleh muncul
    val isPending = statusName == "PENDING"

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)) {
            Text("Detail Pelanggaran", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))

            if (isOwner) DetailRowItem("Karyawan", violation.userName)
            DetailRowItem("Tanggal Kejadian", violation.violationDate.take(10))
            DetailRowItem("Sanksi Dikenakan", violation.appliedPenalty)

            // Ubah label agar lebih dinamis sesuai status baru
            DetailRowItem("Status Saat Ini", statusDisplay)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Catatan/Bukti", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = violation.violationNotes ?: "Tidak ada catatan.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))

            // 4. KUNCI TOMBOL AKSI: Hanya muncul jika statusnya benar-benar PENDING
            if (isPending) {
                Button(onClick = onProcessOrAppeal, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C)), shape = RoundedCornerShape(12.dp)) {
                    Text(text = if (isOwner) "Proses Pelanggaran Ini" else "Ajukan Banding", fontWeight = FontWeight.Bold)
                }
            } else {
                // Jika sedang banding, sudah menang, atau sudah diproses, paksa tutup saja.
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Tutup", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DetailRowItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
    }
}