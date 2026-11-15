package id.biz.yuka.performax.ui.screen.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.performax.ui.navigation.Screen
import id.biz.yuka.performax.ui.screen.home.GrowthViewModel
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderLime
import kotlin.math.absoluteValue

fun LazyListScope.growthTab(viewModel: GrowthViewModel,onNavigateToMenu: (String) -> Unit) {
    val myUserId = viewModel.myUserId ?: ""

    // ✅ PENGURUTAN: 1. Urgent, 2. Tenggat Waktu Terdekat, 3. Tugas Baru (Pending)
    val tasks = viewModel.tasksList.filter { task ->
        task.assignees.any { assignee ->
            assignee.userId == myUserId && assignee.status == "pending" || assignee.status == "in_progress"
        }
    }.sortedWith(
        compareBy<TaskEntity> { task ->
            // Prioritas 1: Label Urgent / Penting
            val isUrgent = task.labels.any { it.name.contains("urgent", true) || it.name.contains("penting", true) }
            if (isUrgent) 0 else 1
        }.thenBy { task ->
            // Prioritas 2: Tenggat Waktu Paling Dekat
            try {
                java.time.OffsetDateTime.parse(task.dueDate).toEpochSecond()
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }
    )

    item {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "Mulai kembangin poin untuk karirmu",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Selesaikan tugas atau tingkatkan performa kerja.",
                style = MaterialTheme.typography.bodySmall,
                color = WonderGrey
            )
        }
    }

    if (tasks.isEmpty()) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Belum ada tugas pengembangan tersedia.", color = WonderGrey)
            }
        }
    } else {
        items(tasks) { task ->
            GrowthTaskCard(task,
                onClick = { onNavigateToMenu(Screen.TaskDetail.createRoute(task.id)) })
        }
    }
}

@Composable
fun GrowthTaskCard(task: TaskEntity,
                   onClick: () -> Unit) {
    val category = task.labels.firstOrNull()?.name ?: "Tugas Umum"

    // Palet warna dinamis seperti desain referensi
    val colorPalette = listOf(
        Color(0xFFB2EBF2), // Cyan/Blue Light
        Color(0xFFC8E6C9), // Green Light
        Color(0xFFFFCCBC), // Orange/Peach Light
        Color(0xFFE1BEE7)  // Purple Light
    )
    val categoryColor = colorPalette[task.id.hashCode().absoluteValue % colorPalette.size]

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // ✅ HIASAN POJOK KANAN ATAS (Bubble Transparan)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(100.dp)
                    .clip(RoundedCornerShape(topEnd = 16.dp))
            ) {
                // Bubble Utama
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(x = 40.dp, y = (-20).dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.6f))
                )
                // Bubble Sekunder
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .offset(x = 10.dp, y = 20.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.8f))
                )
                // Bubble Tersier
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = 60.dp, y = 50.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.4f))
                )
            }

            Column {
                // ✅ TAG KATEGORI (Pojok Kiri Atas)
                Surface(
                    color = categoryColor,
                    shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // ✅ BADGE POIN (Pill Shape)
                            Surface(
                                color = WonderLime,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "+${task.assignees.firstOrNull()?.earnedPoints ?: 0} Poin",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Deskripsi Singkat
                        Text(
                            text = task.description.ifBlank { "Tidak ada deskripsi untuk tugas ini" },
                            style = MaterialTheme.typography.bodySmall,
                            color = WonderGrey,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // ✅ IKON PANAH KANAN
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Detail",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}