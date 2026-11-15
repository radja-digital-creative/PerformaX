package id.biz.yuka.performax.ui.screen.employee.rule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeRuleEntity
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.request.CreateEmployeeRuleRequest
import id.biz.yuka.libperformx.request.UpdateEmployeeRuleRequest
import id.biz.yuka.performax.ui.theme.WonderGrey
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeRuleScreen(
    onBack: () -> Unit,
    viewModel: EmployeeRuleViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val tealHeaderColor = Color(0xFF9FE2DB)
    val backgroundColor = Color(0xFFF5F6F8)
    val activeTabColor = Color(0xFFE5DE44)

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRuleTypeTab by remember { mutableStateOf("violation") }

    // Bottom Sheet States
    var showCreateRuleSheet by remember { mutableStateOf(false) }
    var showAssignRuleSheet by remember { mutableStateOf(false) }

    // Edit Rule States
    var showEditRuleSheet by remember { mutableStateOf(false) }
    var selectedRuleToEdit by remember { mutableStateOf<EmployeeRuleEntity?>(null) }

    // Remove from Store States
    var showRemoveRuleSheet by remember { mutableStateOf(false) }
    var selectedRuleToRemove by remember { mutableStateOf<EmployeeRuleEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
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

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadData() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().imePadding()) {
                    item {
                        Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.Black) }
                            Text("Aturan & Kedisiplinan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                            IconButton(onClick = { showCreateRuleSheet = true }) { Icon(Icons.Default.Add, "Buat Aturan", tint = Color.Black) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Manajemen Aturan HR", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                            Text(
                                text = "Buat aturan sanksi & reward otomatis dan tugaskan ke cabang toko.",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray),
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

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
                                    Text("Aturan", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal))
                                }
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (selectedTab == 1) activeTabColor else Color.Transparent).clickable { selectedTab = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Toko", style = MaterialTheme.typography.labelLarge.copy(color = Color.Black, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item { Spacer(modifier = Modifier.fillMaxWidth().height(8.dp).background(backgroundColor)) }

                    if (selectedTab == 0) {
                        // ================= TAB 1: ATURAN MASTER =================
                        item {
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1F2EB)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column {
                                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Power by AI HR Disciplinary Agent", fontWeight = FontWeight.Bold, color = Color(0xFF006064), fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Total aturan yg diawasi oleh AI", fontSize = 11.sp, color = Color(0xFF00838F))
                                                    Text("${viewModel.rules.size} Aturan", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Black)
                                                }
                                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF26C6DA), modifier = Modifier.size(48.dp))
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp)) {
                                                Text("Catatan: AI mungkin bisa melakukan salah tafsir.", fontSize = 11.sp, color = Color.DarkGray)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // SUB TABS: VIOLATION (PELANGGARAN) VS BONUS (REWARD)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(44.dp),
                                        shape = RoundedCornerShape(50),
                                        color = Color.White
                                    ) {
                                        Row(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                                            Box(
                                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50))
                                                    .background(if (selectedRuleTypeTab == "violation") Color(0xFFEF5350) else Color.Transparent)
                                                    .clickable { selectedRuleTypeTab = "violation" },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Pelanggaran (Violation)", style = MaterialTheme.typography.labelMedium.copy(color = if (selectedRuleTypeTab == "violation") Color.White else Color.Black, fontWeight = FontWeight.Bold))
                                            }
                                            Box(
                                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50))
                                                    .background(if (selectedRuleTypeTab == "bonus") Color(0xFF38B29C) else Color.Transparent)
                                                    .clickable { selectedRuleTypeTab = "bonus" },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Bonus / Reward", style = MaterialTheme.typography.labelMedium.copy(color = if (selectedRuleTypeTab == "bonus") Color.White else Color.Black, fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }

                                    Text(
                                        if (selectedRuleTypeTab == "violation") "Daftar Aturan Pelanggaran" else "Daftar Aturan Bonus & Reward",
                                        fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                                    )
                                }
                            }
                        }

                        val filteredRules = viewModel.rules.filter { it.ruleType == selectedRuleTypeTab }
                        if (filteredRules.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Belum ada aturan untuk kategori ini.", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(filteredRules) { rule ->
                                Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                    RuleItemCard(rule = rule, onClick = {
                                        selectedRuleToEdit = rule
                                        showEditRuleSheet = true
                                    })
                                }
                            }
                        }
                    } else {
                        // ================= TAB 2: ASSIGNMENT TOKO =================
                        item {
                            Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                Column(modifier = Modifier.fillMaxWidth()) {

                                    val pagerState = rememberPagerState(pageCount = { viewModel.stores.size })

                                    LaunchedEffect(pagerState.currentPage, viewModel.stores) {
                                        if (viewModel.stores.isNotEmpty()) {
                                            viewModel.selectStore(viewModel.stores[pagerState.currentPage])
                                        }
                                    }

                                    if (viewModel.stores.isNotEmpty()) {
                                        HorizontalPager(
                                            state = pagerState,
                                            contentPadding = PaddingValues(horizontal = 24.dp),
                                            pageSpacing = 12.dp,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        ) { page ->
                                            val store = viewModel.stores[page]
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                            ) {
                                                Column {
                                                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("Cabang Toko Aktif", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), fontSize = 14.sp)
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text("Menampilkan aturan untuk:", fontSize = 11.sp, color = Color(0xFF1565C0))
                                                            Text(store.name, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        Icon(Icons.Default.Storefront, contentDescription = "Store", tint = Color(0xFF64B5F6), modifier = Modifier.size(48.dp))
                                                    }
                                                    Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp)) {
                                                        Text("Geser untuk melihat cabang lain. (${page + 1}/${viewModel.stores.size})", fontSize = 11.sp, color = Color.DarkGray)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text("Belum ada toko terdaftar.", modifier = Modifier.padding(24.dp), color = Color.Gray)
                                    }

                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAssignRuleSheet = true }) {
                                            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray), modifier = Modifier.size(56.dp)) {
                                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AddLink, contentDescription = "Assign", tint = Color(0xFF38B29C)) }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Tambah Aturan ke Store", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text("Aturan yg diterapkan di toko ini", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp))
                                }
                            }
                        }

                        if (viewModel.rulesForSelectedStore.isEmpty() && !viewModel.isLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Belum ada aturan khusus", fontWeight = FontWeight.Bold)
                                        Text("Assign aturan dari master ke toko ini.", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            items(viewModel.rulesForSelectedStore) { rule ->
                                Box(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
                                    RuleItemCard(rule = rule, onClick = {
                                        selectedRuleToRemove = rule
                                        showRemoveRuleSheet = true
                                    })
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.fillMaxWidth().height(120.dp).background(backgroundColor)) }
                    item { Spacer(modifier = Modifier.fillMaxWidth().height(500.dp).background(backgroundColor)) }
                }
            }
        }
    }

    // ==========================================
    // BOTTOM SHEETS
    // ==========================================
    if (showCreateRuleSheet) {
        CreateRuleBottomSheet(
            onDismiss = { showCreateRuleSheet = false },
            onSave = { request ->
                viewModel.addRule(request)
                showCreateRuleSheet = false
            }
        )
    }

    // BOTTOM SHEET UNTUK EDIT / DELETE ATURAN MASTER
    if (showEditRuleSheet && selectedRuleToEdit != null) {
        EditRuleBottomSheet(
            initialData = selectedRuleToEdit!!,
            onDismiss = { showEditRuleSheet = false },
            onSave = { request ->
                viewModel.updateRule(selectedRuleToEdit!!.id, request)
                showEditRuleSheet = false
            },
            onDelete = {
                viewModel.deleteRule(selectedRuleToEdit!!.id)
                showEditRuleSheet = false
            }
        )
    }

    if (showAssignRuleSheet) {
        AssignRuleBottomSheet(
            stores = viewModel.stores,
            rules = viewModel.rules,
            defaultSelectedStore = viewModel.selectedStoreForView,
            onDismiss = { showAssignRuleSheet = false },
            onSave = { storeId, ruleId ->
                viewModel.assignRuleToStore(storeId, ruleId)
                showAssignRuleSheet = false
            }
        )
    }

    // BOTTOM SHEET UNTUK REMOVE ATURAN DARI TOKO
    if (showRemoveRuleSheet && selectedRuleToRemove != null && viewModel.selectedStoreForView != null) {
        RemoveAssignedRuleBottomSheet(
            rule = selectedRuleToRemove!!,
            store = viewModel.selectedStoreForView!!,
            onDismiss = { showRemoveRuleSheet = false },
            onRemove = {
                viewModel.removeRuleFromStore(viewModel.selectedStoreForView!!.id, selectedRuleToRemove!!.id)
                showRemoveRuleSheet = false
            }
        )
    }
}

// ==========================================
// CARD: GAYA ORIO3OT3
// ==========================================
// ==========================================
// CARD: GAYA ORIO3OT3
// ==========================================
@Composable
fun RuleItemCard(rule: EmployeeRuleEntity, onClick: () -> Unit) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatter.maximumFractionDigits = 0

    val isBonus = rule.ruleType == "bonus"

    val mappedTriggerType = when (rule.triggerType) {
        "late" -> "Terlambat"
        "early_leave" -> "Pulang Lebih Awal"
        "absent" -> "Tidak Hadir / Alpha"
        "misconduct" -> "Kedisiplinan"
        "task_overdue" -> "Tugas Terlambat"
        "kpi_under_grade" -> "KPI < % (Di Bawah Grade)"
        "kpi_over_grade" -> "KPI > % (Di Atas Grade)"
        else -> rule.triggerType
    }

    val mappedTriggerInfo = if (rule.triggerMode != null && rule.triggerThreshold != null) {
        when (rule.triggerMode) {
            "consecutive" -> "${rule.triggerThreshold} Hari berturut-turut"
            "accumulative_monthly" -> "${rule.triggerThreshold}x selama sebulan"
            else -> if (rule.triggerType == "kpi_under_grade" || rule.triggerType == "kpi_over_grade") "Target: ${rule.triggerThreshold}%" else null
        }
    } else if (rule.triggerThreshold != null && (rule.triggerType == "kpi_under_grade" || rule.triggerType == "kpi_over_grade")) {
        "Target: ${rule.triggerThreshold}%"
    } else null

    val mappedActionType = if (isBonus) {
        when (rule.rewardType) {
            "reward_fixed" -> "Bonus Tunai (Rp)"
            "status_change" -> "Promosi Status"
            else -> rule.rewardType ?: "Bonus"
        }
    } else {
        when (rule.penaltyType) {
            "deduction_fixed" -> "Potongan Tetap (Gaji)"
            "kpi_deduction_fixed" -> "Potongan Poin KPI"
            "deduction_multiplier" -> "Potong Gaji Harian"
            "status_change" -> "Perubahan Status"
            else -> rule.penaltyType ?: "Sanksi"
        }
    }

    val mappedConsequence = if (isBonus) {
        when (rule.rewardType) {
            "reward_fixed" -> formatter.format(rule.rewardFixedAmount)
            "status_change" -> rule.rewardStatusAction ?: "Promosi"
            else -> "-"
        }
    } else {
        when (rule.penaltyType) {
            "deduction_fixed" -> formatter.format(rule.penaltyFixedAmount)
            "kpi_deduction_fixed" -> "${rule.penaltyFixedAmount.toInt()} Poin KPI"
            "deduction_multiplier" -> "${rule.penaltyMultiplier}x Gaji"
            "status_change" -> rule.penaltyStatusAction ?: "Diperbarui"
            else -> "-"
        }
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isBonus) Color(0xFF38B29C) else Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isBonus) "BONUS" else "HR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isBonus) 9.sp else 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = rule.ruleName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = mappedTriggerType, fontSize = 12.sp, color = Color.Gray)
                        if (mappedTriggerInfo != null) {
                            Text(text = " • ", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                            Text(text = mappedTriggerInfo, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!rule.description.isNullOrBlank()) {
                Text(text = rule.description!!, fontSize = 13.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isBonus) Icons.Default.CardGiftcard else Icons.Default.TrendingUp,
                            contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBonus) "Pemberian Reward" else "Sanksi", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = mappedActionType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isBonus) Color(0xFF38B29C) else Color(0xFF4CAF50))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBonus) "Nilai Reward" else "Konsekuensi", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = mappedConsequence, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

// ==========================================
// BOTTOM SHEET 1: BUAT ATURAN BARU
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleBottomSheet(
    onDismiss: () -> Unit,
    onSave: (CreateEmployeeRuleRequest) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var ruleType by remember { mutableStateOf("violation") } // "violation" vs "bonus"

    // Violation states
    var triggerType by remember { mutableStateOf("late") }
    var triggerMode by remember { mutableStateOf("none") }
    var triggerThreshold by remember { mutableStateOf("") }
    var penaltyType by remember { mutableStateOf("deduction_fixed") }
    var penaltyFixedAmount by remember { mutableStateOf("") }
    var penaltyMultiplier by remember { mutableStateOf("") }
    var penaltyStatusAction by remember { mutableStateOf("") }

    // Bonus states
    var rewardType by remember { mutableStateOf("reward_fixed") }
    var rewardFixedAmount by remember { mutableStateOf("") }
    var rewardMultiplier by remember { mutableStateOf("") }
    var rewardStatusAction by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)) {
            Text("Buat Aturan Baru", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Sub-Tab Switch: Violation vs Bonus
            Text("Tipe Kategori Aturan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = ruleType == "violation", onClick = { ruleType = "violation"; triggerType = "late" }, label = { Text("Aturan Pelanggaran (Sanksi)") })
                FilterChip(selected = ruleType == "bonus", onClick = { ruleType = "bonus"; triggerType = "kpi_over_grade" }, label = { Text("Aturan Bonus (Reward)") })
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Aturan") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)

            Spacer(modifier = Modifier.height(16.dp))
            Text(if (ruleType == "violation") "Tipe Pelanggaran" else "Tipe Pemicu Bonus", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            if (ruleType == "violation") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = triggerType == "late", onClick = { triggerType = "late" }, label = { Text("Telat") })
                    FilterChip(selected = triggerType == "early_leave", onClick = { triggerType = "early_leave" }, label = { Text("Pulang Cepat") })
                    FilterChip(selected = triggerType == "absent", onClick = { triggerType = "absent" }, label = { Text("Alfa") })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = triggerType == "misconduct", onClick = { triggerType = "misconduct" }, label = { Text("Kedisiplinan/SP") })
                    FilterChip(selected = triggerType == "task_overdue", onClick = { triggerType = "task_overdue" }, label = { Text("Tugas Terlambat") })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = triggerType == "kpi_under_grade", onClick = { triggerType = "kpi_under_grade" }, label = { Text("KPI < Persen (Di Bawah Grade)") })
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = triggerType == "kpi_over_grade", onClick = { triggerType = "kpi_over_grade" }, label = { Text("KPI > Persen (Di Atas Grade)") })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Mode Pengulangan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = triggerMode == "none", onClick = { triggerMode = "none" }, label = { Text("Langsung") })
                FilterChip(selected = triggerMode == "consecutive", onClick = { triggerMode = "consecutive" }, label = { Text("Berturut-turut") })
                FilterChip(selected = triggerMode == "accumulative_monthly", onClick = { triggerMode = "accumulative_monthly" }, label = { Text("Akumulasi Sebulan") })
            }

            if (triggerMode != "none" || triggerType == "kpi_under_grade" || triggerType == "kpi_over_grade") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = triggerThreshold, onValueChange = { triggerThreshold = it },
                    label = { Text(if (triggerType == "kpi_under_grade" || triggerType == "kpi_over_grade") "Target KPI (Persen / %)" else "Batas Maksimal (Kali/Hari)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (ruleType == "violation") {
                Text("Tipe Sanksi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = penaltyType == "deduction_multiplier", onClick = { penaltyType = "deduction_multiplier" }, label = { Text("Potong Hari (x)") })
                    FilterChip(selected = penaltyType == "deduction_fixed", onClick = { penaltyType = "deduction_fixed" }, label = { Text("Denda (Rp)") })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = penaltyType == "kpi_deduction_fixed", onClick = { penaltyType = "kpi_deduction_fixed" }, label = { Text("Potong KPI (Poin)") })
                    FilterChip(selected = penaltyType == "status_change", onClick = { penaltyType = "status_change" }, label = { Text("Ubah Status") })
                }

                Spacer(modifier = Modifier.height(8.dp))
                when (penaltyType) {
                    "deduction_multiplier" -> OutlinedTextField(value = penaltyMultiplier, onValueChange = { penaltyMultiplier = it }, label = { Text("Pengali Potongan (Misal: 1 atau 1.5)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    "deduction_fixed" -> OutlinedTextField(value = penaltyFixedAmount, onValueChange = { penaltyFixedAmount = it }, label = { Text("Nominal Denda (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    "kpi_deduction_fixed" -> OutlinedTextField(value = penaltyFixedAmount, onValueChange = { penaltyFixedAmount = it }, label = { Text("Nominal Potongan Poin KPI (Misal: 50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    "status_change" -> OutlinedTextField(value = penaltyStatusAction, onValueChange = { penaltyStatusAction = it }, label = { Text("Status Baru (Misal: SP 1, Resigned)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            } else {
                Text("Tipe Hadiah / Reward", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = rewardType == "reward_fixed", onClick = { rewardType = "reward_fixed" }, label = { Text("Bonus Tunai (Rp)") })
                    FilterChip(selected = rewardType == "status_change", onClick = { rewardType = "status_change" }, label = { Text("Ubah Status / Promosi") })
                }

                Spacer(modifier = Modifier.height(8.dp))
                when (rewardType) {
                    "reward_fixed" -> OutlinedTextField(value = rewardFixedAmount, onValueChange = { rewardFixedAmount = it }, label = { Text("Nominal Bonus (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    "status_change" -> OutlinedTextField(value = rewardStatusAction, onValueChange = { rewardStatusAction = it }, label = { Text("Status Baru / Promosi (Misal: Senior, Manager)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    onSave(CreateEmployeeRuleRequest(
                        rule_name = name,
                        description = description.ifBlank { null },
                        trigger_type = triggerType,
                        trigger_mode = if (triggerMode == "none") null else triggerMode,
                        trigger_threshold = triggerThreshold.toIntOrNull(),
                        rule_type = ruleType,
                        penalty_type = if (ruleType == "violation") penaltyType else null,
                        penalty_multiplier = if (ruleType == "violation") (penaltyMultiplier.toDoubleOrNull() ?: 0.0) else 0.0,
                        penalty_fixed_amount = if (ruleType == "violation") (penaltyFixedAmount.toDoubleOrNull() ?: 0.0) else 0.0,
                        penalty_status_action = if (ruleType == "violation") (penaltyStatusAction.ifBlank { null }) else null,
                        reward_type = if (ruleType == "bonus") rewardType else null,
                        reward_multiplier = if (ruleType == "bonus") (rewardMultiplier.toDoubleOrNull() ?: 0.0) else 0.0,
                        reward_fixed_amount = if (ruleType == "bonus") (rewardFixedAmount.toDoubleOrNull() ?: 0.0) else 0.0,
                        reward_status_action = if (ruleType == "bonus") (rewardStatusAction.ifBlank { null }) else null
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C)), shape = RoundedCornerShape(12.dp)
            ) { Text("Simpan Aturan", fontWeight = FontWeight.Bold) }
        }
    }
}

// ==========================================
// BOTTOM SHEET: EDIT & DELETE ATURAN MASTER
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleBottomSheet(
    initialData: EmployeeRuleEntity,
    onDismiss: () -> Unit,
    onSave: (UpdateEmployeeRuleRequest) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialData.ruleName) }
    var description by remember { mutableStateOf(initialData.description ?: "") }
    var ruleType by remember { mutableStateOf(initialData.ruleType) }
    var triggerType by remember { mutableStateOf(initialData.triggerType) }
    var triggerMode by remember { mutableStateOf(initialData.triggerMode ?: "none") }
    var triggerThreshold by remember { mutableStateOf(initialData.triggerThreshold?.toString() ?: "") }

    // Violation states
    var penaltyType by remember { mutableStateOf(initialData.penaltyType ?: "deduction_fixed") }
    var penaltyFixedAmount by remember { mutableStateOf(if(initialData.penaltyFixedAmount > 0) initialData.penaltyFixedAmount.toLong().toString() else "") }
    var penaltyMultiplier by remember { mutableStateOf(if(initialData.penaltyMultiplier > 0) initialData.penaltyMultiplier.toString() else "") }
    var penaltyStatusAction by remember { mutableStateOf(initialData.penaltyStatusAction ?: "") }

    // Bonus states
    var rewardType by remember { mutableStateOf(initialData.rewardType ?: "reward_fixed") }
    var rewardFixedAmount by remember { mutableStateOf(if(initialData.rewardFixedAmount > 0) initialData.rewardFixedAmount.toLong().toString() else "") }
    var rewardMultiplier by remember { mutableStateOf(if(initialData.rewardMultiplier > 0) initialData.rewardMultiplier.toString() else "") }
    var rewardStatusAction by remember { mutableStateOf(initialData.rewardStatusAction ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)) {
            Text("Edit Aturan", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Sub-Tab Switch: Violation vs Bonus
            Text("Tipe Kategori Aturan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = ruleType == "violation", onClick = { ruleType = "violation" }, label = { Text("Aturan Pelanggaran (Sanksi)") })
                FilterChip(selected = ruleType == "bonus", onClick = { ruleType = "bonus" }, label = { Text("Aturan Bonus (Reward)") })
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Aturan") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)

            Spacer(modifier = Modifier.height(16.dp))
            Text(if (ruleType == "violation") "Tipe Pelanggaran" else "Tipe Pemicu Bonus", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            if (ruleType == "violation") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = triggerType == "late", onClick = { triggerType = "late" }, label = { Text("Telat") })
                    FilterChip(selected = triggerType == "early_leave", onClick = { triggerType = "early_leave" }, label = { Text("Pulang Cepat") })
                    FilterChip(selected = triggerType == "absent", onClick = { triggerType = "absent" }, label = { Text("Alfa") })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = triggerType == "misconduct", onClick = { triggerType = "misconduct" }, label = { Text("Kedisiplinan/SP") })
                    FilterChip(selected = triggerType == "task_overdue", onClick = { triggerType = "task_overdue" }, label = { Text("Tugas Terlambat") })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = triggerType == "kpi_under_grade", onClick = { triggerType = "kpi_under_grade" }, label = { Text("KPI < Persen (Di Bawah Grade)") })
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = triggerType == "kpi_over_grade", onClick = { triggerType = "kpi_over_grade" }, label = { Text("KPI > Persen (Di Atas Grade)") })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Mode Pengulangan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = triggerMode == "none", onClick = { triggerMode = "none" }, label = { Text("Langsung") })
                FilterChip(selected = triggerMode == "consecutive", onClick = { triggerMode = "consecutive" }, label = { Text("Berturut-turut") })
                FilterChip(selected = triggerMode == "accumulative_monthly", onClick = { triggerMode = "accumulative_monthly" }, label = { Text("Akumulasi Sebulan") })
            }

            if (triggerMode != "none" || triggerType == "kpi_under_grade" || triggerType == "kpi_over_grade") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = triggerThreshold, onValueChange = { triggerThreshold = it },
                    label = { Text(if (triggerType == "kpi_under_grade" || triggerType == "kpi_over_grade") "Target KPI (Persen / %)" else "Batas Maksimal (Kali/Hari)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (ruleType == "violation") {
                Text("Tipe Sanksi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = penaltyType == "deduction_multiplier", onClick = { penaltyType = "deduction_multiplier" }, label = { Text("Potong Hari (x)") })
                    FilterChip(selected = penaltyType == "deduction_fixed", onClick = { penaltyType = "deduction_fixed" }, label = { Text("Denda (Rp)") })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = penaltyType == "kpi_deduction_fixed", onClick = { penaltyType = "kpi_deduction_fixed" }, label = { Text("Potong KPI (Poin)") })
                    FilterChip(selected = penaltyType == "status_change", onClick = { penaltyType = "status_change" }, label = { Text("Ubah Status") })
                }

                Spacer(modifier = Modifier.height(8.dp))
                when (penaltyType) {
                    "deduction_multiplier" -> OutlinedTextField(value = penaltyMultiplier, onValueChange = { penaltyMultiplier = it }, label = { Text("Pengali Potongan (Misal: 1 atau 1.5)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    "deduction_fixed" -> OutlinedTextField(value = penaltyFixedAmount, onValueChange = { penaltyFixedAmount = it }, label = { Text("Nominal Denda (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    "kpi_deduction_fixed" -> OutlinedTextField(value = penaltyFixedAmount, onValueChange = { penaltyFixedAmount = it }, label = { Text("Nominal Potongan Poin KPI (Misal: 50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    "status_change" -> OutlinedTextField(value = penaltyStatusAction, onValueChange = { penaltyStatusAction = it }, label = { Text("Status Baru (Misal: SP 1, Resigned)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            } else {
                Text("Tipe Hadiah / Reward", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = rewardType == "reward_fixed", onClick = { rewardType = "reward_fixed" }, label = { Text("Bonus Tunai (Rp)") })
                    FilterChip(selected = rewardType == "status_change", onClick = { rewardType = "status_change" }, label = { Text("Ubah Status / Promosi") })
                }

                Spacer(modifier = Modifier.height(8.dp))
                when (rewardType) {
                    "reward_fixed" -> OutlinedTextField(value = rewardFixedAmount, onValueChange = { rewardFixedAmount = it }, label = { Text("Nominal Bonus (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    "status_change" -> OutlinedTextField(value = rewardStatusAction, onValueChange = { rewardStatusAction = it }, label = { Text("Status Baru / Promosi (Misal: Senior, Manager)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TOMBOL SIMPAN & HAPUS
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
                        onSave(UpdateEmployeeRuleRequest(
                            rule_name = name,
                            description = description.ifBlank { null },
                            trigger_type = triggerType,
                            trigger_mode = if (triggerMode == "none") null else triggerMode,
                            trigger_threshold = triggerThreshold.toIntOrNull(),
                            rule_type = ruleType,
                            penalty_type = if (ruleType == "violation") penaltyType else null,
                            penalty_multiplier = if (ruleType == "violation") (penaltyMultiplier.toDoubleOrNull() ?: 0.0) else 0.0,
                            penalty_fixed_amount = if (ruleType == "violation") (penaltyFixedAmount.toDoubleOrNull() ?: 0.0) else 0.0,
                            penalty_status_action = if (ruleType == "violation") (penaltyStatusAction.ifBlank { null }) else null,
                            reward_type = if (ruleType == "bonus") rewardType else null,
                            reward_multiplier = if (ruleType == "bonus") (rewardMultiplier.toDoubleOrNull() ?: 0.0) else 0.0,
                            reward_fixed_amount = if (ruleType == "bonus") (rewardFixedAmount.toDoubleOrNull() ?: 0.0) else 0.0,
                            reward_status_action = if (ruleType == "bonus") (rewardStatusAction.ifBlank { null }) else null
                        ))
                    },
                    modifier = Modifier.weight(1f).padding(start = 16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Simpan Perubahan", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
// ==========================================
// BOTTOM SHEET 2: ASSIGN RULE KE STORE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignRuleBottomSheet(
    stores: List<StoreEntity>,
    rules: List<EmployeeRuleEntity>,
    defaultSelectedStore: StoreEntity?,
    onDismiss: () -> Unit,
    onSave: (storeId: String, ruleId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var expandedStore by remember { mutableStateOf(false) }
    var selectedStore by remember { mutableStateOf(defaultSelectedStore ?: stores.firstOrNull()) }

    var expandedRule by remember { mutableStateOf(false) }
    var selectedRule by remember { mutableStateOf(rules.firstOrNull()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)) {
            Text("Tambah Aturan ke Store", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = expandedStore, onExpandedChange = { expandedStore = !expandedStore }) {
                OutlinedTextField(
                    value = selectedStore?.name ?: "Pilih Toko", onValueChange = {}, readOnly = true,
                    label = { Text("Tugas ke Cabang") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStore) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expandedStore, onDismissRequest = { expandedStore = false }) {
                    stores.forEach { store -> DropdownMenuItem(text = { Text(store.name) }, onClick = { selectedStore = store; expandedStore = false }) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = expandedRule, onExpandedChange = { expandedRule = !expandedRule }) {
                OutlinedTextField(
                    value = selectedRule?.ruleName ?: "Pilih Aturan", onValueChange = {}, readOnly = true,
                    label = { Text("Pilih Aturan Karyawan") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRule) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expandedRule, onDismissRequest = { expandedRule = false }) {
                    rules.forEach { rule -> DropdownMenuItem(text = { Text(rule.ruleName) }, onClick = { selectedRule = rule; expandedRule = false }) }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { if (selectedStore != null && selectedRule != null) onSave(selectedStore!!.id, selectedRule!!.id) },
                modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B29C)), shape = RoundedCornerShape(12.dp)
            ) { Text("Tugaskan Aturan", fontWeight = FontWeight.Bold) }
        }
    }
}

// ==========================================
// BOTTOM SHEET 3: REMOVE ASSIGNED RULE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveAssignedRuleBottomSheet(
    rule: EmployeeRuleEntity,
    store: StoreEntity,
    onDismiss: () -> Unit,
    onRemove: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Peringatan", tint = Color(0xFFFFB300), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Lepaskan Aturan", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Apakah anda yakin ingin melepaskan aturan ${rule.ruleName} dari cabang ${store.name}?",
                color = Color.DarkGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Batal", color = Color.Gray) }

                Button(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Lepaskan", fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
    }
}