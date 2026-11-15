package id.biz.yuka.performax.ui.screen.task.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSettingsScreen(
    onBack: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    onNavigateToUrgent: () -> Unit,
    onNavigateToReport: () -> Unit
) {
    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Tugas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Manajemen Tugas Otomatis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                SettingsMenuItem(
                    title = "Atur Tugas Rutin",
                    subtitle = "Kelola jadwal perulangan tugas harian, mingguan, atau bulanan.",
                    icon = Icons.Default.Autorenew,
                    onClick = onNavigateToRoutine
                )
            }

            item {
                SettingsMenuItem(
                    title = "Atur Tugas Darurat",
                    subtitle = "Kelola pengingat persisten untuk tugas-tugas berlabel Urgent.",
                    icon = Icons.Default.NotificationImportant,
                    onClick = onNavigateToUrgent
                )
            }

            item {
                SettingsMenuItem(
                    title = "Laporan Tugas",
                    subtitle = "Riwayat tugas tim karyawan.",
                    icon = Icons.Default.FileDownload,
                    onClick = onNavigateToReport
                )
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = WonderOrange.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = WonderOrange)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}