package id.biz.yuka.performax.ui.screen.location

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.components.LiveLocationLeafletMap
import id.biz.yuka.performax.ui.components.LiveLocationMapMarker
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLocationScreen(
    onBack: () -> Unit,
    viewModel: LiveLocationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val locationList = viewModel.locationList
    val isLoading = viewModel.isLoading
    val isRefreshing = viewModel.isRefreshing
    val selectedUserId = viewModel.selectedUserId
    val toastMessage = viewModel.toastMessage
    val refreshingUserId = viewModel.refreshingUserId

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val mapMarkers = remember(locationList) {
        val filtered = locationList.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        android.util.Log.d("LiveLocationScreen", "Total Karyawan: ${locationList.size}, Valid Lokasi: ${filtered.size}")
        
        filtered.map { item ->
            LiveLocationMapMarker(
                userId = item.userId,
                lat = item.latitude,
                lng = item.longitude,
                title = item.fullName,
                photoUrl = item.profileUrl,
                phone = item.phoneNumber,
                timeText = viewModel.formatRelativeTime(item.recordedAt)
            )
        }
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val effectiveSheetPeekHeight = 72.dp + navBarPadding

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "📍 Live Location",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Monitoring ${locationList.size} Karyawan",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Tombol Ping Massal (⚡)
                    IconButton(
                        onClick = { viewModel.refreshAllLocations() },
                        enabled = !isRefreshing && !isLoading
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = "Ping Semua Karyawan",
                            tint = WonderOrange
                        )
                    }

                    // Tombol Refresh Data Peta (🔄)
                    IconButton(
                        onClick = { viewModel.loadLocations() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Peta",
                            tint = Color.DarkGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = effectiveSheetPeekHeight,
            sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            sheetContainerColor = Color.White,
            sheetShadowElevation = 12.dp,
            sheetDragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.LightGray)
                    )
                }
            },
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .navigationBarsPadding()
                ) {
                    // Header Bottom Sheet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Karyawan (${locationList.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        FilterChip(
                            selected = viewModel.isShowStores,
                            onClick = { viewModel.isShowStores = !viewModel.isShowStores },
                            label = {
                                Text(
                                    text = "🏪 Lokasi Toko",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WonderOrange.copy(alpha = 0.15f),
                                selectedLabelColor = WonderOrange
                            )
                        )
                    }

                    Divider(color = Color(0xFFF0F0F0))

                    if (locationList.isEmpty() && !isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📍", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Belum Ada Data Lokasi Karyawan",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(locationList) { item ->
                                val isSelected = selectedUserId == item.userId
                                val isItemRefreshing = refreshingUserId == item.userId

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) Color(0xFFFFF3E0) else Color.Transparent
                                        )
                                        .clickable {
                                            viewModel.selectUser(item.userId)
                                            // Saat item di-klik, kecilkan sheet agar peta terlihat jelas
                                            scope.launch {
                                                scaffoldState.bottomSheetState.partialExpand()
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Foto Profil
                                    AsyncImage(
                                        model = item.profileUrl.ifBlank { "https://placehold.co/150?text=User" },
                                        contentDescription = item.fullName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE2E8F0))
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Detail Nama & Waktu
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.fullName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "📱 ${item.phoneNumber}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🕒 ${viewModel.formatRelativeTime(item.recordedAt)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = WonderOrange,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }

                                    // Tombol Focus (🎯)
                                    IconButton(
                                        onClick = {
                                            viewModel.selectUser(item.userId)
                                            scope.launch {
                                                scaffoldState.bottomSheetState.partialExpand()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.MyLocation,
                                            contentDescription = "Fokus ke Peta",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Tombol Refresh Tunggal (🔄)
                                    IconButton(
                                        onClick = { viewModel.refreshSingleLocation(item.userId) },
                                        enabled = !isItemRefreshing
                                    ) {
                                        if (isItemRefreshing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = WonderOrange
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Refresh Lokasi Karyawan",
                                                tint = WonderOrange,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                Divider(color = Color(0xFFF8F8F8))
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // FULLSCREEN MAP (100% Layar)
            Box(modifier = Modifier.fillMaxSize()) {
                LiveLocationLeafletMap(
                    modifier = Modifier.fillMaxSize(),
                    markers = mapMarkers,
                    stores = viewModel.storeList,
                    isShowStores = viewModel.isShowStores,
                    zoomLevel = 13,
                    isInteractive = true,
                    selectedUserId = selectedUserId
                )

                if (isRefreshing || isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = WonderOrange
                    )
                }
            }
        }
    }
}
