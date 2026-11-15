package id.biz.yuka.performax.ui.screen.loan.detail

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeLoanEntity
import id.biz.yuka.performax.ui.screen.loan.ApproveLoanBottomSheet
import id.biz.yuka.performax.ui.screen.loan.CreateLoanBottomSheet
import id.biz.yuka.performax.ui.screen.loan.LoanItemCard
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    onBack: () -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val headerColor = Color(0xFF4CAF50)
    val backgroundColor = Color(0xFFF5F6F8)

    var showCreateSheet by remember { mutableStateOf(false) }
    var loanToApprove by remember { mutableStateOf<EmployeeLoanEntity?>(null) }
    var showApproveSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.errorMessage = null
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.successMessage = null
        }
    }

    val isSelfView = viewModel.isSelfView
    val targetName = viewModel.targetEmployee?.fullName ?: if (isSelfView) "Saya" else "Karyawan"

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .drawBehind {
                    drawRect(
                        color = headerColor,
                        size = Size(size.width, size.height)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = 220.dp.toPx(),
                        center = Offset(size.width * 1.1f, -40.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = 110.dp.toPx(),
                        center = Offset(size.width * 0.15f, size.height * 0.8f)
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
                    // Top App Bar Header
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
                                    text = if (isSelfView) "Pinjaman Saya" else "Detail Pinjaman",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(48.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Subtitle Banner
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isSelfView) "Fasilitas Pinjaman Karyawan" else targetName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (isSelfView)
                                        "Ajukan pinjaman kas untuk kebutuhan mendesak. Maksimal 1 pinjaman aktif diperbolehkan."
                                    else
                                        "Riwayat pinjaman dan jadwal angsuran milik $targetName.",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Employee Request Actions (Only for Self View / Employee)
                    if (isSelfView && !viewModel.isOwnerOrAdmin) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(24.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (!viewModel.canRequestLoan) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Kamu belum memenuhi syarat untuk pengajuan pinjaman. Silakan hubungi Owner/Admin.",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    } else if (viewModel.hasActiveLoan) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = WonderOrange)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Anda memiliki 1 pinjaman aktif / dalam proses. Selesaikan pinjaman berjalan sebelum membuat pengajuan baru.",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { showCreateSheet = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = headerColor)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Ajukan Pinjaman Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Riwayat & Status Pinjaman",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = "Daftar Pinjaman: $targetName",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // Loan Items
                    if (viewModel.loanList.isEmpty() && !viewModel.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada riwayat pinjaman.", color = WonderGrey)
                            }
                        }
                    } else {
                        items(viewModel.loanList) { loan ->
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                LoanItemCard(
                                    loan = loan,
                                    isOwner = viewModel.isOwnerOrAdmin,
                                    onApproveClick = {
                                        loanToApprove = loan
                                        showApproveSheet = true
                                    },
                                    onRespondEmployeeClick = { isApproved ->
                                        viewModel.respondLoanByEmployee(loan.id, isApproved)
                                    }
                                )
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

    // Modal BottomSheet Pengajuan Baru (Employee)
    if (showCreateSheet) {
        CreateLoanBottomSheet(
            onDismiss = { showCreateSheet = false },
            onSubmit = { amount, tenor, reason ->
                viewModel.submitLoanRequest(amount, tenor, reason) {
                    showCreateSheet = false
                }
            }
        )
    }

    // Modal BottomSheet Approval (Owner)
    if (showApproveSheet && loanToApprove != null) {
        ApproveLoanBottomSheet(
            loan = loanToApprove!!,
            onDismiss = { showApproveSheet = false },
            onApprove = { amount, tenor ->
                viewModel.approveLoan(loanToApprove!!.id, amount, tenor) {
                    showApproveSheet = false
                }
            }
        )
    }
}
