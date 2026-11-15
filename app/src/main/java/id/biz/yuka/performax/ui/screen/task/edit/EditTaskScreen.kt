package id.biz.yuka.performax.ui.screen.task.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.screen.task.create.TaskAttachment
import id.biz.yuka.performax.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTaskScreen(
    onBack: () -> Unit,
    onSuccessReturn: () -> Unit,
    viewModel: EditTaskViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showEmployeePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        viewModel.addNewAttachments(context, uris)
    }

    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            onSuccessReturn()
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Edit Tugas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            // Sembunyikan tombol jika tidak berhak edit
            if (viewModel.isAuthorized && !viewModel.isVerifiedLocked && !viewModel.isInitialLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.submitUpdate(context) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Simpan Perubahan", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // 1. STATE LOADING
            if (viewModel.isInitialLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = WonderOrange)
            }
            // 2. STATE DITOLAK (BUKAN CREATOR ATAU SUDAH VERIFIED)
            else if (!viewModel.isAuthorized || viewModel.isVerifiedLocked) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (viewModel.isVerifiedLocked) Icons.Default.Verified else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (viewModel.isVerifiedLocked) Color(0xFF4CAF50) else Color.Red,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (viewModel.isVerifiedLocked) "Tugas Telah Diverifikasi" else "Akses Ditolak",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (viewModel.isVerifiedLocked) "Tugas yang sudah diverifikasi selesai tidak dapat diubah kembali." else "Hanya pembuat tugas (Creator) yang diizinkan untuk mengubah data tugas ini.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
            // 3. STATE AMAN (FORM EDIT TAMPIL)
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewModel.errorMessage != null) {
                        item {
                            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(viewModel.errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // INFORMASI DASAR
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Informasi Dasar", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = viewModel.title, onValueChange = { viewModel.title = it },
                                    label = { Text("Judul Tugas") }, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp), singleLine = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = viewModel.description, onValueChange = { viewModel.description = it },
                                    label = { Text("Deskripsi (Opsional)") }, modifier = Modifier.fillMaxWidth().height(120.dp),
                                    shape = RoundedCornerShape(12.dp), maxLines = 4
                                )
                            }
                        }
                    }

                    // LABEL
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Label Tugas (Wajib)", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))

                                if (viewModel.availableLabels.isEmpty()) {
                                    Text("Memuat label...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(viewModel.availableLabels) { label ->
                                            val isSelected = viewModel.selectedLabels.contains(label)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.toggleLabel(label) },
                                                label = { Text(label.name) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = WonderOrange.copy(alpha = 0.2f),
                                                    selectedLabelColor = WonderOrange,
                                                    containerColor = Color(0xFFF5F5F5)
                                                ),
                                                border = if (isSelected) BorderStroke(1.dp, WonderOrange) else null,
                                                shape = RoundedCornerShape(50)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // PENGATURAN
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Pengaturan", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))

                                val assigneeCount = viewModel.selectedAssignees.size
                                val assigneeText = when {
                                    assigneeCount == 0 -> "Pilih Karyawan"
                                    assigneeCount == 1 -> viewModel.selectedAssignees.first().fullName
                                    else -> "${viewModel.selectedAssignees.first().fullName} +${assigneeCount - 1} Orang"
                                }

                                OutlinedTextField(
                                    value = assigneeText, onValueChange = { }, label = { Text("Diberikan Kepada") },
                                    modifier = Modifier.fillMaxWidth().clickable { showEmployeePicker = true },
                                    readOnly = true, enabled = false, shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Gray, disabledTrailingIconColor = Color.Gray),
                                    trailingIcon = { Icon(Icons.Default.Group, contentDescription = null) }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale("id", "ID"))
                                val dateText = viewModel.deadline?.format(dateFormatter) ?: "Pilih Tenggat Waktu"

                                OutlinedTextField(
                                    value = dateText, onValueChange = { }, label = { Text("Tenggat Waktu (Deadline)") },
                                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                                    readOnly = true, enabled = false, shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Gray, disabledTrailingIconColor = Color.Gray),
                                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                                )
                            }
                        }
                    }

                    // LAMPIRAN (LAMA & BARU)
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Lampiran", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = WonderOrange)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tambah", color = WonderOrange, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (viewModel.existingAttachments.isEmpty() && viewModel.newAttachments.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                        Text("Belum ada lampiran", color = WonderGrey, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Render Lampiran Lama
                    items(viewModel.existingAttachments) { attachment ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(12.dp)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(WonderBg), contentAlignment = Alignment.Center) {
                                AsyncImage(model = attachment.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gambar Terunggah", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("Tersimpan di Cloud", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                            }
                            IconButton(onClick = { viewModel.removeExistingAttachment(attachment) }) { Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.7f)) }
                        }
                    }

                    // Render Lampiran Baru
                    items(viewModel.newAttachments) { attachment ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(12.dp)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(WonderBg), contentAlignment = Alignment.Center) {
                                if (attachment.isImage) AsyncImage(model = attachment.uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(attachment.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${attachment.size / 1024} KB (Baru)", style = MaterialTheme.typography.labelSmall, color = WonderOrange)
                            }
                            IconButton(onClick = { viewModel.removeNewAttachment(attachment) }) { Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.7f)) }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }

    // Modal DatePicker
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.deadline = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Pilih", color = WonderOrange, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal", color = Color.Gray) } }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(selectedDayContainerColor = WonderOrange, todayDateBorderColor = WonderOrange, todayContentColor = WonderOrange)
            )
        }
    }

    // Modal Employee Picker
    if (showEmployeePicker) {
        ModalBottomSheet(onDismissRequest = { showEmployeePicker = false }, containerColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pilih Karyawan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showEmployeePicker = false }) { Text("Selesai", color = WonderOrange, fontWeight = FontWeight.Bold) }
                }
                HorizontalDivider(color = Color(0xFFF0F0F0))
                if (viewModel.employees.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Belum ada data karyawan", color = Color.Gray) }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(viewModel.employees) { employee ->
                            val isSelected = viewModel.selectedAssignees.contains(employee)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleAssignee(employee) }.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = { viewModel.toggleAssignee(employee) }, colors = CheckboxDefaults.colors(checkedColor = WonderOrange))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(employee.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text(employee.roleName, color = WonderGrey, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }
            }
        }
    }
}