package id.biz.yuka.performax.ui.screen.violation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateViolationScreen(
    onBack: () -> Unit,
    onSuccessReturn: () -> Unit,
    viewModel: CreateViolationViewModel = hiltViewModel()
) {
    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)

    // Form States
    var expandedUser by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<id.biz.yuka.libperformx.entity.EmployeeEntity?>(null) }
    var expandedStore by remember { mutableStateOf(false) }
    var selectedStore by remember { mutableStateOf<id.biz.yuka.libperformx.entity.StoreEntity?>(null) }

    var expandedRule by remember { mutableStateOf(false) }
    var selectedRule by remember { mutableStateOf<id.biz.yuka.libperformx.entity.EmployeeRuleEntity?>(null) }

    var notes by remember { mutableStateOf("") }
    var appliedPenalty by remember { mutableStateOf("") }

    // Auto-fill Sanksi ketika aturan dipilih
    LaunchedEffect(selectedRule) {
        selectedRule?.let { rule ->
            appliedPenalty = when (rule.penaltyType) {
                "deduction_fixed" -> "Denda Rp ${rule.penaltyFixedAmount.toLong()}"
                "kpi_deduction_fixed" -> "Potongan KPI ${rule.penaltyFixedAmount.toLong()} Poin"
                "deduction_multiplier" -> "Potongan ${rule.penaltyMultiplier}x Gaji"
                "status_change" -> "Perubahan Status ke: ${rule.penaltyStatusAction}"
                else -> "Sanksi Manual"
            }
        }
    }

    // Navigasi kembali jika sukses
    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) {
            onSuccessReturn()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // LAYER 1: Background Header Statis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                item {
                    Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 16.dp))

                    // ROW 1: HEADER NAVIGATION
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.Black) }
                        Text("Tambah Teguran", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                        IconButton(onClick = { }) { Icon(Icons.Default.Description, "Log", tint = Color.Transparent) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ROW 2: DESKRIPSI
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Teguran Kedisiplinan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                        Text(
                            text = "Berikan teguran SP atau sanksi manual kepada karyawan atas pelanggaran spesifik di luar absensi otomatis.",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray),
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // ROW 4: FORM DALAM KARTU
                item {
                    Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Detail Pelanggar", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Dropdown Karyawan
                                ExposedDropdownMenuBox(expanded = expandedUser, onExpandedChange = { expandedUser = !expandedUser }) {
                                    OutlinedTextField(
                                        // Ubah pemanggilan properti menjadi fullName
                                        value = selectedUser?.fullName ?: "Pilih Karyawan",
                                        onValueChange = {}, readOnly = true, label = { Text("Nama Karyawan") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUser) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(expanded = expandedUser, onDismissRequest = { expandedUser = false }) {
                                        viewModel.employees.forEach { emp ->
                                            // Ubah pemanggilan properti menjadi fullName
                                            DropdownMenuItem(text = { Text(emp.fullName) }, onClick = { selectedUser = emp; expandedUser = false })
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dropdown Toko
                                ExposedDropdownMenuBox(expanded = expandedStore, onExpandedChange = { expandedStore = !expandedStore }) {
                                    OutlinedTextField(
                                        value = selectedStore?.name ?: "Pilih Toko/Cabang",
                                        onValueChange = {}, readOnly = true, label = { Text("Lokasi Kejadian") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStore) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(expanded = expandedStore, onDismissRequest = { expandedStore = false }) {
                                        viewModel.stores.forEach { store ->
                                            DropdownMenuItem(text = { Text(store.name) }, onClick = { selectedStore = store; expandedStore = false })
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                                Spacer(modifier = Modifier.height(24.dp))

                                Text("Rincian Pelanggaran", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Dropdown Aturan (Hanya Misconduct)
                                ExposedDropdownMenuBox(expanded = expandedRule, onExpandedChange = { expandedRule = !expandedRule }) {
                                    OutlinedTextField(
                                        value = selectedRule?.ruleName ?: "Pilih Aturan",
                                        onValueChange = {}, readOnly = true, label = { Text("Tipe Pelanggaran") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRule) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(expanded = expandedRule, onDismissRequest = { expandedRule = false }) {
                                        if (viewModel.misconductRules.isEmpty()) {
                                            DropdownMenuItem(text = { Text("Belum ada aturan khusus kedisiplinan") }, onClick = { expandedRule = false })
                                        } else {
                                            viewModel.misconductRules.forEach { rule ->
                                                DropdownMenuItem(text = { Text(rule.ruleName) }, onClick = { selectedRule = rule; expandedRule = false })
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Catatan Detail / Alasan") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 3
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = appliedPenalty,
                                    onValueChange = { appliedPenalty = it },
                                    label = { Text("Sanksi / Konsekuensi") },
                                    placeholder = { Text("Contoh: SP 1, Denda 50rb") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = {
                                        if (selectedUser != null && selectedStore != null && selectedRule != null && appliedPenalty.isNotBlank()) {
                                            viewModel.submitViolation(
                                                userId = selectedUser!!.id, // Ubah ID dari 'id' menjadi 'userId' sesuai dengan bentuk EmployeeEntity
                                                storeId = selectedStore!!.id,
                                                ruleId = selectedRule!!.id,
                                                notes = notes,
                                                appliedPenalty = appliedPenalty
                                            )
                                        }
                                    },
                                    enabled = !viewModel.isLoading,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)), // Merah Peringatan
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (viewModel.isLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("Simpan Teguran", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.fillMaxWidth().height(120.dp).background(backgroundColor)) }
                item { Spacer(modifier = Modifier.fillMaxWidth().height(500.dp).background(backgroundColor)) }
            }
        }
    }
}