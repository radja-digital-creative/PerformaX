package id.biz.yuka.performax.ui.screen.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.NotificationEntity
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifikasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = WonderBg
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && notifications.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = WonderOrange)
            } else if (!errorMessage.isNullOrEmpty()) {
                Text(
                    text = errorMessage ?: "Terjadi kesalahan",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = WonderGrey.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Belum ada notifikasi", color = WonderGrey)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItemCard(
                            notification = notification,
                            onClick = {
                                if (!notification.isRead) {
                                    viewModel.markAsRead(notification.id)
                                }
                                
                                // ✅ PARSE PAYLOAD UNTUK NAVIGASI
                                notification.payload?.let { payloadStr ->
                                    try {
                                        val json = Json.parseToJsonElement(payloadStr).jsonObject
                                        val type = json["type"]?.jsonPrimitive?.content
                                        
                                        if (type == "task") {
                                            val taskId = json["task_id"]?.jsonPrimitive?.content
                                            if (!taskId.isNullOrBlank()) {
                                                onNavigateToTaskDetail(taskId)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(notification: NotificationEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        // Latar belakang diatur ke Putih murni agar terlihat bersih & elegan
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        // Menghilangkan bayangan tebal yang membuat desain terlihat "kotor"
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // Menggunakan garis tepi (border) tipis berwarna abu-abu muda sebagai ganti bayangan
        border = BorderStroke(1.dp, WonderGrey.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Indikator Titik Oranye untuk Notifikasi Belum Dibaca
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 12.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(WonderOrange)
                )
            } else {
                Spacer(modifier = Modifier.width(20.dp)) // Menjaga jarak teks agar sejajar
            }

            Column(modifier = Modifier.weight(1f)) {
                // 1. JUDUL NOTIFIKASI
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. HARI, TANGGAL, & JAM FORMAT INDONESIA
                Text(
                    text = formatToIndonesianDateTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = WonderGrey.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. PESAN (MESSAGE)
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WonderGrey,
                    lineHeight = 20.sp // Memberikan jarak antar baris agar nyaman dibaca
                )
            }
        }
    }
}

fun formatToIndonesianDateTime(dateString: String): String {
    return try {
        // Contoh data Supabase: "2026-07-02T16:31:51.67218+00:00"
        // Kita potong langsung 19 karakter pertama saja lalu ubah "T" jadi spasi
        // Hasilnya pasti dan aman: "2026-07-02 16:31:51"
        val cleanString = if (dateString.length >= 19) {
            dateString.substring(0, 19).replace("T", " ")
        } else {
            dateString.replace("T", " ") // Jaga-jaga jika formatnya lebih pendek
        }

        // 1. Format membaca data (Anggap sebagai waktu UTC/GMT dari server)
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val parsedDate = inputFormat.parse(cleanString)

        // 2. Format menulis data (Ubah ke bahasa Indonesia & Waktu HP Lokal)
        val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale("id", "ID"))
        outputFormat.timeZone = TimeZone.getDefault() // Otomatis menyesuaikan WIB/WITA/WIT HP

        parsedDate?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        // Jika masih gagal, setidaknya tidak crash
        dateString
    }
}