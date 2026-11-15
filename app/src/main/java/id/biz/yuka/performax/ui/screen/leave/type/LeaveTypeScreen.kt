package id.biz.yuka.performax.ui.screen.leave.type

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.LeaveTypeEntity
import id.biz.yuka.libperformx.request.CreateLeaveTypeRequest
import id.biz.yuka.libperformx.request.UpdateLeaveTypeRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveTypeScreen(
    onBack: () -> Unit,
    viewModel: LeaveTypeViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)
    val activeTabColor = Color(0xFFE5DE44)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedLeaveType by remember { mutableStateOf<LeaveTypeEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // LAYER 1: Background Header Statis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .drawBehind {
                    drawRect(color = tealHeaderColor, size = Size(size.width, size.height))
                    drawCircle(color = Color.White.copy(alpha = 0.2f), radius = 250.dp.toPx(), center = Offset(size.width * 1.1f, -50.dp.toPx()))
                    drawCircle(color = Color.White.copy(alpha = 0.1f), radius = 120.dp.toPx(), center = Offset(size.width * 0.9f, size.height * 0.4f))
                }
        )

        // LAYER 2: Scaffold Transparan
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadLeaveTypes() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().imePadding()) {
                    item {
                        Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 16.dp))

                        // ROW 1: HEADER NAVIGATION
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.Black) }
                            Text("Master Tipe Izin", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                            IconButton(onClick = { }) { Icon(Icons.Default.Description, "Icon Kanan", tint = Color.Transparent) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ROW 2: DESKRIPSI HALAMAN
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Kategori Izin & Cuti", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                            Text(
                                text = "Kelola daftar tipe izin yang dapat diajukan oleh karyawan beserta aturannya secara dinamis.",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray),
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ROW 3: CUSTOM TABS
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (selectedTab == 0) activeTabColor else Color.Transparent).clickable { selectedTab = 0 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Daftar Tipe", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal))
                                }
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (selectedTab == 1) activeTabColor else Color.Transparent).clickable { selectedTab = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Buat Baru", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // ROW 4: LIST ITEM
                    item { Spacer(modifier = Modifier.fillMaxWidth().height(8.dp).background(backgroundColor)) }

                    if (selectedTab == 0) {
                        if (viewModel.leaveTypes.isEmpty() && !viewModel.isLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("Belum ada kategori izin yang dibuat.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(viewModel.leaveTypes) { leaveType ->
                                Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                    LeaveTypeGrowthCard(
                                        leaveType = leaveType,
                                        onClick = {
                                            selectedLeaveType = leaveType
                                            showEditSheet = true
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                CreateLeaveTypeForm(
                                    onSave = { request ->
                                        viewModel.addLeaveType(request)
                                        selectedTab = 0
                                    }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.fillMaxWidth().height(120.dp).background(backgroundColor)) }
                    item { Spacer(modifier = Modifier.fillMaxWidth().height(500.dp).background(backgroundColor)) }
                }
            }
        }
    }

    // BOTTOM SHEET UNTUK FORM EDIT
    if (showEditSheet && selectedLeaveType != null) {
        LeaveTypeEditBottomSheet(
            initialData = selectedLeaveType!!,
            onDismiss = { showEditSheet = false },
            onSave = { request ->
                viewModel.updateLeaveType(selectedLeaveType!!.id, request)
                showEditSheet = false
            },
            onDelete = {
                viewModel.deleteLeaveType(selectedLeaveType!!.id)
                showEditSheet = false
            }
        )
    }
}

// ==========================================
// KARTU GROWTH CARD UNTUK LEAVE TYPE
// ==========================================
@Composable
fun LeaveTypeGrowthCard(leaveType: LeaveTypeEntity, onClick: () -> Unit) {
    // Hijau Light jika berbayar, Orange/Peach jika potong gaji
    val categoryColor = if (leaveType.isPaidLeave) Color(0xFFC8E6C9) else Color(0xFFFFCCBC)
    val badgeLabel = if (leaveType.isPaidLeave) "Cuti Berbayar" else "Potong Gaji (Unpaid)"

    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(100.dp)
                    .clip(RoundedCornerShape(topEnd = 16.dp))
            ) {
                Box(modifier = Modifier.size(80.dp).offset(x = 40.dp, y = (-20).dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.6f)))
                Box(modifier = Modifier.size(50.dp).offset(x = 10.dp, y = 20.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.8f)))
            }

            Column {
                Surface(
                    color = categoryColor,
                    shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Text(
                        text = leaveType.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = leaveType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (leaveType.rejectionPenaltyMultiplier > 0) "${leaveType.rejectionPenaltyMultiplier}x Denda Gaji (Jika Mangkir)" else "Tanpa Denda Mangkir",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (leaveType.rejectionPenaltyMultiplier > 0) Color(0xFFE57373) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// FORM BUAT BARU
// ==========================================
@Composable
fun CreateLeaveTypeForm(onSave: (CreateLeaveTypeRequest) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPaidLeave by remember { mutableStateOf(false) }
    var penaltyMultiplier by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp)) {
        Text("Rincian Tipe Izin", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama (Cth: Cuti Tahunan, Izin Sakit)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Deskripsi (Opsional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Aturan Finansial", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Toggle Is Paid Leave
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cuti Berbayar (Paid Leave)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text("Jika aktif, gaji harian tidak akan dipotong saat izin.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Switch(checked = isPaidLeave, onCheckedChange = { isPaidLeave = it })
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = penaltyMultiplier,
            onValueChange = { penaltyMultiplier = it },
            label = { Text("Denda Penolakan (Multiplier Gaji)") },
            placeholder = { Text("Contoh: 1.5 atau 3") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Text("Berapa kali lipat denda gaji harian yang dibebankan jika izin ditolak namun karyawan tetap mangkir.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        CreateLeaveTypeRequest(
                            name = name,
                            description = description.ifBlank { null },
                            is_paid_leave = isPaidLeave,
                            rejection_penalty_multiplier = penaltyMultiplier.toDoubleOrNull() ?: 0.0
                        )
                    )
                    name = ""; description = ""; isPaidLeave = false; penaltyMultiplier = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C))
        ) {
            Text("Simpan Kategori Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ==========================================
// BOTTOM SHEET EDIT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveTypeEditBottomSheet(
    initialData: LeaveTypeEntity,
    onDismiss: () -> Unit,
    onSave: (UpdateLeaveTypeRequest) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialData.name) }
    var description by remember { mutableStateOf(initialData.description) }
    var isPaidLeave by remember { mutableStateOf(initialData.isPaidLeave) }
    var penaltyMultiplier by remember { mutableStateOf(if (initialData.rejectionPenaltyMultiplier > 0) initialData.rejectionPenaltyMultiplier.toString() else "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Text("Edit Tipe Izin", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Is Paid Leave
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cuti Berbayar (Paid Leave)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Switch(checked = isPaidLeave, onCheckedChange = { isPaidLeave = it })
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = penaltyMultiplier,
                onValueChange = { penaltyMultiplier = it },
                label = { Text("Denda Penolakan (Multiplier)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFE57373))
                }

                Button(
                    onClick = {
                        onSave(UpdateLeaveTypeRequest(
                            name = name,
                            description = description.ifBlank { null },
                            is_paid_leave = isPaidLeave,
                            rejection_penalty_multiplier = penaltyMultiplier.toDoubleOrNull() ?: 0.0
                        ))
                    },
                    modifier = Modifier.weight(1f).padding(start = 16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}