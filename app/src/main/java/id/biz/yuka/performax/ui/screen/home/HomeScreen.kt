package id.biz.yuka.performax.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.navigation.Screen
import id.biz.yuka.performax.ui.screen.home.components.growthTab
import id.biz.yuka.performax.ui.screen.home.components.insightTab
import id.biz.yuka.performax.ui.screen.home.components.operasionalTab
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderLime
import id.biz.yuka.performax.ui.theme.WonderOrange
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import id.biz.yuka.libperformx.entity.AnnouncementEntity
import id.biz.yuka.performax.BuildConfig

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToMenu: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = true) {
        viewModel.logoutSuccess.collect {
            onLogout()
        }
    }

    var selectedTab by remember { mutableIntStateOf(1) } // 0: Insight, 1: Operasional, 2: Growth
    // ViewModels start network work in init: create only the visible tab.
    val operasionalViewModel = if (selectedTab == 1) hiltViewModel<OperasionalViewModel>() else null
    val insightViewModel = if (selectedTab == 0) hiltViewModel<InsightViewModel>() else null
    val growthViewModel = if (selectedTab == 2) hiltViewModel<GrowthViewModel>() else null
    val pullToRefreshState = rememberPullToRefreshState()
    val currentWarning = viewModel.currentWarning
    val isAnyLoading = viewModel.isLoading || operasionalViewModel?.isLoading == true ||
            insightViewModel?.isLoading == true || growthViewModel?.isLoading == true

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            // Floating Absensi (Pill Style)
            FloatingActionButton(
                onClick = { onNavigateToMenu(Screen.ProcessAttendance.route) },
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .width(84.dp)
                    .height(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Absensi",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isAnyLoading,
                onRefresh = {
                    viewModel.loadGlobalData()
                    operasionalViewModel?.fetchOperasionalData()
                    insightViewModel?.fetchInsightData()
                    growthViewModel?.fetchGrowthData()
                },
                state = pullToRefreshState,
                indicator = {
                    if (isAnyLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().zIndex(1f),
                            color = WonderOrange,
                            trackColor = WonderLime
                        )
                    }
                }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        CombinedHeader(
                            name = viewModel.userProfile?.fullName ?: "User",
                            onLogout = { viewModel.logout() },
                            onNavigateToUserAnnouncement = { onNavigateToMenu(Screen.UserAnnouncement.route) },
                            onNavigateToKpi = { onNavigateToMenu(Screen.Kpi.route) },
                            onNavigateToProfile = { onNavigateToMenu(Screen.Profile.route) },
                            onNavigateToNotification = { onNavigateToMenu(Screen.UserAnnouncement.route) },
                            hasNotification = viewModel.currentWarning != null
                        )
                    }

                    // ✅ 3. BUBBLE CHAT NOTIFICATION (MENGGUNAKAN SKALA PRIORITAS)
                    if (currentWarning != null && !isAnyLoading) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .offset(y = (-8).dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                // Segitiga Penunjuk (Bubble Beak)
                                Box(
                                    modifier = Modifier
                                        .padding(end = 80.dp)
                                        .size(16.dp)
                                        .offset(y = 8.dp)
                                        .graphicsLayer { rotationZ = 45f }
                                        .background(Color(0xFFFFF3E0))
                                )

                                // Kotak Pesan (Bubble Body)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = "Peringatan",
                                        tint = WonderOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = currentWarning.message,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 2. Sticky Tab Switcher
                    stickyHeader {
                        WonderTabSwitcher(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    // 3. Tab Content
                    when (selectedTab) {
                        0 -> insightTab(insightViewModel!!, onNavigateToMenu)
                        1 -> operasionalTab(operasionalViewModel!!, onNavigateToMenu)
                        2 -> growthTab(growthViewModel!!, onNavigateToMenu)
                    }
                }
            }

            // ✅ ANNOUNCEMENT MODAL POPUP
            if (viewModel.showAnnouncementPopup && viewModel.activeAnnouncements.isNotEmpty()) {
                AnnouncementPopup(
                    announcements = viewModel.activeAnnouncements,
                    onDismiss = { viewModel.showAnnouncementPopup = false },
                    onNavigateToDetail = { id ->
                        viewModel.showAnnouncementPopup = false
                        onNavigateToMenu(Screen.AnnouncementDetail.createRoute(id))
                    }
                )
            }
        }
    }
}

@Composable
fun AnnouncementPopup(
    announcements: List<AnnouncementEntity>,
    onDismiss: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { announcements.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 16.dp
                ) { page ->
                    val item = announcements[page]
                    var aspectRatio by remember { mutableStateOf(1f) } // Default 1:1
                    var imageLoading by remember { mutableStateOf(true) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable { onNavigateToDetail(item.id) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = false)
                                .fillMaxHeight(0.7f) // Jangan melebihi 70% tinggi layar
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (imageLoading) Color.White.copy(alpha = 0.1f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    onSuccess = { state ->
                                        imageLoading = false
                                        val size = state.painter.intrinsicSize
                                        if (size.width > 0 && size.height > 0) {
                                            aspectRatio = size.width / size.height
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                                )
                            } else {
                                // Fallback jika teks saja
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(24.dp))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                            
                            if (imageLoading && !item.imageUrl.isNullOrBlank()) {
                                CircularProgressIndicator(color = WonderOrange, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }

                if (announcements.size > 1) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(announcements.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.3f)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CombinedHeader(
    name: String, onLogout: () -> Unit,
    onNavigateToUserAnnouncement: () -> Unit,
    onNavigateToKpi: () -> Unit,
    onNavigateToProfile: () -> Unit,
    hasNotification: Boolean,
    onNavigateToNotification: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WonderBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.End, // Rata kanan agar versi ada di bawah huruf 'X'
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "performaX",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = WonderOrange,
                        letterSpacing = (-1).sp
                    )
                )
                Text(
                    text = BuildConfig.VERSION_NAME, // Versi Aplikasi
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = WonderGrey,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    // Tarik teks sedikit ke atas agar menempel/menyatu dengan nama aplikasi
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.height(32.dp).clickable { onNavigateToKpi() },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Stars, null, modifier = Modifier.size(16.dp), tint = WonderOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Poin", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.height(32.dp).clickable { onLogout() },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    color = Color.White
                ) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Logout, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AccountCircle, null, modifier = Modifier.size(32.dp).clickable { onNavigateToProfile() })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hi, $name !",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f).clickable { onNavigateToProfile() }
            )
            IconButton(onClick = onNavigateToNotification) {
                Box {
                    val infiniteTransition = rememberInfiniteTransition(label = "shake")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = -15f,
                        targetValue = 15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(150, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "shake_anim"
                    )

                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            // Terapkan rotasi hanya jika hasNotification bernilai true
                            rotationZ = if (hasNotification) rotation else 0f
                        }
                            // integrasi disini onNavigateToUserAnnouncement()
                    )

                    // Titik Merah Notifikasi
                    if (hasNotification) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .background(Color.Red, CircleShape)
                                .border(1.dp, WonderBg, CircleShape) // Garis luar agar tidak menyatu dengan ikon
                        )
                    }
                }
            }
            IconButton(onClick = onNavigateToUserAnnouncement) { Icon(Icons.Outlined.Assignment, null) }
        }
    }
}

@Composable
fun WonderTabSwitcher(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = WonderBg) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("Insight", "Operasional", "Growth")
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selectedTab == index) WonderLime else Color.Transparent)
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) Color.Black else WonderGrey,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}
