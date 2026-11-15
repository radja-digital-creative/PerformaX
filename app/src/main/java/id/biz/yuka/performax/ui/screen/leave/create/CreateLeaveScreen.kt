package id.biz.yuka.performax.ui.screen.leave.create

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLeaveScreen(
    onBack: () -> Unit,
    onSuccessReturn: () -> Unit,
    viewModel: CreateLeaveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.capturedImage = bitmap
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) onSuccessReturn()
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Ajukan Izin Baru", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.submitLeave() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !viewModel.isLoading
                ) {
                    // Teks berubah saat loading
                    Text(
                        text = if (viewModel.isLoading) "Memproses Pengajuan..." else "Kirim Pengajuan",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ✅ LOADING LINE BAR (SELARAS DENGAN TASK SCREEN)
            if (viewModel.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = WonderOrange,
                    trackColor = WonderLime
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                if (viewModel.errorMessage != null) {
                    item { Text(viewModel.errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            OutlinedTextField(
                                value = viewModel.selectedType?.name ?: "Pilih Jenis Izin",
                                onValueChange = { },
                                label = { Text("Jenis Izin / Cuti") },
                                modifier = Modifier.fillMaxWidth().clickable { showTypePicker = true },
                                readOnly = true, enabled = false,
                                trailingIcon = { Icon(Icons.Default.List, null) },
                                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val dateStr = if (viewModel.startDate != null && viewModel.endDate != null) {
                                "${viewModel.startDate!!.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} - ${viewModel.endDate!!.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
                            } else "Pilih Rentang Tanggal"

                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = { },
                                label = { Text("Tanggal Izin") },
                                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                                readOnly = true, enabled = false,
                                trailingIcon = { Icon(Icons.Default.DateRange, null) },
                                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = viewModel.reason,
                                onValueChange = { viewModel.reason = it },
                                label = { Text("Alasan Lengkap") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Lampiran / Bukti (Optional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = WonderGrey
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (viewModel.capturedImage == null) {
                                OutlinedButton(
                                    onClick = {
                                        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                        if (status == PackageManager.PERMISSION_GRANTED) {
                                            cameraLauncher.launch(null)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WonderOrange)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddAPhoto, null)
                                        Text("Ambil Foto Bukti", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.LightGray)
                                ) {
                                    Image(
                                        bitmap = viewModel.capturedImage!!.asImageBitmap(),
                                        contentDescription = "Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.capturedImage = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Pilih Tanggal
    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = viewModel.startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = viewModel.endDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )

        ModalBottomSheet(onDismissRequest = { showDatePicker = false }, containerColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pilih Rentang Tanggal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(onClick = {
                        val start = datePickerState.selectedStartDateMillis
                        val end = datePickerState.selectedEndDateMillis ?: start // Jika hanya 1 tgl, anggap start = end

                        if (start != null) {
                            viewModel.startDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.endDate = Instant.ofEpochMilli(end!!).atZone(ZoneId.systemDefault()).toLocalDate()
                            showDatePicker = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)) { Text("Pilih", color = Color.Black) }
                }
                DateRangePicker(state = datePickerState, modifier = Modifier.height(400.dp), title = null, headline = null, showModeToggle = false)
            }
        }
    }

    // Modal Pilih Jenis Izin
    if (showTypePicker) {
        ModalBottomSheet(onDismissRequest = { showTypePicker = false }, containerColor = Color.White) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                item { 
                    Text("Pilih Jenis Izin", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) 
                }

                if (viewModel.isDataLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = WonderOrange)
                        }
                    }
                } else {
                    items(viewModel.leaveTypes) { type ->
                        val (isLocked, lockReason) = viewModel.isTypeLocked(type)
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isLocked) {
                                    viewModel.selectedType = type
                                    showTypePicker = false
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = type.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (isLocked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                ),
                                color = if (isLocked) Color.Gray else Color.Black
                            )
                            if (isLocked && lockReason != null) {
                                Text(
                                    text = lockReason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Red.copy(alpha = 0.7f)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }
            }
        }
    }
}