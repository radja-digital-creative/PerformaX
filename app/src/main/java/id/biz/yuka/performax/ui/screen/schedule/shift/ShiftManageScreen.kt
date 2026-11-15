package id.biz.yuka.performax.ui.screen.schedule.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManageScreen(
    onBack: () -> Unit,
    viewModel: ShiftManageViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Efek ketika aksi simpan atau hapus berhasil dilakukan
    LaunchedEffect(viewModel.successEvent.value) {
        if (viewModel.successEvent.value) {
            onBack()
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isEditMode) "Ubah Detail Shift" else "Tambah Shift Baru",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Hanya munculkan tombol hapus jika dalam mode Edit
                    if (viewModel.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Shift",
                                tint = Color.Red
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.saveShift() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !viewModel.isLoading && viewModel.shiftName.isNotBlank()
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Save, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isEditMode) "Simpan Perubahan" else "Konfirmasi Tambah",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- ALERT ERROR ERROR (JIKA ADA) ---
            if (!viewModel.errorMessage.isNullOrBlank()) {
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.errorMessage!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // --- INPUT NAMA SHIFT ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nama Shift *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = viewModel.shiftName,
                    onValueChange = { viewModel.shiftName = it },
                    placeholder = { Text("Contoh: Shift Pagi Reguler, Shift Malam") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WonderOrange,
                        cursorColor = WonderOrange
                    ),
                    singleLine = true
                )
            }

            // --- INPUT JAM MASUK & KELUAR BERSEBELAHAN ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jam Masuk *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = viewModel.startTime,
                        onValueChange = { viewModel.startTime = it },
                        placeholder = { Text("08:00:00") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WonderOrange),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jam Keluar *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = viewModel.endTime,
                        onValueChange = { viewModel.endTime = it },
                        placeholder = { Text("17:00:00") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WonderOrange),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }
            }
            Text(
                text = "Format waktu wajib menggunakan HH:mm:ss (Contoh: 08:30:00)",
                style = MaterialTheme.typography.labelSmall,
                color = WonderGrey
            )

            // --- INPUT RADIUS ABSENSI ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Radius Toleransi Absensi (Meter) *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = viewModel.radius,
                    onValueChange = { viewModel.radius = it },
                    placeholder = { Text("50") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WonderOrange),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
                Text(
                    text = "Jarak radius maksimal dalam meter dari titik koordinat toko agar karyawan bisa melakukan check-in.",
                    style = MaterialTheme.typography.labelSmall,
                    color = WonderGrey
                )
            }

            // --- TOGGLE STATUS SHIFT ---
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Status Shift Aktif", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "Jika dinonaktifkan, shift tidak akan muncul saat manajer membuat jadwal baru.",
                            style = MaterialTheme.typography.labelSmall,
                            color = WonderGrey
                        )
                    }
                    Switch(
                        checked = viewModel.isActive,
                        onCheckedChange = { viewModel.isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WonderOrange
                        )
                    )
                }
            }
        }
    }

    // --- DIALOG POPUP KONFIRMASI HAPUS SHIFT ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Shift?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Aksi ini tidak bisa dibatalkan. Sistem otomatis memeriksa apakah shift ini masih dipakai oleh jadwal aktif karyawan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteShift()
                    }
                ) {
                    Text("Hapus", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}