package id.biz.yuka.performax.ui.screen.manualdeductions

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.ManualDeductionEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualDeductionsScreen(
    onBack: () -> Unit,
    viewModel: ManualDeductionsViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val headerColor = Color(0xFF8D6E63)
    val backgroundColor = Color(0xFFF5F6F8)

    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadData() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    // Header Bar with Back & (+) Action Button
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
                                    text = "Potongan Manual Gaji",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                )
                                IconButton(onClick = { viewModel.openAddSheet() }) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah Potongan", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Daftar Potongan Karyawan",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Kelola potongan gaji khusus (kasbon, denda, kerusakan) per individu karyawan.",
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

                    // Section Title
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daftar Potongan Bulan Ini (${state.deductions.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                OutlinedButton(
                                    onClick = { viewModel.openAddSheet() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = headerColor)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // History Items
                    if (state.deductions.isEmpty() && !state.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.MoneyOff, contentDescription = null, tint = WonderGrey, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Belum ada potongan manual bulan ini.", color = WonderGrey, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    } else {
                        items(state.deductions) { deduction ->
                            val matchedEmployeeName = state.employees.firstOrNull { it.id == deduction.userSalaryId }?.fullName
                            val displayEmployeeName = deduction.userName ?: matchedEmployeeName ?: "Karyawan"

                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                ManualDeductionItemCard(
                                    employeeName = displayEmployeeName,
                                    deduction = deduction,
                                    onDelete = { viewModel.deleteManualDeduction(deduction.id) }
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

    // Modal BottomSheet Tambah Potongan Baru per Individu
    if (state.showAddSheet) {
        AddManualDeductionBottomSheet(
            employees = state.employees,
            selectedEmployee = viewModel.selectedEmployee,
            onSelectEmployee = { viewModel.selectedEmployee = it },
            titleInput = viewModel.titleInput,
            onTitleChange = { viewModel.titleInput = it },
            amountInput = viewModel.amountInput,
            onAmountChange = { viewModel.amountInput = it },
            notesInput = viewModel.notesInput,
            onNotesChange = { viewModel.notesInput = it },
            isSubmitting = state.isSubmitting,
            onDismiss = { viewModel.closeAddSheet() },
            onSubmit = { viewModel.submitManualDeduction() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualDeductionBottomSheet(
    employees: List<EmployeeEntity>,
    selectedEmployee: EmployeeEntity?,
    onSelectEmployee: (EmployeeEntity) -> Unit,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    amountInput: String,
    onAmountChange: (String) -> Unit,
    notesInput: String,
    onNotesChange: (String) -> Unit,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val headerColor = Color(0xFF8D6E63)
    var employeeDropdownExpanded by remember { mutableStateOf(false) }

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
            Text("Tambah Potongan Karyawan", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Pilih karyawan dan masukkan rincian potongan manual per individu.",
                style = MaterialTheme.typography.bodySmall,
                color = WonderGrey
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Employee Picker Dropdown
            ExposedDropdownMenuBox(
                expanded = employeeDropdownExpanded,
                onExpandedChange = { employeeDropdownExpanded = !employeeDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedEmployee?.fullName ?: "Pilih Karyawan",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Karyawan Target") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = employeeDropdownExpanded,
                    onDismissRequest = { employeeDropdownExpanded = false }
                ) {
                    if (employees.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Tidak ada karyawan aktif", color = WonderGrey) },
                            onClick = { employeeDropdownExpanded = false }
                        )
                    } else {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(emp.fullName, fontWeight = FontWeight.Bold)
                                        Text(emp.roleName, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    }
                                },
                                onClick = {
                                    onSelectEmployee(emp)
                                    employeeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = titleInput,
                onValueChange = onTitleChange,
                label = { Text("Judul Potongan (cth: Kasbon / Denda)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountInput,
                onValueChange = onAmountChange,
                label = { Text("Nominal Potongan (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notesInput,
                onValueChange = onNotesChange,
                label = { Text("Catatan / Keterangan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = headerColor)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Potongan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ManualDeductionItemCard(
    employeeName: String,
    deduction: ManualDeductionEntity,
    onDelete: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFEBE9)
                ) {
                    Text(
                        text = employeeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF5D4037),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = deduction.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = currencyFormat.format(deduction.amount),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC62828)
                    )
                )
                if (!deduction.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Catatan: ${deduction.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WonderGrey
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus Potongan",
                    tint = Color(0xFFE57373)
                )
            }
        }
    }
}
