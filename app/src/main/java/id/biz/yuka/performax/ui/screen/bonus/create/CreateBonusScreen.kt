package id.biz.yuka.performax.ui.screen.bonus.create

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBonusScreen(
    onBack: () -> Unit,
    onSuccessReturn: () -> Unit,
    viewModel: CreateBonusViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val backgroundColor = Color(0xFFF9F9F9)

    // Form States
    var selectedEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Menangkap sukses dari ViewModel
    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) {
            Toast.makeText(context, "Bonus berhasil diberikan!", Toast.LENGTH_SHORT).show()
            onSuccessReturn() // Kembali ke layar sebelumnya
        }
    }

    // Menangkap pesan error dari ViewModel
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.errorMessage = null // reset setelah ditampilkan
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                    Text(
                        text = "Tambah Bonus Karyawan",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // INFO BANNER
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Bonus yang ditambahkan akan otomatis diakumulasikan ke dalam Slip Gaji / Payroll karyawan bersangkutan.",
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 1. DROPDOWN KARYAWAN
            Text("Pilih Karyawan", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = selectedEmployee?.fullName ?: "Pilih...",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    if (viewModel.employees.isEmpty() && viewModel.isLoading) {
                        DropdownMenuItem(
                            text = { Text("Memuat data...") },
                            onClick = { }
                        )
                    } else if (viewModel.employees.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Tidak ada karyawan.") },
                            onClick = { expandedDropdown = false }
                        )
                    } else {
                        viewModel.employees.forEach { employee ->
                            DropdownMenuItem(
                                text = { Text(employee.fullName) },
                                onClick = {
                                    selectedEmployee = employee
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. INPUT NOMINAL
            Text("Nominal Bonus (Rp)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contoh: 500000") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = WonderOrange) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. INPUT CATATAN
            Text("Keterangan / Alasan Bonus", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Contoh: Bonus target penjualan tercapai.") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = WonderGrey) },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 4. TOMBOL SUBMIT
            Button(
                onClick = {
                    viewModel.submitBonus(
                        selectedEmployee = selectedEmployee,
                        amountStr = amountText,
                        notes = notesText
                    )
                },
                enabled = !viewModel.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Warna hijau sukses
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Simpan Bonus Karyawan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}