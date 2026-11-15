package id.biz.yuka.performax.ui.screen.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.components.LeafletMap
import id.biz.yuka.performax.ui.components.MapMarker
import id.biz.yuka.performax.ui.theme.*
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val attendance = viewModel.getSelectedAttendance()
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Info absensi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.loadAttendances() },
            state = pullToRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DateFilterRow(
                    dates = viewModel.dateList,
                    selectedDate = viewModel.selectedDate,
                    onDateSelected = { viewModel.updateSelectedDate(it) }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (attendance != null) {
                        item {
                            val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                            Text(
                                text = viewModel.selectedDate.format(formatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // 1. FOTO CHECK-IN & CHECK-OUT
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                AttendancePhotoCard(
                                    label = "Foto Masuk",
                                    imageUrl = attendance.checkInImage,
                                    modifier = Modifier.weight(1f)
                                ) { fullscreenImageUrl = it }

                                AttendancePhotoCard(
                                    label = "Foto Keluar",
                                    imageUrl = attendance.checkOutImage,
                                    modifier = Modifier.weight(1f)
                                ) { fullscreenImageUrl = it }
                            }
                        }

                        // 2. JAM & STATUS (Dengan Konversi Zona Waktu Otomatis)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // ✅ PERBAIKAN: Menggunakan fungsi konversi UTC -> WIB (+7)
                                    val inTime = formatTimeWIB(attendance.checkInTime)
                                    val outTime = formatTimeWIB(attendance.checkOutTime)

                                    AttendanceTimeDetail(
                                        label = "Jam Masuk",
                                        time = inTime,
                                        status = attendance.checkInStatus ?: "-",
                                        modifier = Modifier.weight(1f)
                                    )
                                    VerticalDivider(modifier = Modifier.height(40.dp))
                                    AttendanceTimeDetail(
                                        label = "Jam Keluar",
                                        time = outTime,
                                        status = attendance.checkOutStatus ?: "Belum Keluar",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // 3. MAPS (Pusat di Kantor, Titik di Check-in/Check-out)
                        item {
                            Column {
                                Text("Lokasi Absensi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                if (viewModel.storeName.isNotBlank()) {
                                    Text(viewModel.storeName, style = MaterialTheme.typography.bodySmall, color = WonderGrey)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(modifier = Modifier.fillMaxWidth().height(250.dp), shape = RoundedCornerShape(16.dp)) {

                                // Ambil data koordinat
                                val inLat = attendance.checkInLatitude
                                val inLng = attendance.checkInLongitude
                                val outLat = attendance.checkOutLatitude
                                val outLng = attendance.checkOutLongitude

                                val storeLat = viewModel.storeLat ?: -6.2000
                                val storeLng = viewModel.storeLng ?: 106.8166
                                val storeLabel = viewModel.storeName

                                val markers = mutableListOf<MapMarker>()
                                var centerMarker: MapMarker? = null // Penentu pusat kamera peta

                                if (storeLat != 0.0) {
                                    val storeMarker = MapMarker(storeLat, storeLng, storeLabel.ifBlank { "Toko/Cabang" })
                                    markers.add(storeMarker)
                                    centerMarker = storeMarker
                                }

                                if (inLat != 0.0) {
                                    val inMarker = MapMarker(inLat, inLng, "📍 Titik Masuk")
                                    markers.add(inMarker)
                                    // Jika koordinat toko belum dimuat, jadikan titik masuk sebagai pusat kamera
                                    if (centerMarker == null) centerMarker = inMarker
                                }

                                if (outLat != 0.0) {
                                    val outMarker = MapMarker(outLat, outLng, "🚩 Titik Keluar")
                                    markers.add(outMarker)
                                }

                                LeafletMap(
                                    modifier = Modifier.fillMaxSize(),
                                    markers = markers,
                                    radiusInMeters = 50.0,
                                    circleCenter = MapMarker(storeLat, storeLng),
                                    zoomLevel = 18
                                )
                            }
                        }
                    } else {
                        item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("Data tidak tersedia.", color = WonderGrey) } }
                    }
                }
            }
        }
    }

    if (fullscreenImageUrl != null) {
        Dialog(onDismissRequest = { fullscreenImageUrl = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(model = fullscreenImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { fullscreenImageUrl = null }, modifier = Modifier.padding(16.dp)) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }
    }
}

// ✅ FUNGSI HELPER: Konversi UTC dari Supabase ke WIB (+7)
private fun formatTimeWIB(timeStr: String?): String {
    if (timeStr.isNullOrBlank() || timeStr == "-") return "-"
    return try {
        // Coba baca jika formatnya ISO Standar (Contoh: "2026-05-20T01:09:00Z" atau "+00:00")
        val parsedTime = OffsetDateTime.parse(timeStr)
        val wibTime = parsedTime.atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        wibTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        try {
            // Coba baca jika formatnya SQL biasa tanpa T/Z (Contoh: "2026-05-20 01:09:00")
            val cleanStr = timeStr.substringBefore(".")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localTime = LocalDateTime.parse(cleanStr, formatter)

            // Asumsikan data dari server adalah UTC
            val wibTime = localTime.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Asia/Jakarta"))
            wibTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e2: Exception) {
            // Fallback Ekstrem: Jika format sangat berantakan, potong saja angkanya
            val t = timeStr
            if (t.contains("T")) t.substringAfter("T").take(5)
            else if (t.contains(" ")) t.substringAfter(" ").take(5)
            else t.take(5)
        }
    }
}

@Composable
fun DateFilterRow(dates: List<java.time.LocalDate>, selectedDate: java.time.LocalDate, onDateSelected: (java.time.LocalDate) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("id", "ID"))
    LazyRow(modifier = Modifier.fillMaxWidth().background(WonderBg).padding(vertical = 12.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            FilterChip(
                selected = isSelected,
                onClick = { onDateSelected(date) },
                label = { Text(date.format(formatter)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WonderLime, selectedLabelColor = Color.Black, containerColor = Color.White),
                border = BorderStroke(1.dp, if (isSelected) WonderLime else Color.LightGray),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun AttendancePhotoCard(label: String, imageUrl: String?, modifier: Modifier = Modifier, onImageClick: (String) -> Unit) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)).clickable(enabled = !imageUrl.isNullOrBlank()) { imageUrl?.let { onImageClick(it) } },
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNullOrBlank()) {
                Icon(Icons.Default.ImageNotSupported, null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
            } else {
                AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

@Composable
fun AttendanceTimeDetail(label: String, time: String, status: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
        Text(text = time, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(color = if (status.lowercase().contains("tepat")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp)) {
            Text(
                text = status,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (status.lowercase().contains("tepat")) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}