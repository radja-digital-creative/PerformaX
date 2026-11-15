package id.biz.yuka.performax.ui.screen.task.settings.taskreport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeTaskReportEntity
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskReportScreen(
    onBack: () -> Unit,
    viewModel: TaskReportViewModel = hiltViewModel()
) {
    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Laporan Tugas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── FILTER SECTION ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Filter Laporan", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Dropdown Periode
                    var periodExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = periodExpanded,
                        onExpandedChange = { periodExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedPeriod.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Periode") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = periodExpanded,
                            onDismissRequest = { periodExpanded = false }
                        ) {
                            TaskReportPeriod.entries.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.label) },
                                    onClick = {
                                        viewModel.updatePeriod(period)
                                        periodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Dropdown Nama Karyawan (Hanya tampil jika data tersedia)
                    if (viewModel.availableEmployeeNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        var empExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = empExpanded,
                            onExpandedChange = { empExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = viewModel.selectedEmployeeName ?: "Semua Karyawan",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Filter Karyawan") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = empExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = empExpanded,
                                onDismissRequest = { empExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Semua Karyawan") },
                                    onClick = {
                                        viewModel.updateEmployeeFilter(null)
                                        empExpanded = false
                                    }
                                )
                                viewModel.availableEmployeeNames.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            viewModel.updateEmployeeFilter(name)
                                            empExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── DATA SECTION ───
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WonderOrange)
                }
            } else if (viewModel.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(viewModel.errorMessage!!, color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else if (viewModel.filteredReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tidak ada data laporan", color = WonderGrey)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.filteredReports) { report ->
                        ReportItemCard(report)
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReportItemCard(report: EmployeeTaskReportEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(report.employeeName, fontWeight = FontWeight.Black, color = WonderOrange, fontSize = 12.sp)
                
                val statusColor = when(report.status.lowercase()) {
                    "completed" -> Color(0xFF4CAF50)
                    "verified" -> Color(0xFF2E7D32)
                    "in_progress" -> Color(0xFF2196F3)
                    else -> Color.Gray
                }
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        report.status.uppercase(), 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(report.taskTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp), tint = WonderGrey)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dibuat: ${report.createdAt.take(10)}", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Timer, null, modifier = Modifier.size(14.dp), tint = WonderGrey)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Deadline: ${report.deadline.take(10)}", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
            }
        }
    }
}
