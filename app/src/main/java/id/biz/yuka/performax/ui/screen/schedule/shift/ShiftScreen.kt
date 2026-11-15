package id.biz.yuka.performax.ui.screen.schedule.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(
    onBack: () -> Unit,
    onNavigateToManage: (String, String) -> Unit,
    viewModel: ShiftViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { viewModel.stores.size })

    // Sinkronisasi Pager dengan ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onStoreChanged(pagerState.currentPage)
    }

    // Refresh data saat kembali ke layar ini
    LaunchedEffect(Unit) {
        viewModel.loadStores()
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Shift", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            if (viewModel.stores.isNotEmpty()) {
                Box(modifier = Modifier.background(Color.White).navigationBarsPadding().padding(16.dp)) {
                    Button(
                        onClick = {
                            val storeId = viewModel.stores[viewModel.selectedStoreIndex].id
                            onNavigateToManage(storeId, "new")
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Shift Baru", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (viewModel.isLoading && viewModel.stores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WonderOrange)
                }
            } else {
                // --- 1. STORE CAROUSEL ---
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    pageSpacing = 16.dp,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) { page ->
                    val store = viewModel.stores[page]
                    Card(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = if (pagerState.currentPage == page) WonderOrange else Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Store, null, tint = Color.Black)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(store.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Text(store.address ?: "-", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }

                // --- 2. SHIFT LIST ---
                Text(
                    text = "Daftar Shift Tersedia",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (viewModel.shifts.isEmpty()) {
                        item {
                            Text("Belum ada shift di toko ini.", modifier = Modifier.padding(top = 40.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = WonderGrey)
                        }
                    }
                    items(viewModel.shifts) { shift ->
                        ShiftItemCard(shift) {
                            onNavigateToManage(shift.storeId, shift.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftItemCard(shift: id.biz.yuka.libperformx.entity.ShiftEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(shift.shiftName, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp), tint = WonderGrey)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${shift.startTime.take(5)} - ${shift.endTime.take(5)}", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                }
            }
            Surface(color = WonderLime.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                Text(text = "${shift.attendanceRadius}m", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}