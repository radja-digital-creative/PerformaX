package id.biz.yuka.performax.ui.screen.home.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biz.yuka.libperformx.summary.DailyReportCardState
import id.biz.yuka.performax.ui.navigation.Screen
import id.biz.yuka.performax.ui.screen.home.*
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderLime
import id.biz.yuka.performax.ui.theme.WonderOrange

fun LazyListScope.operasionalTab(
    viewModel: OperasionalViewModel,
    onNavigateToMenu: (String) -> Unit
) {
    // 1. Attendance Cards (Rekening Section)
    item {
        AttendanceCardsSection(viewModel, onNavigateToMenu)
    }

    // 2. Feature Grid (Fitur Pilihan)
    item {
        FeatureGridSection(viewModel, onNavigateToMenu)
    }

    // 3. Summary Section (Promo buat kamu)
    item {
        SummaryCarouselSection(viewModel, onNavigateToMenu)
    }
}

@Composable
fun AttendanceCardsSection(
    viewModel: OperasionalViewModel,
    onNavigateToMenu: (String) -> Unit
) {
    val userStores = viewModel.userProfile?.stores ?: emptyList()
    val storeCount = userStores.size

    val userRole = viewModel.userProfile?.role?.lowercase() ?: "employee"
    val isOwnerOrAdmin = userRole == "owner" || userRole == "admin"

    // ✅ State untuk mendeteksi posisi scroll indikator
    val listState = rememberLazyListState()
    val activeIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Absensi hari ini",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )

            if (storeCount > 1) {
                Text(
                    text = "Lihat Semua",
                    style = MaterialTheme.typography.labelLarge.copy(color = WonderOrange, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onNavigateToMenu(Screen.Store.route) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ✅ Menyematkan listState agar terpantau geserannya
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val targetRoute = if (isOwnerOrAdmin) {
                Screen.OwnerAttendance.route
            } else {
                Screen.Attendance.route
            }

            item {
                PortfolioCard(onClick = { onNavigateToMenu(targetRoute) })
            }

            if (userStores.isNotEmpty()) {
                items(userStores) { store ->
                    MainAttendanceCard(
                        storeName = store.name,
                        checkIn = viewModel.checkInTime,
                        checkOut = viewModel.checkOutTime,
                        isOwnerOrAdmin = isOwnerOrAdmin // Melempar parameter role
                    )
                }
            } else {
                item {
                    MainAttendanceCard(
                        storeName = "Pusat",
                        checkIn = viewModel.checkInTime,
                        checkOut = viewModel.checkOutTime,
                        isOwnerOrAdmin = isOwnerOrAdmin
                    )
                }
            }
        }

        // ✅ Indikator titik yang bergerak secara dinamis
        if (storeCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(storeCount + 1) { index ->
                    val color = if (index == activeIndex) Color.Black else Color.LightGray
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
fun PortfolioCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFB2EBF2))
            .padding(12.dp)
            .clickable { onClick() },
    ) {
        Column {
            Icon(Icons.Default.PieChart, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Lihat Riwayat",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 12.sp)
            )
        }
    }
}

@Composable
fun MainAttendanceCard(
    storeName: String,
    checkIn: String,
    checkOut: String,
    isOwnerOrAdmin: Boolean
) {
    val formattedIn = formatTimeWIB(checkIn)
    val formattedOut = formatTimeWIB(checkOut)

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WonderOrange)
            // ✅ Bubbles Transparan agar tidak monoton
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = 120.dp.toPx(),
                    center = Offset(size.width, 0f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 70.dp.toPx(),
                    center = Offset(size.width * 0.2f, size.height + 20.dp.toPx())
                )
            }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column {
                Text(
                    text = "LOKASI KERJA",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Black.copy(alpha = 0.7f))
                )
                Text(
                    text = storeName,
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.Black, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ✅ Menghilangkan jam bagi owner dan menggantinya dengan teks ringkas
            if (isOwnerOrAdmin) {
                Text(
                    text = "Kelola dan pantau absensi\nkaryawan di lokasi ini.",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.Black.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Jam Masuk",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Black.copy(alpha = 0.7f))
                        )
                        Text(
                            text = formattedIn,
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.Black, fontWeight = FontWeight.Black)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.Black.copy(alpha = 0.2f)))

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Jam Keluar",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Black.copy(alpha = 0.7f))
                        )
                        Text(
                            text = formattedOut,
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.Black, fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

fun formatTimeWIB(timeStr: String?): String {
    Log.d("TIME_DEBUG", "1. RAW INPUT DARI DATABASE: [$timeStr]")

    if (timeStr.isNullOrBlank() || timeStr == "-" || timeStr == "--:--") {
        return "--:--"
    }

    return try {
        if (timeStr.contains("T")) {
            val zonedDateTime = java.time.ZonedDateTime.parse(timeStr)
            val jakartaTime = zonedDateTime.withZoneSameInstant(java.time.ZoneId.of("Asia/Jakarta"))
            jakartaTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        } else if (timeStr.contains(" ")) {
            val timePart = timeStr.split(" ")[1]
            if (timePart.length >= 5) timePart.substring(0, 5) else timePart
        } else if (timeStr.length >= 5 && timeStr.contains(":")) {
            timeStr.substring(0, 5)
        } else {
            timeStr
        }
    } catch (e: Exception) {
        if (timeStr.contains(" ") && timeStr.split(" ").size > 1) {
            val tp = timeStr.split(" ")[1]
            if (tp.length >= 5) tp.substring(0, 5) else tp
        } else if (timeStr.length >= 5) {
            timeStr.substring(0, 5)
        } else {
            timeStr
        }
    }
}

@Composable
fun FeatureGridSection(viewModel: OperasionalViewModel, onNavigateToMenu: (String) -> Unit) {
    val dynamicStoreRoute = viewModel.getDynamicStoreRoute()
    val dynamicLoanRoute = viewModel.getDynamicLoanRoute()
    val role = viewModel.userProfile?.role?.lowercase() ?: "employee"
    val isOwnerOrAdmin = role == "owner" || role == "admin"
    val isOwnerOrManager = isOwnerOrAdmin || role == "manager"

    val salaryRoute = if (isOwnerOrAdmin) Screen.AdminSalary.route else Screen.Salary.createRoute("me")

    val allFeatures = mutableListOf(
        FeatureItemData("Tugas", Icons.Default.Assignment, Color(0xFFFCD69E), Screen.Task.route),
        FeatureItemData("Izin/Cuti", Icons.Default.EventNote, Color(0xFF6D81FA), Screen.Leave.route),
        FeatureItemData("Slip Gaji", Icons.Default.Payments, Color(0xFF78CFF8), salaryRoute),
        FeatureItemData("Pinjaman", Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50), dynamicLoanRoute),
        FeatureItemData("Jadwal", Icons.Default.Schedule, Color(0xFFE78CF6), Screen.Schedule.route),
        FeatureItemData("Karyawan", Icons.Default.People, Color(0xFFCBF89B), Screen.Employee.route),
        FeatureItemData("Toko", Icons.Default.Storefront, Color(0xFFFFD54F), dynamicStoreRoute)
    )

    if (isOwnerOrManager) {
        allFeatures.add(
            FeatureItemData("Man. Absen", Icons.Default.AssignmentTurnedIn, Color(0xFFFFB74D), Screen.ManualAttendanceApproval.route)
        )
        allFeatures.add(
            FeatureItemData("Acc Kerja", Icons.Default.WorkHistory, Color(0xFF64B5F6), Screen.ExtraWorkApproval.route)
        )
    }

    allFeatures.add(FeatureItemData("Pelanggaran", Icons.Default.Gavel, Color(0xFF89E9F6), Screen.Violation.route))
    allFeatures.add(FeatureItemData("Resign", Icons.Default.ExitToApp, Color(0xFFFF7043), Screen.Resignation.route))
    allFeatures.add(FeatureItemData("Semua", Icons.Default.Apps, Color(0xFFF58D8D), Screen.AllFeatures.route))

    val visibleFeatures = allFeatures.filter { feature ->
        if (role == "employee") {
            when (feature.label) {
                "Karyawan", "Toko", "Persetujuan Absen", "Semua" -> false
                else -> true
            }
        } else {
            when (feature.label) {
                "Resign" -> false
                else -> true
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fitur pilihan kamu",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )

            if (visibleFeatures.size > 8) {
                Text(
                    text = "Atur",
                    style = MaterialTheme.typography.labelLarge.copy(color = WonderOrange, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { /* Aksi Navigasi Atur Fitur */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            visibleFeatures.chunked(4).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { item ->
                        FeatureItem(
                            data = item,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToMenu(item.route) }
                        )
                    }
                    if (rowItems.size < 4) {
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

data class FeatureItemData(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@Composable
fun FeatureItem(data: FeatureItemData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(data.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                data.icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = data.label,
            color = Color.Black,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

@Composable
fun SummaryCarouselSection(
    viewModel: OperasionalViewModel,
    onNavigateToMenu: (String) -> Unit
) {
    val performanceSummary = viewModel.performanceSummary
    val pendingTasks = viewModel.pendingTasksCount
    val role = viewModel.userProfile?.role?.lowercase() ?: "employee"
    val isOwner = role == "owner"

    data class SummaryItemData(val title: String, val desc: String, val color: Color, val route: String)
    val summaryList = mutableListOf<SummaryItemData>()

    // ── Card Rincian Performa: HANYA untuk NON-OWNER ─────────────────────
    if (performanceSummary != null && !isOwner) {
        val themeColor = when (performanceSummary.colorTheme) {
            "WonderLime" -> WonderLime
            "WonderOrange" -> WonderOrange
            "WonderGrey" -> WonderGrey
            else -> WonderLime
        }
        summaryList.add(
            SummaryItemData(
                title = performanceSummary.title,
                desc = performanceSummary.description,
                color = themeColor,
                route = "performa_detail"
            )
        )
    }

    // ── Card Tugas Pending: NON-OWNER ────────────────────────────────────
    if (pendingTasks > 0 && !isOwner) {
        summaryList.add(
            SummaryItemData(
                title = "Tugas Menunggu",
                desc = "Ada $pendingTasks tugas baru yang perlu kamu cek",
                color = Color(0xFFB2EBF2),
                route = Screen.Task.route
            )
        )
    }

    // ── Card Daily Report: HANYA OWNER ───────────────────────────────────
    if (isOwner) {
        when (val reportState = viewModel.dailyReportState) {
            is DailyReportCardState.Available -> {
                // Format jam dari generatedAt (ISO string)
                val displayTime = try {
                    val zdt = java.time.ZonedDateTime.parse(reportState.generatedAt)
                    val wib = zdt.withZoneSameInstant(java.time.ZoneId.of("Asia/Jakarta"))
                    wib.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: Exception) { "--:--" }

                summaryList.add(
                    SummaryItemData(
                        title = "Laporan Hari Ini",
                        desc = "Tersedia · Dibuat pukul $displayTime WIB · Ketuk untuk membaca",
                        color = WonderLime,
                        route = Screen.DailyReport.route
                    )
                )
            }
            is DailyReportCardState.NotAvailable -> {
                summaryList.add(
                    SummaryItemData(
                        title = "Laporan Belum Tersedia",
                        desc = "Akan dibuat otomatis jam 19.00 WIB · Ketuk untuk membuat sekarang",
                        color = Color(0xFFEEEEEE),
                        route = Screen.DailyReport.route
                    )
                )
            }
            // Idle / Loading / Error: jangan tampilkan card
            else -> Unit
        }

        // ── Card Live Location: HANYA OWNER ──────────────────────────────────
        summaryList.add(
            SummaryItemData(
                title = "Live Location Karyawan",
                desc = "Pantau lokasi real-time karyawan di peta · Ketuk untuk membuka",
                color = Color(0xFFB2EBF2),
                route = Screen.LiveLocation.route
            )
        )
    }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ringkasan buat kamu",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )

            if (summaryList.size > 8) {
                Text(
                    text = "Lihat Semua",
                    style = MaterialTheme.typography.labelLarge.copy(color = WonderOrange, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onNavigateToMenu("performa_detail") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(summaryList.size) { index ->
                val currentItem = summaryList[index]
                SummaryCard(
                    title = currentItem.title,
                    desc = currentItem.desc,
                    bgColor = currentItem.color,
                    onClick = { onNavigateToMenu(currentItem.route) }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, desc: String, bgColor: Color, onClick: () -> Unit) {
    val bubbleColor = when (bgColor) {
        WonderLime -> WonderOrange
        Color(0xFFB2EBF2) -> WonderOrange
        WonderOrange -> WonderLime
        else -> Color.White
    }.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .width(300.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .drawBehind {
                drawCircle(
                    color = bubbleColor,
                    radius = 130.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                )
                drawCircle(
                    color = bubbleColor,
                    radius = 80.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(0f, size.height + 20.dp.toPx())
                )
                drawCircle(
                    color = bubbleColor,
                    radius = 45.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width - 50.dp.toPx(), size.height)
                )
            }
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column {
            Surface(
                color = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick() }
            ) {
                Text(
                    text = "Cek Sekarang",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}