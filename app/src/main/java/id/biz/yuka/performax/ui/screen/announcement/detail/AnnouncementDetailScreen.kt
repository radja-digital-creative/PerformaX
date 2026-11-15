package id.biz.yuka.performax.ui.screen.announcement.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailScreen(
    onBack: () -> Unit,
    viewModel: AnnouncementDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val ann = viewModel.announcement
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.newImageUri = uri
    }

    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            delay(2000)
            viewModel.successMessage = null
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) "Edit Pengumuman" else "Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (viewModel.isEditMode) viewModel.isEditMode = false else onBack() }) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    if (viewModel.isOwnerOrAdmin && !viewModel.isEditMode && ann != null) {
                        IconButton(onClick = { viewModel.isEditMode = true }) {
                            Icon(Icons.Default.Edit, "Edit", tint = WonderOrange)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Hapus", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            if (viewModel.isEditMode) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp)) {
                    Button(
                        onClick = { viewModel.updateAnnouncement(context) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (viewModel.isLoading && ann == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderOrange)
            }
        } else if (ann != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (viewModel.errorMessage != null) {
                    item {
                        Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                
                if (viewModel.successMessage != null) {
                    item {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(viewModel.successMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (viewModel.isEditMode) {
                    // --- FORM EDIT ---
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Informasi Dasar", fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(value = viewModel.editTitle, onValueChange = { viewModel.editTitle = it }, label = { Text("Judul") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = viewModel.editContent, onValueChange = { viewModel.editContent = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth().height(150.dp))
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Gambar (Opsional)", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (viewModel.newImageUri != null || !ann.imageUrl.isNullOrBlank()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))) {
                                        AsyncImage(
                                            model = viewModel.newImageUri ?: ann.imageUrl,
                                            contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                                        )
                                        IconButton(onClick = { 
                                            if (viewModel.newImageUri != null) viewModel.newImageUri = null
                                            // logic delete existing image will be handled by passing null in update
                                        }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                                            Icon(Icons.Default.Close, null, tint = Color.White)
                                        }
                                    }
                                }
                                
                                TextButton(onClick = { photoLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.AddPhotoAlternate, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ganti Gambar")
                                }
                            }
                        }
                    }
                    
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Status & Jadwal", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = viewModel.editIsActive, onCheckedChange = { viewModel.editIsActive = it }, colors = CheckboxDefaults.colors(checkedColor = WonderOrange))
                                    Text("Aktif Ditayangkan")
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = viewModel.editStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        onValueChange = {}, readOnly = true, label = { Text("Mulai") },
                                        modifier = Modifier.weight(1f).clickable { showStartDatePicker = true }, enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Gray)
                                    )
                                    OutlinedTextField(
                                        value = viewModel.editEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        onValueChange = {}, readOnly = true, label = { Text("Berakhir") },
                                        modifier = Modifier.weight(1f).clickable { showEndDatePicker = true }, enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Gray)
                                    )
                                }
                            }
                        }
                    }

                } else {
                    // --- VIEW MODE ---
                    item {
                        if (!ann.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ann.imageUrl, contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(ann.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = WonderGrey)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ditayangkan: ${ann.startAt.take(10)} s/d ${ann.endAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = WonderGrey)
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
                                
                                Text(ann.content ?: "Tidak ada deskripsi tambahan.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Ditujukan Ke", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (ann.storeIds.isEmpty()) {
                                    Text("Seluruh Cabang", color = Color.Gray)
                                } else {
                                    // Tampilkan toko yang dipilih (Untuk simplifikasi, tampilkan jumlah)
                                    Text("${ann.storeIds.size} Toko terpilih", color = WonderOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Pengumuman?") },
            text = { Text("Data yang dihapus tidak dapat dikembalikan.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAnnouncement { onBack() } }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
        )
    }

    if (showStartDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = viewModel.editStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showStartDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { viewModel.editStartDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                showStartDatePicker = false
            }) { Text("Pilih") }
        }) { DatePicker(state = state) }
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = viewModel.editEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { viewModel.editEndDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                showEndDatePicker = false
            }) { Text("Pilih") }
        }) { DatePicker(state = state) }
    }
}
