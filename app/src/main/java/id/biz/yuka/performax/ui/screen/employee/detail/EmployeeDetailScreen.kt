package id.biz.yuka.performax.ui.screen.employee.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.components.GlobalErrorPopup
import id.biz.yuka.performax.ui.screen.employee.create.formatRupiah
import id.biz.yuka.performax.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    onBack: () -> Unit,
    viewModel: EmployeeDetailViewModel = hiltViewModel()
) {
    val emp = viewModel.employee

    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            delay(3000)
            viewModel.successMessage = null
        }
    }

    Scaffold(
        containerColor = Color(0xFFF4F6F8),
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) "Edit Profil" else "Detail Karyawan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (viewModel.isEditMode) viewModel.isEditMode = false else onBack() }) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    if (viewModel.isOwner && !viewModel.isEditMode && emp != null) {
                        IconButton(onClick = { viewModel.isEditMode = true }) {
                            Icon(Icons.Default.Edit, "Edit", tint = WonderOrange)
                        }
                        IconButton(onClick = { viewModel.showDeleteDialog = true }) {
                            Icon(Icons.Default.PersonRemove, "Hapus Karyawan", tint = Color(0xFFC62828))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (viewModel.isEditMode) {
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                        Button(
                            onClick = { viewModel.saveChanges() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !viewModel.isLoading
                        ) {
                            if (viewModel.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Simpan Perubahan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        // ==========================================
        // DIALOG KONFIRMASI HAPUS KARYAWAN
        // ==========================================
        if (viewModel.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!viewModel.isLoading) viewModel.showDeleteDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        "Nonaktifkan Karyawan?",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column {
                        Text(
                            "Karyawan ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            emp?.fullName ?: "",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            " akan dinonaktifkan dan tidak dapat login ke aplikasi. Data karyawan tetap tersimpan.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (viewModel.errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                viewModel.errorMessage!!,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteEmployee(onBack) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Ya, Nonaktifkan", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.showDeleteDialog = false },
                        enabled = !viewModel.isLoading
                    ) {
                        Text("Batal")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (viewModel.isLoading && emp == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderOrange)
            }
        } else if (emp != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ==========================================
                // 1. HEADER PROFIL
                // ==========================================
                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(Color.White).padding(4.dp)) {
                            if (emp.profileUrl.isNotBlank()) {
                                AsyncImage(
                                    model = emp.profileUrl,
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(WonderGrey.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = WonderGrey, modifier = Modifier.size(60.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (!viewModel.isEditMode) {
                            Text(emp.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = WonderLime.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable(enabled = viewModel.isOwner) { viewModel.openRoleSheet() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(emp.roleName.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        if (viewModel.isOwner) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.SyncAlt, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                                Surface(color = if (emp.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                                    Text(if (emp.isActive) "AKTIF" else "NONAKTIF", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (emp.isActive) Color(0xFF2E7D32) else Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }

                if (viewModel.successMessage != null) {
                    item { Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(viewModel.successMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) } }
                }
                if (viewModel.errorMessage != null) {
                    item { Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp)) } }
                }

                // ==========================================
                // 2. KARTU AKUN (Khusus Edit)
                // ==========================================
                if (viewModel.isEditMode && viewModel.isOwner) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, null, tint = WonderOrange)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Akun & Keamanan", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(value = viewModel.editEmail, onValueChange = { viewModel.editEmail = it }, label = { Text("Email Baru (Opsional)") }, placeholder = { Text("Biarkan kosong jika tidak diubah") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.editPassword, onValueChange = { viewModel.editPassword = it }, label = { Text("Password Baru (Opsional)") }, placeholder = { Text("Biarkan kosong jika tidak diubah") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                // ==========================================
                // 3. KARTU INFORMASI PERSONAL
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

                            if (viewModel.isEditMode) {
                                OutlinedTextField(value = viewModel.editName, onValueChange = { viewModel.editName = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.editPhone, onValueChange = { viewModel.editPhone = it }, label = { Text("No. HP") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.editAddress, onValueChange = { viewModel.editAddress = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // ✅ INPUT JOIN DATE (Edit Mode)
                                var showDatePicker by remember { mutableStateOf(false) }
                                val datePickerState = rememberDatePickerState()
                                
                                OutlinedTextField(
                                    value = viewModel.editJoinDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tanggal Bergabung") },
                                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = Color.Black,
                                        disabledBorderColor = Color.Gray,
                                        disabledLabelColor = WonderOrange
                                    ),
                                    trailingIcon = { Icon(Icons.Default.CalendarMonth, null) }
                                )

                                if (showDatePicker) {
                                    DatePickerDialog(
                                        onDismissRequest = { showDatePicker = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                val selectedDate = datePickerState.selectedDateMillis
                                                if (selectedDate != null) {
                                                    val date = Instant.ofEpochMilli(selectedDate).atZone(ZoneId.systemDefault()).toLocalDate()
                                                    viewModel.editJoinDate = date.toString()
                                                }
                                                showDatePicker = false
                                            }) { Text("Pilih") }
                                        }
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }
                            } else {
                                DetailRowItem(Icons.Default.Phone, "Nomor Handphone", emp.phoneNumber)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                                DetailRowItem(Icons.Default.Wc, "Jenis Kelamin", if (emp.gender == "male") "Laki-laki" else "Perempuan")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                                DetailRowItem(Icons.Default.LocationOn, "Alamat Tinggal", emp.address)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                                
                                // ✅ DISPLAY JOIN DATE (View Mode)
                                val formattedJoinDate = try {
                                    val date = LocalDate.parse(emp.joinDate?.take(10) ?: "")
                                    date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("id")))
                                } catch (_: Exception) {
                                    emp.joinDate ?: "-"
                                }
                                DetailRowItem(Icons.Default.CalendarToday, "Tanggal Bergabung", formattedJoinDate)
                            }
                        }
                    }
                }

                if (viewModel.isOwner && !viewModel.isEditMode) {
                    item { EmployeeAttendanceSettings(viewModel) }
                }

                // ==========================================
                // 4. KARTU PENEMPATAN TOKO
                // ==========================================
                if (viewModel.isEditMode && viewModel.isOwner) {
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
                                                onCheckedChange = { isChecked ->
                                                    if (isChecked) viewModel.setAsPrimaryStore(viewModel.selectedTabIndex)
                                                },
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
                }

                // ==========================================
                // 5. KARTU BIOMETRIK & PENEMPATAN (Mode Lihat)
                // ==========================================
                if (!viewModel.isEditMode) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storefront, null, tint = WonderOrange)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Penempatan & Keamanan", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                val storeNames = emp.stores.joinToString(", ") { it.name }.ifEmpty { "Belum ada penempatan" }
                                DetailRowItem(Icons.Default.MapsHomeWork, "Toko Ditugaskan", storeNames)

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

                                val isBioComplete = emp.faceEmbedding.isNotBlank()
                                DetailRowItem(
                                    Icons.Default.Fingerprint,
                                    "Status Biometrik",
                                    if (isBioComplete) "Terverifikasi (Score: ${emp.ktpMatchScore.toInt()}%)" else "Belum Dilengkapi",
                                    valueColor = if (isBioComplete) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 6. KARTU FINANSIAL & POTONGAN
                // ==========================================
                if (viewModel.isOwner || !viewModel.isEditMode) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, null, tint = WonderOrange)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Data Finansial", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                if (viewModel.isEditMode && viewModel.isOwner) {
                                    Text("Sistem Penggajian", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = viewModel.editPayrollType == "monthly", onClick = { viewModel.editPayrollType = "monthly" }, colors = RadioButtonDefaults.colors(selectedColor = WonderOrange))
                                        Text("Bulanan (Monthly)")
                                        Spacer(modifier = Modifier.width(16.dp))
                                        RadioButton(selected = viewModel.editPayrollType == "weekly", onClick = { viewModel.editPayrollType = "weekly" }, colors = RadioButtonDefaults.colors(selectedColor = WonderOrange))
                                        Text("Mingguan (Weekly)")
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(value = viewModel.editPayPerDay, onValueChange = { viewModel.editPayPerDay = it }, label = { Text("Gaji Per Hari (Angka/Rp)") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(value = viewModel.editBasicSalary, onValueChange = { viewModel.editBasicSalary = it }, label = { Text("Gaji Pokok (Angka/Rp)") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(value = viewModel.editTransportAllowance, onValueChange = { viewModel.editTransportAllowance = it }, label = { Text("Tunjangan Transport (Angka)") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(value = viewModel.editMealAllowance, onValueChange = { viewModel.editMealAllowance = it }, label = { Text("Tunjangan Makan (Angka)") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(value = viewModel.editPositionAllowance, onValueChange = { viewModel.editPositionAllowance = it }, label = { Text("Tunjangan Jabatan (Angka)") }, modifier = Modifier.fillMaxWidth())

                                    // BAGIAN POTONGAN (CHECKBOX)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
                                    Text("Sistem Potongan", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(value = viewModel.editPerMinute, onValueChange = { viewModel.editPerMinute = it }, label = { Text("Potongan Per Menit (Angka/Rp)") }, modifier = Modifier.fillMaxWidth())
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
                                        viewModel.availableDeductions.forEach { deduction ->
                                            val isChecked = viewModel.selectedDeductionIds.contains(deduction.id)

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
                                } else {
                                    // MODE LIHAT (READ-ONLY)
                                    val isWeekly = emp.payrollType == id.biz.yuka.libperformx.entity.PayrollType.WEEKLY
                                    DetailRowItem(Icons.Default.Schedule, "Sistem Penggajian", if (isWeekly) "Mingguan (Weekly)" else "Bulanan (Monthly)")
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

                                    if (emp.payPerDay > 0) {
                                        DetailRowItem(Icons.Default.PriceCheck, "Gaji Per Hari", formatRupiah(emp.payPerDay))
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                                    }

                                    DetailRowItem(Icons.Default.Payments, "Gaji Pokok", formatRupiah(emp.basicSalary))

                                    if (emp.transportAllowance > 0) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                                        DetailRowItem(Icons.Default.DirectionsCar, "Tunjangan Transport", formatRupiah(emp.transportAllowance))
                                    }
                                    if (emp.mealAllowance > 0) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                                        DetailRowItem(Icons.Default.Restaurant, "Tunjangan Makan", formatRupiah(emp.mealAllowance))
                                    }
                                    if (emp.positionAllowance > 0) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                                        DetailRowItem(Icons.Default.Work, "Tunjangan Jabatan", formatRupiah(emp.positionAllowance))
                                    }

                                    Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Total Tunjangan", fontWeight = FontWeight.Bold, color = WonderOrange)
                                            Text(formatRupiah(emp.totalAllowances), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                                        }
                                    }

                                    // Tampilkan Potongan Per Menit & Potongan Aktif
                                    if (emp.perMinute > 0) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
                                        DetailRowItem(
                                            icon = Icons.Default.MoneyOff,
                                            label = "Potongan Per Menit (Keterlambatan)",
                                            value = formatRupiah(emp.perMinute),
                                            valueColor = Color.Red.copy(alpha = 0.8f)
                                        )
                                    }

                                    if (emp.deductions.isNotEmpty()) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))

                                        val activeDeductionNames = emp.deductions
                                            .joinToString("\n") { deduction ->
                                                val amountText = if (deduction.percentageValue > 0.0) "${deduction.percentageValue}%" else formatRupiah(deduction.fixedAmount)
                                                "• ${deduction.name} ($amountText)"
                                            }
                                            .ifEmpty { "${emp.deductions.size} Potongan Aktif" }

                                        DetailRowItem(
                                            icon = Icons.Default.MoneyOff,
                                            label = "Potongan Gaji Aktif",
                                            value = activeDeductionNames,
                                            valueColor = Color.Red.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Pop Up Error Global jika terjadi kesalahan saat menonaktifkan karyawan / aksi lainnya
        GlobalErrorPopup(
            errorMessage = viewModel.errorMessage,
            onDismiss = { viewModel.errorMessage = null }
        )

        // ==========================================
        // BOTTOM SHEET: UBAH JABATAN (ROLE)
        // ==========================================
        if (viewModel.showRoleSheet) {
            ModalBottomSheet(
                onDismissRequest = { if (!viewModel.isUpdatingRole) viewModel.showRoleSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp)
                ) {
                    Text(
                        "Ubah Jabatan Karyawan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Pilih jabatan baru untuk ${emp?.fullName ?: "karyawan ini"}. Perubahan akan langsung berdampak pada hak akses aplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WonderGrey
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    var roleExpanded by remember { mutableStateOf(false) }
                    val selectedRoleName = viewModel.allRoles.find { it.id == viewModel.selectedRoleId }?.roleName?.uppercase() ?: "Pilih Jabatan"

                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRoleName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jabatan Baru") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedBorderColor = WonderOrange,
                                focusedLabelColor = WonderOrange
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            viewModel.allRoles.forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            role.roleName.uppercase(),
                                            fontWeight = if (role.id == viewModel.selectedRoleId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectedRoleId = role.id
                                        roleExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { viewModel.updateRole() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !viewModel.isUpdatingRole && viewModel.selectedRoleId != 0
                    ) {
                        if (viewModel.isUpdatingRole) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Simpan Jabatan Baru", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRowItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, valueColor: Color = Color.Black) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFF3E0)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = WonderOrange, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = WonderGrey, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor, lineHeight = 20.sp)
        }
    }
}