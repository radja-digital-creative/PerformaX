package id.biz.yuka.performax.ui.screen.task.settings.urgent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskUrgentDetailScreen(
    onBack: () -> Unit,
    viewModel: TaskUrgentDetailViewModel = hiltViewModel()
) {
    val reminder = viewModel.urgentReminder
    val task = viewModel.taskTemplate

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Detail Pengingat Urgent", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        if (viewModel.isLoading && reminder == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderOrange)
            }
        } else if (reminder != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. KARTU STATUS
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Status Pengingat", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                                Text(
                                    text = if (reminder.isActive) "AKTIF" else "BERHENTI / SELESAI",
                                    fontWeight = FontWeight.Black,
                                    color = if (reminder.isActive) Color(0xFF4CAF50) else Color.Red
                                )
                            }
                            Switch(
                                checked = reminder.isActive,
                                onCheckedChange = { viewModel.toggleReminder(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = WonderOrange)
                            )
                        }
                    }
                }

                // 2. KARTU INFORMASI TUGAS
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Tugas Terkait", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = reminder.taskTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            if (task != null) {
                                Text(text = task.description, style = MaterialTheme.typography.bodySmall, color = WonderGrey, maxLines = 3)
                            }
                        }
                    }
                }

                // 3. KONFIGURASI PENGINGAT
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Konfigurasi Notifikasi", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tipe Pengingat", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    Text(
                                        text = if (reminder.reminderType == "HOURLY") "Setiap Jam" else "Harian (X Kali)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Interval / Frekuensi", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    Text(
                                        text = "${reminder.intervalValue} ${if (reminder.reminderType == "HOURLY") "Jam Sekali" else "Kali Sehari"}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Terkirim Hari Ini", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    Text(text = "${reminder.remindersSentToday} Kali", fontWeight = FontWeight.Bold, color = WonderOrange)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Terakhir Terkirim", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    val last = reminder.lastRemindedAt?.take(16)?.replace("T", " ") ?: "Belum pernah"
                                    Text(text = last, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 4. PENERIMA TUGAS
                if (task != null) {
                    item {
                        Text("Penerima Notifikasi:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }
                    
                    items(task.assignees) { assignee ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(WonderBg)) {
                                if (!assignee.profileUrl.isNullOrEmpty()) {
                                    AsyncImage(model = assignee.profileUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(assignee.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(assignee.storeName, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
