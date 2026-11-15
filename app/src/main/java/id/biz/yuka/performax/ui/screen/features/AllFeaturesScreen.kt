package id.biz.yuka.performax.ui.screen.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.navigation.Screen
import id.biz.yuka.performax.ui.screen.home.OperasionalViewModel

// Kita buat data class untuk mendefinisikan menu di dalam Grid
data class AppFeature(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@Composable
fun AllFeaturesScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: OperasionalViewModel = hiltViewModel() // Pinjam ViewModel Home untuk dapatkan role
) {
    val backgroundColor = Color(0xFFF9F9F9)
    val role = viewModel.userProfile?.role?.lowercase() ?: "employee"
    val dynamicStoreRoute = viewModel.getDynamicStoreRoute()

    // 1. Grouping Karyawan (Umum)
    val employeeFeatures = listOf(
        AppFeature("Tugas", Icons.Default.Assignment, Color(0xFFFCD69E), Screen.Task.route),
        AppFeature("Izin/Cuti", Icons.Default.EventNote, Color(0xFF6D81FA), Screen.Leave.route),
        AppFeature("Slip Gaji", Icons.Default.Payments, Color(0xFF78CFF8), Screen.Salary.route),
        AppFeature("Jadwal", Icons.Default.Schedule, Color(0xFFE78CF6), Screen.Schedule.route),
        AppFeature("Pengumuman", Icons.Default.Campaign, Color(0xFFFFB74D), Screen.Announcement.route),
        AppFeature("Pelanggaran", Icons.Default.Gavel, Color(0xFF89E9F6), Screen.Violation.route)
    )

    // 2. Grouping HR & Operasional
    val hrFeatures = mutableListOf(
        AppFeature("Toko/Cabang", Icons.Default.Storefront, Color(0xFFFFD54F), dynamicStoreRoute),
        AppFeature("Daftar Karyawan", Icons.Default.People, Color(0xFFCBF89B), Screen.Employee.route),
        AppFeature("Pengaturan Cuti", Icons.Default.FactCheck, Color(0xFFFFA726), "leave_type"),
        AppFeature("Bonus", Icons.Default.Redeem, Color(0xFFFF6326), "bonus")
    )

    // Fitur Khusus Owner/Admin/Manager di HR & Operasional
    if (role == "owner" || role == "admin" || role == "manager") {
        hrFeatures.add(AppFeature("Persetujuan Absen", Icons.Default.AssignmentTurnedIn, Color(0xFFFFB74D), Screen.ManualAttendanceApproval.route))
        hrFeatures.add(AppFeature("Izin Masuk Kerja", Icons.Default.WorkHistory, Color(0xFF64B5F6), Screen.ExtraWorkApproval.route))
    }

    if (role == "owner" || role == "admin") {
        hrFeatures.add(AppFeature("Aturan HR", Icons.Default.RuleFolder, Color(0xFFEF5350), "employee_rule"))
        hrFeatures.add(AppFeature("Tegur Manual", Icons.Default.PostAdd, Color(0xFFAB47BC), "violation_create"))
        hrFeatures.add(AppFeature("Pengajuan Resign", Icons.Default.ExitToApp, Color(0xFFFF7043), Screen.Resignation.route))
    }

    // 3. Grouping Finansial & Potongan
    val financialFeatures = mutableListOf(
        AppFeature("Potongan Gaji", Icons.Default.MoneyOff, Color(0xFF8D6E63), Screen.Deduction.route),
        AppFeature("Potongan Manual", Icons.Default.PriceCheck, Color(0xFFE57373), Screen.ManualDeductions.createRoute("all")),
        AppFeature("Pinjaman Loan", Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50), Screen.Loan.route)
    )

    // 4. Grouping KPI & Penilaian
    val kpiFeatures = mutableListOf(
        AppFeature("Penilaian KPI", Icons.Default.Assessment, Color(0xFF42A5F5), "kpi_rule")
    )

    // 5. Grouping Laporan
    val reportFeatures = listOf(
        AppFeature("Daily Report", Icons.Default.DateRange, Color(0xFFFCD69E), Screen.ReportMenu.route)
    )

    Scaffold(
        topBar = {
            Surface(
                color = backgroundColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                    Text(
                        text = "Semua Fitur",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Group 1: Karyawan
            item { FeatureGroup("Karyawan", employeeFeatures, onNavigate) }

            // Group 2: HR & Operasional
            item { FeatureGroup("HR & Operasional", hrFeatures, onNavigate) }

            // Group 3: Finansial
            item { FeatureGroup("Finansial", financialFeatures, onNavigate) }

            // Group 4: KPI & Performa
            item { FeatureGroup("KPI & Performa", kpiFeatures, onNavigate) }

            // Group 5: Laporan
            item { FeatureGroup("Laporan", reportFeatures, onNavigate) }

            // Jarak Bawah
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun FeatureGroup(
    title: String,
    features: List<AppFeature>,
    onNavigate: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        // Membuat Grid 4 Kolom mirip dengan UI Referensi
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 1000.dp), // HeightIn agar tidak bentrok dengan LazyColumn
            userScrollEnabled = false // [PERBAIKAN] Menggunakan userScrollEnabled untuk mematikan scroll internal di Jetpack Compose
        ) {
            items(features) { feature ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clickable { onNavigate(feature.route) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(feature.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = feature.title,
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
    }
}