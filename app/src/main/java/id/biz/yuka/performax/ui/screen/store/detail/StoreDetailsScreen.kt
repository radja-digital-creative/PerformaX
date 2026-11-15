package id.biz.yuka.performax.ui.screen.store.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
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
fun StoreDetailsScreen(
    onBack: () -> Unit,
    viewModel: StoreDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showFullscreenMap by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            delay(2500)
            viewModel.successMessage = null
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Toko", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (viewModel.successMessage != null) {
                    item {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(viewModel.successMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (viewModel.errorMessage != null) {
                    item {
                        Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                
                if (viewModel.isEditable) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Informasi & Lokasi", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { viewModel.updateStoreInfo() }, enabled = !viewModel.isSaving) {
                                        Icon(Icons.Default.Save, contentDescription = "Simpan Info", tint = WonderOrange)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(value = viewModel.storeName, onValueChange = { viewModel.storeName = it }, label = { Text("Nama Toko/Cabang") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.storePhone, onValueChange = { viewModel.storePhone = it }, label = { Text("Nomor Telepon") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.storeAddress, onValueChange = { viewModel.storeAddress = it }, label = { Text("Alamat Lengkap") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Peta Preview
                                Text("Titik Lokasi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray)
                                ) {
                                    LeafletMap(
                                        markers = if (viewModel.latitude.isNotBlank() && viewModel.longitude.isNotBlank()) {
                                            listOf(MapMarker(viewModel.latitude.toDoubleOrNull() ?: 0.0, viewModel.longitude.toDoubleOrNull() ?: 0.0, viewModel.storeName))
                                        } else emptyList(),
                                        isInteractive = false // Matikan interaksi agar tidak bentrok dengan scroll
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = viewModel.latitude, onValueChange = { viewModel.latitude = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                    OutlinedTextField(value = viewModel.longitude, onValueChange = { viewModel.longitude = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (viewModel.latitude.isNotBlank() && viewModel.longitude.isNotBlank()) {
                                            val gmmIntentUri = Uri.parse("geo:${viewModel.latitude},${viewModel.longitude}?q=${Uri.encode(viewModel.storeName)}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            context.startActivity(mapIntent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Open in Google Maps", color = Color.White)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Status Operasional", fontWeight = FontWeight.Bold)
                                        Text(if (viewModel.isActive) "Toko sedang aktif" else "Toko dinonaktifkan", style = MaterialTheme.typography.bodySmall, color = WonderGrey)
                                    }
                                    Switch(checked = viewModel.isActive, onCheckedChange = { viewModel.isActive = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WonderOrange))
                                }
                            }
                        }
                    }

                    // KARTU 2: ATURAN ABSENSI
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Aturan Waktu Absensi", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { viewModel.updateAttendanceSettings() }, enabled = !viewModel.isSaving) {
                                        Icon(Icons.Default.Save, contentDescription = "Simpan Absensi", tint = WonderOrange)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.openBeforeMinutes, onValueChange = { viewModel.openBeforeMinutes = it }, label = { Text("Absen masuk lebih awal (menit)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.lateToleranceMinutes, onValueChange = { viewModel.lateToleranceMinutes = it }, label = { Text("Toleransi terlambat (menit)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.overtimeThresholdMinutes, onValueChange = { viewModel.overtimeThresholdMinutes = it }, label = { Text("Ambang batas lembur (menit)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = viewModel.dailyCloseTime, onValueChange = { viewModel.dailyCloseTime = it }, label = { Text("Jam tutup harian (Misal: 22:00:00)") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                } else {
                    // MODE READ-ONLY
                    item {
                        Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(220.dp), shadowElevation = 2.dp) {
                            LeafletMap(
                                markers = listOf(MapMarker(viewModel.latitude.toDoubleOrNull() ?: 0.0, viewModel.longitude.toDoubleOrNull() ?: 0.0, viewModel.storeName)),
                                isInteractive = true
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                if (viewModel.latitude.isNotBlank() && viewModel.longitude.isNotBlank()) {
                                    val gmmIntentUri = Uri.parse("geo:${viewModel.latitude},${viewModel.longitude}?q=${Uri.encode(viewModel.storeName)}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(mapIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Open in Google Maps", color = Color.White) }
                    }
                    item {
                        InfoCard("Informasi Cabang") {
                            ReadOnlyDetail("Nama Cabang", viewModel.storeName)
                            ReadOnlyDetail("Alamat", viewModel.storeAddress.ifBlank { "-" })
                            ReadOnlyDetail("No. Telepon", viewModel.storePhone.ifBlank { "-" })
                            ReadOnlyDetail("Status", if (viewModel.isActive) "BEROPERASI" else "DITUTUP", if (viewModel.isActive) Color(0xFF2E7D32) else Color.Red)
                        }
                    }
                    item {
                        InfoCard("Aturan Jam Kerja") {
                            ReadOnlyDetail("Absen Masuk Lebih Awal", "${viewModel.openBeforeMinutes} menit")
                            ReadOnlyDetail("Toleransi Terlambat", "${viewModel.lateToleranceMinutes} menit")
                            ReadOnlyDetail("Ambang Batas Lembur", "${viewModel.overtimeThresholdMinutes} menit")
                            ReadOnlyDetail("Jam Tutup Harian", viewModel.dailyCloseTime)
                        }
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

@Composable
fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun ReadOnlyDetail(label: String, value: String, valueColor: Color = Color.Black) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = WonderGrey)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
