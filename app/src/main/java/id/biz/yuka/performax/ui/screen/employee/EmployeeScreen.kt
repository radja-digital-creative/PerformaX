package id.biz.yuka.performax.ui.screen.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.performax.ui.theme.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadEmployees()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Daftar Karyawan", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.loadEmployees() },
            state = pullToRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = WonderBg
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Hanya tampilkan tombol Tambah jika user adalah Owner/Admin
                                if (viewModel.isOwnerOrAdmin) {
                                    EmployeeQuickAction(
                                        label = "Tambah",
                                        icon = Icons.Default.PersonAdd,
                                        color = Color(0xFFE0F2F1), // Hijau muda
                                        onClick = onNavigateToCreate
                                    )
                                }
                                EmployeeQuickAction(
                                    label = "Filter",
                                    icon = Icons.Default.FilterList,
                                    color = Color(0xFFFFF3E0), // Oranye muda
                                    onClick = { /* TODO */ }
                                )
                                EmployeeQuickAction(
                                    label = "Laporan",
                                    icon = Icons.Default.Assessment,
                                    color = Color(0xFFE1F5FE), // Biru muda
                                    onClick = { /* TODO */ }
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.LightGray))
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (viewModel.isOwnerOrAdmin) "Semua Cabang (${viewModel.employees.size})" else "Toko Anda (${viewModel.employees.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                }

                if (!viewModel.isLoading && viewModel.employees.isEmpty()) {
                    item { Box(modifier = Modifier.fillParentMaxSize(0.5f), contentAlignment = Alignment.Center) { Text("Tidak ada data", color = WonderGrey) } }
                } else {
                    items(viewModel.employees) { employee ->
                        EmployeeItemCard(
                            employee = employee,
                            canViewDetails = viewModel.canViewDetails,
                            onClick = { if (viewModel.canViewDetails) onNavigateToDetail(employee.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeItemCard(employee: EmployeeEntity, canViewDetails: Boolean, onClick: () -> Unit) {
    val roleColor = when(employee.roleName.lowercase()) {
        "owner", "admin" -> WonderOrange
        "manager" -> Color(0xFF2E7D32)
        else -> WonderGrey
    }

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(enabled = canViewDetails) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountCircle, null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(employee.roleName.uppercase(), color = roleColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                if (employee.stores.isNotEmpty()) {
                    val storesText = employee.stores.joinToString(", ") { it.name }
                    Text(storesText, color = WonderGrey, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            if (canViewDetails) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun EmployeeQuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f)) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}