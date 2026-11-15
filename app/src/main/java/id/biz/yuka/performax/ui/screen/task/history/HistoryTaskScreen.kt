package id.biz.yuka.performax.ui.screen.task.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import io.github.jan.supabase.gotrue.auth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTaskScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HistoryTaskViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dibuat, 1: Dikerjakan
    val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
    val pullToRefreshState = rememberPullToRefreshState()

    // Memfilter data berdasarkan tab yang aktif
    val filteredTasks = viewModel.tasks.filter { task ->
        if (selectedTab == 0) {
            task.creatorId == currentUserId
        } else {
            task.assignees.any { it.userId == currentUserId }
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Riwayat Tugas", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = WonderBg,
                    contentColor = WonderOrange,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = WonderOrange
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Tugas Dibuat", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == 0) WonderOrange else WonderGrey) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Tugas Dikerjakan", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == 1) WonderOrange else WonderGrey) }
                    )
                }

                if (viewModel.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = WonderOrange,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.loadHistoryTasks() },
            state = pullToRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!viewModel.isLoading && filteredTasks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Riwayat Kosong", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Belum ada tugas yang selesai di kategori ini.", color = WonderGrey, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        HistoryTaskCard(task = task, currentUserId = currentUserId, onClick = { onNavigateToDetail(task.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTaskCard(task: TaskEntity, currentUserId: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = Color.Gray)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))

                val dateText = try {
                    val date = LocalDate.parse(task.dueDate.take(10))
                    date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))
                } catch (e: Exception) { task.dueDate }

                Text(text = "Tenggat: $dateText", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
            }

            // Ambil status spesifik assignment untuk user ini, atau status assignee pertama jika dia creator
            val assignment = task.assignees.find { it.userId == currentUserId } ?: task.assignees.firstOrNull()
            val status = assignment?.status ?: "unknown"

            val (statusText, statusColor, bgColor) = when (status) {
                "verified" -> Triple("Selesai", Color(0xFF2E7D32), Color(0xFFE8F5E9))
                "completed" -> Triple("Menunggu Review", WonderOrange, Color(0xFFFFF3E0))
                "canceled" -> Triple("Dibatalkan", Color.Red, Color(0xFFFFEBEE))
                else -> Triple(status.replaceFirstChar { it.uppercase() }, Color.Gray, Color(0xFFF5F5F5))
            }

            Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor)
                )
            }
        }
    }
}