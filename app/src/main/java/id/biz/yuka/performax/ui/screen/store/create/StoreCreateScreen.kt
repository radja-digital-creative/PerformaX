package id.biz.yuka.performax.ui.screen.store.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.components.LeafletMap
import id.biz.yuka.performax.ui.components.MapMarker
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCreateScreen(
    onBack: () -> Unit,
    viewModel: StoreCreateViewModel = hiltViewModel()
) {
    var showFullscreenMap by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            delay(1500)
            onBack()
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Tambah Cabang Baru", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp)) {
                Button(
                    onClick = { viewModel.createNewStore() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    else Text("Simpan Cabang", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifikasi Sukses / Error
            if (viewModel.successMessage != null) {
                item { Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(viewModel.successMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) } }
            }
            if (viewModel.errorMessage != null) {
                item { Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp)) } }
            }

            // KARTU 1: INFO DASAR
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Informasi Cabang", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = viewModel.storeName, onValueChange = { viewModel.storeName = it }, label = { Text("Nama Toko/Cabang") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = viewModel.storePhone, onValueChange = { viewModel.storePhone = it }, label = { Text("Nomor Telepon") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = viewModel.storeAddress, onValueChange = { viewModel.storeAddress = it }, label = { Text("Alamat Lengkap") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    }
                }
            }

            // KARTU 2: LOKASI
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Lokasi Titik Absen (GPS)", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray)
                        ) {
                            LeafletMap(
                                markers = if (viewModel.latitude.isNotBlank() && viewModel.longitude.isNotBlank()) {
                                    listOf(MapMarker(viewModel.latitude.toDoubleOrNull() ?: 0.0, viewModel.longitude.toDoubleOrNull() ?: 0.0, "Titik Cabang"))
                                } else emptyList(),
                                isInteractive = false
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showFullscreenMap = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Update Lokasi di Peta", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = viewModel.latitude, onValueChange = { viewModel.latitude = it }, label = { Text("Latitude") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = viewModel.longitude, onValueChange = { viewModel.longitude = it }, label = { Text("Longitude") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }

            // KARTU 3: PENGATURAN ABSENSI
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Aturan Waktu Absensi", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Text("Default parameter untuk cabang ini", style = MaterialTheme.typography.bodySmall, color = WonderGrey)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = viewModel.openBeforeMinutes, onValueChange = { viewModel.openBeforeMinutes = it }, label = { Text("Bisa absen masuk lebih awal (menit)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = viewModel.lateToleranceMinutes, onValueChange = { viewModel.lateToleranceMinutes = it }, label = { Text("Toleransi terlambat (menit)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = viewModel.overtimeThresholdMinutes, onValueChange = { viewModel.overtimeThresholdMinutes = it }, label = { Text("Ambang batas lembur (menit)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = viewModel.dailyCloseTime, onValueChange = { viewModel.dailyCloseTime = it }, label = { Text("Jam tutup harian (Misal: 22:00:00)") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    // FULLSCREEN MAP PICKER
    if (showFullscreenMap) {
        var tempLat by remember { mutableStateOf(viewModel.latitude) }
        var tempLng by remember { mutableStateOf(viewModel.longitude) }

        Dialog(
            onDismissRequest = { showFullscreenMap = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LeafletMap(
                        markers = if (tempLat.isNotBlank() && tempLng.isNotBlank()) {
                            listOf(MapMarker(tempLat.toDoubleOrNull() ?: 0.0, tempLng.toDoubleOrNull() ?: 0.0, "Titik Baru"))
                        } else emptyList(),
                        onMapClick = { lat, lng ->
                            tempLat = lat.toString()
                            tempLng = lng.toString()
                        },
                        isInteractive = true,
                        zoomLevel = 18
                    )

                    // Toolbar Atas
                    Surface(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = Color.White.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { showFullscreenMap = false }) {
                                Icon(Icons.Default.Close, "Batal")
                            }
                            Text("Pilih Lokasi Toko", fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                viewModel.latitude = tempLat
                                viewModel.longitude = tempLng
                                showFullscreenMap = false
                            }) {
                                Text("Simpan", fontWeight = FontWeight.Black, color = WonderOrange)
                            }
                        }
                    }
                    
                    // Info Floating
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).navigationBarsPadding(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ketuk pada peta untuk menentukan posisi presisi", style = MaterialTheme.typography.bodySmall)
                            if (tempLat.isNotBlank()) {
                                Text("$tempLat, $tempLng", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WonderGrey)
                            }
                        }
                    }
                }
            }
        }
    }
}
