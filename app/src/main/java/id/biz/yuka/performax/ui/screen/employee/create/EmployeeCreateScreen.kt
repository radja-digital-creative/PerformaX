package id.biz.yuka.performax.ui.screen.employee.create

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeCreateScreen(
    onBack: () -> Unit,
    viewModel: EmployeeCreateViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            delay(2000)
            onBack()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF4F6F8),
        topBar = {
            TopAppBar(
                title = { Text("Tambah Karyawan Baru", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    Button(
                        onClick = { viewModel.createEmployee() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !viewModel.isLoading && viewModel.allowedRoles.any { it.first == viewModel.roleId }
                    ) {
                        if (viewModel.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Simpan Karyawan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.errorMessage != null) {
                item { Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp)) } }
            }
            if (viewModel.successMessage != null) {
                item { Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(viewModel.successMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) } }
            }

            // ==========================================
            // 1. KARTU AKUN KREDENSIAL
            // ==========================================
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = WonderOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Akun & Login", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = viewModel.email, onValueChange = { viewModel.email = it }, label = { Text("Email (Wajib)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        id.biz.yuka.performax.ui.screen.account.NewPasswordField(value = viewModel.password, onValueChange = { viewModel.password = it }, label = "Password Sementara (Wajib)")
                    }
                }
            }

            // ==========================================
            // 2. KARTU INFORMASI PERSONAL
            // ==========================================
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBox, null, tint = WonderOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Informasi Personal", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = viewModel.fullName, onValueChange = { viewModel.fullName = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = viewModel.phone, onValueChange = { viewModel.phone = it }, label = { Text("No. Handphone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = viewModel.address, onValueChange = { viewModel.address = it }, label = { Text("Alamat Tinggal") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Jenis Kelamin", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = viewModel.gender == "male", onClick = { viewModel.gender = "male" }, colors = RadioButtonDefaults.colors(selectedColor = WonderOrange))
                            Text("Laki-laki")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = viewModel.gender == "female", onClick = { viewModel.gender = "female" }, colors = RadioButtonDefaults.colors(selectedColor = WonderOrange))
                            Text("Perempuan")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Posisi / Role", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                        Column {
                            viewModel.allowedRoles.forEach { (id, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = viewModel.roleId == id, onClick = { viewModel.roleId = id }, colors = RadioButtonDefaults.colors(selectedColor = WonderOrange))
                                    Text(name)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. KARTU PENEMPATAN TOKO (MULTI-STORE)
            // ==========================================
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, null, tint = WonderOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Penempatan Toko", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ScrollableTabRow(
                            selectedTabIndex = viewModel.selectedTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = WonderOrange,
                            edgePadding = 16.dp,
                            indicator = { tabPositions ->
                                if (viewModel.selectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedTabIndex]),
                                        color = WonderOrange
                                    )
                                }
                            }
                        ) {
                            viewModel.storeAssignments.forEachIndexed { index, assignment ->
                                Tab(
                                    selected = viewModel.selectedTabIndex == index,
                                    onClick = { viewModel.selectedTabIndex = index },
                                    text = {
                                        Text(
                                            text = "Toko ${index + 1}" + if (assignment.isPrimary) " ★" else "",
                                            fontWeight = if (viewModel.selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (viewModel.selectedTabIndex == index) WonderOrange else Color.Gray
                                        )
                                    }
                                )
                            }
                            Tab(
                                selected = false,
                                onClick = { viewModel.addStoreAssignment() },
                                text = { Text("+ Tambah", color = Color.Gray) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ISI TAB (FORM DROPDOWN)
                        if (viewModel.storeAssignments.isNotEmpty() && viewModel.selectedTabIndex < viewModel.storeAssignments.size) {
                            val currentAssignment = viewModel.storeAssignments[viewModel.selectedTabIndex]
                            var expandedStore by remember { mutableStateOf(false) }

                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                                ExposedDropdownMenuBox(
                                    expanded = expandedStore,
                                    onExpandedChange = { expandedStore = it }
                                ) {
                                    OutlinedTextField(
                                        value = currentAssignment.storeName.ifEmpty { "Pilih Toko" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Toko Penempatan") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStore) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedStore,
                                        onDismissRequest = { expandedStore = false }
                                    ) {
                                        if (viewModel.availableStores.isEmpty()) {
                                            DropdownMenuItem(text = { Text("Memuat toko...") }, onClick = { })
                                        } else {
                                            viewModel.availableStores.forEach { store ->
                                                DropdownMenuItem(
                                                    text = { Text(store.name) },
                                                    onClick = {
                                                        viewModel.storeAssignments[viewModel.selectedTabIndex] = currentAssignment.copy(
                                                            storeId = store.id,
                                                            storeName = store.name
                                                        )
                                                        expandedStore = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Jadikan Toko Utama", fontWeight = FontWeight.Bold)
                                        Text("Karyawan akan default absen di toko ini", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = currentAssignment.isPrimary,
                                        onCheckedChange = { isChecked -> if (isChecked) viewModel.setAsPrimaryStore(viewModel.selectedTabIndex) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = WonderOrange, checkedTrackColor = WonderOrange.copy(alpha=0.5f))
                                    )
                                }

                                if (viewModel.storeAssignments.size > 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.removeStoreAssignment(viewModel.selectedTabIndex) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Hapus Penempatan Ini")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. KARTU FINANSIAL & POTONGAN (DEDUCTIONS)
            // ==========================================
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = WonderOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Data Finansial Awal", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Sistem Penggajian", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = viewModel.payrollType == "monthly", onClick = { viewModel.payrollType = "monthly" }, colors = RadioButtonDefaults.colors(selectedColor = WonderOrange))
                            Text("Bulanan (Monthly)")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = viewModel.payrollType == "weekly", onClick = { viewModel.payrollType = "weekly" }, colors = RadioButtonDefaults.colors(selectedColor = WonderOrange))
                            Text("Mingguan (Weekly)")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.payPerDay,
                            onValueChange = { viewModel.payPerDay = it },
                            label = { Text("Gaji Per Hari (Rp)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        val estimatedSalary = (viewModel.payPerDay.toDoubleOrNull() ?: 0.0) * 26.0
                        Text(
                            text = "Gaji Pokok Otomatis: ${formatRupiah(estimatedSalary)} (26 Hari Kerja Efektif)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        OutlinedTextField(value = viewModel.transportAllowance, onValueChange = { viewModel.transportAllowance = it }, label = { Text("Tunjangan Transport (Angka)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = viewModel.mealAllowance, onValueChange = { viewModel.mealAllowance = it }, label = { Text("Tunjangan Makan (Angka)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = viewModel.positionAllowance, onValueChange = { viewModel.positionAllowance = it }, label = { Text("Tunjangan Jabatan (Angka)") }, modifier = Modifier.fillMaxWidth())

                        // BAGIAN POTONGAN (CHECKBOX)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MoneyOff, null, tint = Color.Red.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Komponen Potongan Gaji", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (viewModel.availableDeductions.isEmpty()) {
                            Text("Memuat data potongan...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            if (viewModel.availableDeductions.isEmpty()) {
                                Text("Memuat data potongan...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            } else {
                                viewModel.availableDeductions.forEach { deduction ->
                                    val isChecked = viewModel.selectedDeductionIds.contains(deduction.id)

                                    // Tentukan teks nominal (Persen atau Fixed Amount)
                                    val amountText = if (deduction.percentageValue > 0.0) {
                                        "${deduction.percentageValue}%"
                                    } else {
                                        formatRupiah(deduction.fixedAmount)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { viewModel.toggleDeduction(deduction.id) },
                                            colors = CheckboxDefaults.colors(checkedColor = WonderOrange)
                                        )

                                        Column(modifier = Modifier.padding(start = 4.dp)) {
                                            Text(
                                                text = "${deduction.name} ($amountText)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "ID: ${deduction.id}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
// ==========================================
// FUNGSI HELPER: FORMAT RUPIAH
// ==========================================
fun formatRupiah(number: Double): String {
    val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatRupiah.format(number).replace("Rp", "Rp ").substringBefore(",")
}