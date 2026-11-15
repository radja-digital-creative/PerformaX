package id.biz.yuka.performax.ui.screen.leave

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import id.biz.yuka.libperformx.entity.LeaveEntity
import id.biz.yuka.performax.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(
    onBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToReview: () -> Unit,
    viewModel: LeaveViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadLeaves()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Info Izin & Cuti", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.loadLeaves() },
            state = pullToRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

                // HEADER QUICK ACTIONS
                item {
                    Surface(modifier = Modifier.fillMaxWidth(), color = WonderBg, shadowElevation = 2.dp) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                LeaveQuickAction("Buat Izin", Icons.Default.NoteAdd, Color(0xFFE0F2F1), onNavigateToCreate)

                                // Hanya tampilkan menu Review jika dia Owner/Manager
                                if (viewModel.isOwnerOrManager) {
                                    LeaveQuickAction("Review", Icons.Default.FactCheck, Color(0xFFFFF3E0), onNavigateToReview)
                                }

                                LeaveQuickAction("Riwayat", Icons.Default.History, Color(0xFFE1F5FE)) {}
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                item {
                    Text(
                        text = "Daftar Pengajuan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (viewModel.leaves.isEmpty() && !viewModel.isLoading) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada data izin", color = WonderGrey)
                        }
                    }
                } else {
                    items(viewModel.leaves) { leave ->
                        LeaveCardItem(leave)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaveQuickAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.Black.copy(alpha = 0.7f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun LeaveCardItem(leave: LeaveEntity) {
    val statusColor = when(leave.status.lowercase()) {
        "approved" -> Color(0xFF2E7D32)
        "rejected" -> Color.Red
        else -> WonderOrange
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(leave.employeeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Surface(color = statusColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text(leave.status.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(leave.typeName, color = WonderOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Text(leave.period, color = WonderGrey, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(leave.reason, style = MaterialTheme.typography.bodyMedium)
        }
    }
}