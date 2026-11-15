package id.biz.yuka.performax.ui.screen.leave.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.LeaveEntity
import id.biz.yuka.performax.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewLeaveScreen(
    onBack: () -> Unit,
    viewModel: ReviewLeaveViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Tampilkan pesan error via Snackbar
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            viewModel.errorMessage = null
        }
    }

    // Tampilkan pesan sukses via Snackbar
    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.successMessage = null
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Review Izin Karyawan", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.loadPendingLeaves() },
            state = pullToRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!viewModel.isLoading && viewModel.pendingLeaves.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada pengajuan izin baru.", color = WonderGrey)
                        }
                    }
                } else {
                    items(viewModel.pendingLeaves) { leave ->
                        ReviewLeaveCard(
                            leave = leave,
                            isActionEnabled = !viewModel.isLoading,  // Fix Bug #4: disable saat loading
                            onApprove = { viewModel.respondToLeave(leave.id, true) },
                            onReject = { viewModel.respondToLeave(leave.id, false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewLeaveCard(
    leave: LeaveEntity,
    isActionEnabled: Boolean = true,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(leave.employeeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(leave.typeName, color = WonderOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Tanggal: ${leave.period}", style = MaterialTheme.typography.bodySmall, color = WonderGrey)
            Text("Alasan: ${leave.reason}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = isActionEnabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, if (isActionEnabled) Color.Red else Color.LightGray)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tolak")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onApprove,
                    enabled = isActionEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Setujui")
                }
            }
        }
    }
}