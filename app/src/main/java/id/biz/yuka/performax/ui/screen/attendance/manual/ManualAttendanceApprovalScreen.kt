package id.biz.yuka.performax.ui.screen.attendance.manual

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.libperformx.entity.ManualAttendanceEntity
import id.biz.yuka.performax.ui.components.GlobalErrorPopup
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAttendanceApprovalScreen(
    onBack: () -> Unit,
    viewModel: ManualAttendanceApprovalViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedImageForFullscreen by remember { mutableStateOf<String?>(null) }
    var itemToRespond by remember { mutableStateOf<Pair<ManualAttendanceEntity, Boolean>?>(null) }
    var responseReasonText by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.successMessage = null
        }
    }

    Scaffold(
        containerColor = WonderBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Persetujuan Absen Manual", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // TabRow Filter Status
            TabRow(
                selectedTabIndex = viewModel.selectedTabIndex,
                containerColor = WonderBg,
                contentColor = WonderOrange,
                indicator = { tabPositions ->
                    if (viewModel.selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedTabIndex]),
                            color = WonderOrange
                        )
                    }
                },
                divider = {}
            ) {
                Tab(
                    selected = viewModel.selectedTabIndex == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Pending", fontWeight = if (viewModel.selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = viewModel.selectedTabIndex == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Disetujui", fontWeight = if (viewModel.selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = viewModel.selectedTabIndex == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Ditolak", fontWeight = if (viewModel.selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WonderOrange)
                }
            } else if (viewModel.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = WonderGrey, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada pengajuan absen manual.", color = WonderGrey)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.items, key = { it.id }) { item ->
                        ManualAttendanceCard(
                            item = item,
                            onImageClick = { selectedImageForFullscreen = it },
                            onApprove = { itemToRespond = Pair(item, true) },
                            onReject = { itemToRespond = Pair(item, false) }
                        )
                    }
                }
            }
        }
    }

    // Modal Fullscreen Image Viewer
    if (selectedImageForFullscreen != null) {
        Dialog(
            onDismissRequest = { selectedImageForFullscreen = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = selectedImageForFullscreen,
                    contentDescription = "Foto Bukti Absen",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { selectedImageForFullscreen = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
        }
    }

    // Dialog Alasan Approval/Rejection
    if (itemToRespond != null) {
        val (targetItem, isApproveAction) = itemToRespond!!

        AlertDialog(
            onDismissRequest = {
                if (!viewModel.isProcessing) {
                    itemToRespond = null
                    responseReasonText = ""
                }
            },
            title = {
                Text(
                    if (isApproveAction) "Setujui Absen Manual?" else "Tolak Absen Manual?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Karyawan: ${targetItem.userName}\nTanggal: ${targetItem.date}",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = responseReasonText,
                        onValueChange = { responseReasonText = it },
                        label = { Text("Catatan / Alasan (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentPair = itemToRespond
                        if (currentPair != null) {
                            viewModel.respondToRequest(
                                id = targetItem.id,
                                isApproved = isApproveAction,
                                reason = responseReasonText
                            )
                            itemToRespond = null
                            responseReasonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApproveAction) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    ),
                    enabled = !viewModel.isProcessing
                ) {
                    if (viewModel.isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isApproveAction) "Setujui" else "Tolak", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        itemToRespond = null
                        responseReasonText = ""
                    },
                    enabled = !viewModel.isProcessing
                ) {
                    Text("Batal")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    GlobalErrorPopup(
        errorMessage = viewModel.errorMessage,
        onDismiss = { viewModel.errorMessage = null }
    )
}

@Composable
private fun ManualAttendanceCard(
    item: ManualAttendanceEntity,
    onImageClick: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Foto Profil, Nama, Toko, Tanggal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(WonderBg)
                ) {
                    if (item.profileUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.profileUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(item.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${item.storeName} • ${item.date}", fontSize = 12.sp, color = WonderGrey)
                }

                // Status Badge
                val (badgeBg, badgeTextColor, badgeText) = when (item.approvalStatus) {
                    "approved" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Disetujui")
                    "rejected" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), "Ditolak")
                    else -> Triple(Color(0xFFFFF3E0), WonderOrange, "Pending")
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

            // Seksi Check-In (jika ada)
            if (!item.checkInTime.isNullOrBlank() || !item.checkInImage.isNullOrBlank()) {
                ManualAttendanceSection(
                    title = "Absen Masuk Manual",
                    time = item.checkInTime ?: "-",
                    status = item.checkInStatus ?: "-",
                    lat = item.checkInLatitude,
                    lng = item.checkInLongitude,
                    imageUrl = item.checkInImage,
                    onImageClick = onImageClick
                )
            }

            // Seksi Check-Out (jika ada)
            if (!item.checkOutTime.isNullOrBlank() || !item.checkOutImage.isNullOrBlank()) {
                if (!item.checkInTime.isNullOrBlank() || !item.checkInImage.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF0F0F0))
                }
                ManualAttendanceSection(
                    title = "Absen Pulang Manual",
                    time = item.checkOutTime ?: "-",
                    status = item.checkOutStatus ?: "-",
                    lat = item.checkOutLatitude,
                    lng = item.checkOutLongitude,
                    imageUrl = item.checkOutImage,
                    onImageClick = onImageClick
                )
            }

            // Catatan Alasan (jika ada)
            val reason = item.approvalReason
            if (!reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Catatan Manager:", fontSize = 10.sp, color = WonderGrey, fontWeight = FontWeight.Bold)
                        Text(reason, fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }

            // Tombol Aksi (hanya jika pending)
            if (item.approvalStatus == "pending") {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tolak", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Setujui", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualAttendanceSection(
    title: String,
    time: String,
    status: String,
    lat: Double?,
    lng: Double?,
    imageUrl: String?,
    onImageClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WonderOrange)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Waktu: $time ($status)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (lat != null && lng != null) {
                Text(
                    "GPS: ${String.format(java.util.Locale.US, "%.5f", lat)}, ${String.format(java.util.Locale.US, "%.5f", lng)}",
                    fontSize = 11.sp,
                    color = WonderGrey
                )
            }
        }

        if (!imageUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .clickable { onImageClick(imageUrl) }
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Bukti Foto",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
