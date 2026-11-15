package id.biz.yuka.performax.ui.screen.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.EmployeeLoanEligibilityEntity
import id.biz.yuka.libperformx.entity.EmployeeLoanEntity
import id.biz.yuka.libperformx.entity.EmployeeLoanInstallmentEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: LoanViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val headerColor = Color(0xFF4CAF50)
    val backgroundColor = Color(0xFFF5F6F8)

    var showEligibilitySheet by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Header Decorative Layer
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
                    // Header Bar
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
                                    text = "Kelola Pinjaman Karyawan",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                )
                                if (viewModel.isOwnerOrAdmin) {
                                    IconButton(onClick = {
                                        viewModel.loadAllEmployees()
                                        showEligibilitySheet = true
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Kelola Hak Pinjaman", tint = Color.White)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(48.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Brief Banner Description
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Karyawan Eligible Loan",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Daftar karyawan yang memiliki hak pengajuan pinjaman kas. Tekan pada karyawan untuk melihat detail & rincian cicilan.",
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

                    // Content Body: List of Eligible Employees
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "Karyawan Memenuhi Syarat (${viewModel.eligibleEmployees.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (viewModel.eligibleEmployees.isEmpty() && !viewModel.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada karyawan dengan hak pengajuan pinjaman.\nTekan ikon '+' di kanan atas untuk mengaktifkan karyawan.",
                                    color = WonderGrey,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        items(viewModel.eligibleEmployees) { emp ->
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                EligibleEmployeeCard(
                                    employee = emp,
                                    onClick = { onNavigateToDetail(emp.userId) }
                                )
                            }
                        }
                    }

                    // Bottom Spacing
                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(backgroundColor)
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet Kelola Status Loan Karyawan
    if (showEligibilitySheet) {
        ManageLoanEligibilityBottomSheet(
            allEmployees = viewModel.allEmployees,
            isLoading = viewModel.isLoadingAllEmployees,
            onDismiss = { showEligibilitySheet = false },
            onToggleStatus = { employee, newStatus ->
                viewModel.toggleEmployeeLoanEligibility(employee, newStatus)
            }
        )
    }
}

// ==========================================
// CARD KARYAWAN ELIGIBLE
// ==========================================
@Composable
fun EligibleEmployeeCard(
    employee: EmployeeLoanEligibilityEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = employee.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = employee.roleName,
                        style = MaterialTheme.typography.bodySmall,
                        color = WonderGrey
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Detail Pinjaman",
                tint = WonderGrey
            )
        }
    }
}

// ==========================================
// BOTTOM SHEET KELOLA STATUS LOAN KARYAWAN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLoanEligibilityBottomSheet(
    allEmployees: List<EmployeeEntity>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onToggleStatus: (employee: EmployeeEntity, newStatus: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Text("Kelola Hak Pinjaman Karyawan", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Aktifkan atau nonaktifkan hak karyawan untuk mengajukan pinjaman kas.",
                style = MaterialTheme.typography.bodySmall,
                color = WonderGrey
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                }
            } else if (allEmployees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada karyawan aktif ditemukan.", color = WonderGrey)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allEmployees) { employee ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF9F9F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = employee.fullName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = employee.roleName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WonderGrey
                                    )
                                }

                                Switch(
                                    checked = employee.canRequestLoan,
                                    onCheckedChange = { checked ->
                                        onToggleStatus(employee, checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF4CAF50)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CARD LOAN ITEM & TAGIHAN CICILAN (Shared)
// ==========================================
@Composable
fun LoanItemCard(
    loan: EmployeeLoanEntity,
    isOwner: Boolean,
    onApproveClick: () -> Unit = {},
    onRespondEmployeeClick: ((Boolean) -> Unit)? = null
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    var expandedInstallments by remember { mutableStateOf(false) }

    val statusLower = loan.status.lowercase()
    val (statusLabel, statusColor) = when (statusLower) {
        "approved", "active" -> "APPROVED" to Color(0xFF2E7D32)
        "pending" -> "PENDING OWNER" to Color(0xFFEF6C00)
        "waiting_employee_approval" -> "MENUNGGU PERSETUJUAN KARYAWAN" to Color(0xFF7B1FA2)
        "rejected" -> "DITOLAK ADMIN" to Color(0xFFC62828)
        "rejected_by_employee" -> "DITOLAK KARYAWAN" to Color(0xFFD32F2F)
        "paid" -> "LUNAS" to Color(0xFF1565C0)
        else -> loan.status.uppercase() to WonderGrey
    }

    val statusBg = statusColor.copy(alpha = 0.15f)
    val isLoanFailed = statusLower == "rejected" || statusLower == "rejected_by_employee"

    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
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
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor)
                    )
                }

                Text(
                    text = loan.createdAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = WonderGrey
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Pengajuan", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                    Text(
                        currencyFormat.format(loan.requestedAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text("${loan.requestedTenorMonths} Bulan", style = MaterialTheme.typography.bodySmall)
                }

                if (loan.approvedAmount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Disetujui Owner", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                        Text(
                            currencyFormat.format(loan.approvedAmount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        )
                        Text("${loan.approvedTenorMonths} Bulan (${currencyFormat.format(loan.monthlyInstallment)}/bln)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (loan.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Alasan: ${loan.reason}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            }

            if (isOwner && statusLower == "pending") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onApproveClick,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setujui Pinjaman", fontWeight = FontWeight.Bold)
                }
            }

            if (!isOwner && statusLower == "waiting_employee_approval" && onRespondEmployeeClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0xFFF3E5F5),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Konfirmasi Persetujuan Karyawan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Owner telah menyetujui pinjaman dengan nominal & tenor di atas. Apakah Anda setuju?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onRespondEmployeeClick(false) },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                            ) {
                                Text("Tolak", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onRespondEmployeeClick(true) },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Setuju & Terima", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (loan.installments.isNotEmpty() && !isLoanFailed) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedInstallments = !expandedInstallments }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jadwal & Status Cicilan (${loan.installments.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    )
                    Icon(
                        imageVector = if (expandedInstallments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF1565C0)
                    )
                }

                if (expandedInstallments) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        loan.installments.forEach { inst ->
                            InstallmentRowItem(installment = inst, currencyFormat = currencyFormat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstallmentRowItem(
    installment: EmployeeLoanInstallmentEntity,
    currencyFormat: NumberFormat
) {
    val isPaid = installment.status.lowercase() == "paid"
    val badgeColor = if (isPaid) Color(0xFF2E7D32) else Color(0xFFE65100)

    Surface(
        color = Color(0xFFF9F9F9),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Angsuran ke-${installment.installmentNumber}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Jatuh Tempo: ${installment.dueDate.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WonderGrey
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(installment.amount),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (isPaid) "LUNAS" else "BELUM BAYAR",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
            }
        }
    }
}

// ==========================================
// BOTTOM SHEET EMPLOYEE (PENGAJUAN LOAN)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLoanBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, tenor: Int, reason: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountStr by remember { mutableStateOf("") }
    var tenorStr by remember { mutableStateOf("1") }
    var reasonStr by remember { mutableStateOf("") }

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
            Text("Pengajuan Pinjaman Kas", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Nominal Pinjaman (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tenorStr,
                onValueChange = { tenorStr = it },
                label = { Text("Tenor Angsuran (Bulan)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = reasonStr,
                onValueChange = { reasonStr = it },
                label = { Text("Alasan Pengajuan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val tenor = tenorStr.toIntOrNull() ?: 1
                    onSubmit(amt, tenor, reasonStr)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Kirim Pengajuan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ==========================================
// BOTTOM SHEET OWNER (APPROVAL LOAN)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveLoanBottomSheet(
    loan: EmployeeLoanEntity,
    onDismiss: () -> Unit,
    onApprove: (amount: Double, tenor: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var approvedAmountStr by remember { mutableStateOf(loan.requestedAmount.toLong().toString()) }
    var approvedTenorStr by remember { mutableStateOf(loan.requestedTenorMonths.toString()) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

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
            Text("Persetujuan Pinjaman (Owner)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Edit nominal & tenor yang disetujui untuk karyawan", style = MaterialTheme.typography.bodySmall, color = WonderGrey)

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pengajuan Karyawan:", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                    Text("Nominal: ${currencyFormat.format(loan.requestedAmount)}", fontWeight = FontWeight.Bold)
                    Text("Tenor: ${loan.requestedTenorMonths} Bulan", fontWeight = FontWeight.Bold)
                    if (loan.reason.isNotBlank()) {
                        Text("Alasan: ${loan.reason}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = approvedAmountStr,
                onValueChange = { approvedAmountStr = it },
                label = { Text("Nominal Disetujui (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = approvedTenorStr,
                onValueChange = { approvedTenorStr = it },
                label = { Text("Tenor Disetujui (Bulan)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = approvedAmountStr.toDoubleOrNull() ?: 0.0
                    val tenor = approvedTenorStr.toIntOrNull() ?: 1
                    onApprove(amt, tenor)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Konfirmasi Persetujuan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
