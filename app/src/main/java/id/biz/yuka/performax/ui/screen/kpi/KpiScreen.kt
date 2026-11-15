package id.biz.yuka.performax.ui.screen.kpi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
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
import id.biz.yuka.libperformx.entity.KpiEntity
import id.biz.yuka.performax.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpiScreen(
    onBack: () -> Unit,
    viewModel: KpiViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Poin & KPI", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.loadKpiData() },
            state = pullToRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. KARTU PENCAPAIAN PRIBADI
                item {
                    val myKpi = viewModel.myKpi
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WonderOrange)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Poin Anda Bulan Ini", style = MaterialTheme.typography.labelLarge, color = Color.Black.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = myKpi?.totalKpiScore?.toString() ?: "0",
                                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black, color = Color.Black)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Poin Tugas", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
                                    Text("${myKpi?.taskPoints ?: 0}", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.Black.copy(alpha = 0.2f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Poin Absen", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
                                    Text("${myKpi?.attendanceScore ?: 0}", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.Black.copy(alpha = 0.2f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ranking", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
                                    Text("#${myKpi?.rank ?: "-"}", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                // 2. HEADER LEADERBOARD
                item {
                    Text(
                        text = "🏆 Papan Peringkat (Leaderboard)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 3. LIST RANKING
                if (!viewModel.isLoading && viewModel.leaderboard.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(0.4f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Data ranking belum tersedia.", color = WonderGrey)
                        }
                    }
                } else {
                    itemsIndexed(viewModel.leaderboard) { index, kpi ->
                        // Jika bukan Owner, hanya tampilkan Top 3 dan Diri Sendiri
                        val showCard = viewModel.isOwnerOrAdmin || index < 3 || kpi.userId == viewModel.myKpi?.userId

                        if (showCard) {
                            LeaderboardItemCard(kpi = kpi, isMe = kpi.userId == viewModel.myKpi?.userId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardItemCard(kpi: KpiEntity, isMe: Boolean) {
    // Styling Khusus untuk Ranking 1, 2, 3
    val (medalColor, bgColor) = when (kpi.rank) {
        1 -> Pair(Color(0xFFFFD700), Color(0xFFFFF9C4)) // Emas
        2 -> Pair(Color(0xFFC0C0C0), Color(0xFFF5F5F5)) // Perak
        3 -> Pair(Color(0xFFCD7F32), Color(0xFFEFEBE9)) // Perunggu
        else -> Pair(Color.Transparent, Color.White)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isMe) BorderStroke(2.dp, WonderOrange) else null // Sorot diri sendiri
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nomor Urut / Medali
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (kpi.rank <= 3) medalColor else WonderBg),
                contentAlignment = Alignment.Center
            ) {
                if (kpi.rank <= 3) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White)
                } else {
                    Text("#${kpi.rank}", fontWeight = FontWeight.Bold, color = WonderGrey)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nama
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "${kpi.employeeName} (Anda)" else kpi.employeeName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(kpi.roleName, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
            }

            // Poin
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${kpi.totalKpiScore}",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (kpi.rank <= 3) Color(0xFFE65100) else Color.Black
                )
                Text("Poin", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
            }
        }
    }
}