package id.biz.yuka.performax.ui.screen.salary.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSalaryScreen(
    onBack: () -> Unit,
    onSelectEmployee: (String) -> Unit,
    viewModel: AdminSalaryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val limeTab = Color(0xFFD4E157)

    // Tampilkan pesan PDF via Snackbar
    LaunchedEffect(viewModel.pdfMessage) {
        viewModel.pdfMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.pdfMessage = null
        }
    }

    Scaffold(
        containerColor = Color(0xFFF9F9F9),
        topBar = {
            TopAppBar(
                title = { Text("Daftar Gaji Karyawan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    // Tombol Download Rekap PDF
                    if (viewModel.isPdfGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.generateRekapPdf(context) },
                            enabled = viewModel.employees.isNotEmpty() && !viewModel.isLoading
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Download Rekap PDF",
                                tint = if (viewModel.employees.isNotEmpty()) WonderOrange else Color.LightGray
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Filter Periode (Navigasi Bulan) ──────────────────────────
            if (viewModel.availableMonths.isNotEmpty()) {
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Navigasi Kiri (Lebih Lampau)
                        IconButton(
                            onClick = { viewModel.previousMonth() },
                            enabled = viewModel.selectedMonthIndex < viewModel.availableMonths.size - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Bulan Sebelumnya",
                                tint = if (viewModel.selectedMonthIndex < viewModel.availableMonths.size - 1)
                                    Color.Black else Color.LightGray
                            )
                        }

                        // Label Periode
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = limeTab
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    viewModel.selectedMonth,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    viewModel.periodString,
                                    fontSize = 10.sp,
                                    color = Color.Black.copy(alpha = 0.65f)
                                )
                            }
                        }

                        // Navigasi Kanan (Lebih Baru)
                        IconButton(
                            onClick = { viewModel.nextMonth() },
                            enabled = viewModel.selectedMonthIndex > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Bulan Selanjutnya",
                                tint = if (viewModel.selectedMonthIndex > 0) Color.Black else Color.LightGray
                            )
                        }
                    }
                }
            }

            // ── Daftar Karyawan ─────────────────────────────────────────
            if (viewModel.isLoading && viewModel.employees.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Info jumlah karyawan & periode
                    item {
                        Surface(
                            color = WonderOrange.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${viewModel.employees.size} karyawan aktif  •  ${viewModel.selectedMonth}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WonderOrange
                            )
                        }
                    }

                    items(viewModel.employees) { employee ->
                        EmployeeSalaryCard(
                            employee = employee,
                            onClick = { onSelectEmployee(employee.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun EmployeeSalaryCard(
    employee: EmployeeEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFF0F0F0))) {
                if (employee.profileUrl.isNotBlank()) {
                    AsyncImage(
                        model = employee.profileUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(employee.roleName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}
