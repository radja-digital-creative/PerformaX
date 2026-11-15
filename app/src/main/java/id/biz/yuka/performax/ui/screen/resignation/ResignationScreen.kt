package id.biz.yuka.performax.ui.screen.resignation

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.ResignationEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResignationScreen(
    onBack: () -> Unit,
    viewModel: ResignationViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val headerColor = Color(0xFFFF7043)
    val backgroundColor = Color(0xFFF5F6F8)
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .drawBehind {
                    drawRect(color = headerColor, size = Size(size.width, size.height))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = 200.dp.toPx(),
                        center = Offset(size.width * 1.05f, -20.dp.toPx())
                    )
                }
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.refreshData() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                                }
                                Text(
                                    text = "Pengajuan Resign",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(48.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (viewModel.isOwner) "Persetujuan Resign Karyawan" else "Pengunduran Diri (Resign)",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (viewModel.isOwner)
                                        "Kelola dan proses pengajuan pengunduran diri karyawan."
                                    else
                                        "Form pengajuan pengunduran diri & info tanggal hari terakhir kerja.",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    if (viewModel.isOwner) {
                        // OWNER / ADMIN VIEW: List of Resignations
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Daftar Pengajuan Karyawan (${viewModel.allResignations.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        if (viewModel.allResignations.isEmpty() && !viewModel.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor)
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Belum ada pengajuan resign dari karyawan.", color = WonderGrey)
                                }
                            }
                        } else {
                            items(viewModel.allResignations) { item ->
                                Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                    OwnerResignationCard(
                                        resignation = item,
                                        onApprove = { viewModel.approveResignation(item.id) },
                                        onReject = { viewModel.selectedResignationToReject = item }
                                    )
                                }
                            }
                        }
                    } else {
                        // EMPLOYEE VIEW: Active Status OR Form Input
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                val active = viewModel.activeResignation
                                if (active != null) {
                                    // Status Card
                                    EmployeeResignationStatusCard(active)
                                } else {
                                    // Form Input
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Form Pengajuan Resign Baru",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Tanggal Terakhir Kerja
                                            OutlinedTextField(
                                                value = viewModel.proposedLastDayInput,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Tanggal Hari Terakhir Kerja") },
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        val now = LocalDate.now()
                                                        DatePickerDialog(
                                                            context,
                                                            { _, year, month, dayOfMonth ->
                                                                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                                                viewModel.proposedLastDayInput = selected.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                                            },
                                                            now.year,
                                                            now.monthValue - 1,
                                                            now.dayOfMonth
                                                        ).show()
                                                    }) {
                                                        Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            OutlinedTextField(
                                                value = viewModel.reasonInput,
                                                onValueChange = { viewModel.reasonInput = it },
                                                label = { Text("Alasan Pengunduran Diri") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                minLines = 3
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Button(
                                                onClick = { viewModel.submitResignation() },
                                                enabled = !viewModel.isSubmitting,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = headerColor)
                                            ) {
                                                if (viewModel.isSubmitting) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Send, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Kirim Pengajuan Resign", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(backgroundColor)
                        )
                    }
                }
            }
        }
    }

    // Dialog Rejection (Owner)
    viewModel.selectedResignationToReject?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.selectedResignationToReject = null },
            title = { Text("Tolak Pengajuan Resign", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Masukkan alasan penolakan untuk karyawan ${item.userName ?: "ini"}:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.rejectionReasonInput,
                        onValueChange = { viewModel.rejectionReasonInput = it },
                        label = { Text("Catatan / Alasan Penolakan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectResignation(item.id, viewModel.rejectionReasonInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Tolak Pengajuan")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.selectedResignationToReject = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun EmployeeResignationStatusCard(resignation: ResignationEntity) {
    val statusColor = when (resignation.status.lowercase()) {
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFE57373)
        else -> Color(0xFFFFB74D)
    }

    val statusText = when (resignation.status.lowercase()) {
        "approved" -> "DISETUJUI"
        "rejected" -> "DITOLAK"
        else -> "MENUNGGU PERSETUJUAN"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status Pengajuan Resign", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Hari Terakhir Kerja", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
            Text(resignation.proposedLastDay, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(12.dp))

            Text("Alasan Pengunduran Diri", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
            Text(resignation.reason, style = MaterialTheme.typography.bodyMedium)

            if (!resignation.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Catatan Penolakan:", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 12.sp)
                        resignation.rejectionReason?.let { Text(it, color = Color(0xFFB71C1C), fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerResignationCard(
    resignation: ResignationEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val statusColor = when (resignation.status.lowercase()) {
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFE57373)
        else -> Color(0xFFFFB74D)
    }

    val statusText = when (resignation.status.lowercase()) {
        "approved" -> "DISETUJUI"
        "rejected" -> "DITOLAK"
        else -> "MENUNGGU"
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resignation.userName ?: "Karyawan",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Tanggal Terakhir Kerja: ${resignation.proposedLastDay}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Alasan: ${resignation.reason}", fontSize = 13.sp, color = WonderGrey)

            if (resignation.status.lowercase() == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                    ) {
                        Text("Tolak")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Setujui")
                    }
                }
            }
        }
    }
}
