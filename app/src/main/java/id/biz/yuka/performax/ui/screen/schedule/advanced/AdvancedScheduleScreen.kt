package id.biz.yuka.performax.ui.screen.schedule.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScheduleScreen(
    onBack: () -> Unit,
    viewModel: AdvancedScheduleViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            delay(2000)
            viewModel.successMessage = null
        }
    }

    val today = LocalDate.now()
    val localeId = Locale("id", "ID")

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Advanced Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (viewModel.errorMessage != null) {
                item { Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp)) } }
            }
            if (viewModel.successMessage != null) {
                item { Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(viewModel.successMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) } }
            }

            // 1. DROPDOWN KARYAWAN
            item {
                Text("Pilih Karyawan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                var expandedEmployee by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedEmployee,
                    onExpandedChange = { expandedEmployee = it }
                ) {
                    OutlinedTextField(
                        value = viewModel.selectedEmployee?.fullName ?: "Pilih Karyawan",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEmployee) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedEmployee, onDismissRequest = { expandedEmployee = false }) {
                        viewModel.availableEmployees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text(emp.fullName) },
                                onClick = { viewModel.selectEmployee(emp); expandedEmployee = false }
                            )
                        }
                    }
                }
            }

            // 2. TABS TOKO KARYAWAN
            if (viewModel.selectedEmployee != null) {
                item {
                    Text("Penempatan Toko", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val stores = viewModel.selectedEmployee!!.stores
                    if (stores.isNotEmpty()) {
                        val selectedIndex = stores.indexOfFirst { it.id == viewModel.selectedStoreId }.takeIf { it >= 0 } ?: 0
                        ScrollableTabRow(
                            selectedTabIndex = selectedIndex,
                            containerColor = Color.Transparent,
                            contentColor = WonderOrange,
                            edgePadding = 0.dp,
                            indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]), color = WonderOrange) }
                        ) {
                            stores.forEachIndexed { index, store ->
                                Tab(
                                    selected = selectedIndex == index,
                                    onClick = { viewModel.selectStore(store.id) },
                                    text = { Text(text = store.name, fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal, color = if (selectedIndex == index) WonderOrange else WonderGrey) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. LEGEND SHIFT WARNA
            if (viewModel.selectedStoreId.isNotBlank() && viewModel.availableShifts.isNotEmpty()) {
                item {
                    Text("Legend Shift", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Grid Keterangan Warna
                    Column(modifier = Modifier.fillMaxWidth()) {
                        viewModel.availableShifts.chunked(2).forEach { rowShifts ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowShifts.forEach { shift ->
                                    val shiftColor = viewModel.shiftColors[shift.id] ?: Color.Gray
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(shiftColor))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(shift.shiftName, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        // Legend Libur / Kosong
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(0xFFE0E0E0)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Libur / Belum Ada Jadwal", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 4. KALENDER INTERAKTIF
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // A. Header Kalender (Bulan & Navigasi)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isCurrentMonth = viewModel.currentYearMonth == YearMonth.now()
                                IconButton(
                                    onClick = { viewModel.changeMonth(-1) },
                                    enabled = !isCurrentMonth // Tidak bisa kembali ke bulan lalu
                                ) {
                                    Icon(Icons.Default.ChevronLeft, "Previous Month", tint = if (isCurrentMonth) Color.LightGray else Color.Black)
                                }

                                Text(
                                    text = viewModel.currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", localeId)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                IconButton(onClick = { viewModel.changeMonth(1) }) {
                                    Icon(Icons.Default.ChevronRight, "Next Month")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // B. Nama Hari
                            val daysOfWeek = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                            Row(modifier = Modifier.fillMaxWidth()) {
                                daysOfWeek.forEach { day ->
                                    Text(
                                        text = day,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WonderGrey
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // C. Grid Tanggal
                            val daysInMonth = viewModel.currentYearMonth.lengthOfMonth()
                            val firstDayOfMonth = viewModel.currentYearMonth.atDay(1).dayOfWeek.value // 1 = Senin, 7 = Minggu

                            // Kosongkan sel sebelum tanggal 1
                            var currentDay = 1
                            var emptyCells = firstDayOfMonth - 1

                            while (currentDay <= daysInMonth) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (i in 0..6) {
                                        if (emptyCells > 0) {
                                            // Sel kosong
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                            emptyCells--
                                        } else if (currentDay <= daysInMonth) {
                                            val date = viewModel.currentYearMonth.atDay(currentDay)
                                            val isPast = date.isBefore(today)
                                            val schedule = viewModel.monthlySchedules[date]
                                            val others = viewModel.otherStoreSchedules[date]

                                            // Warna Lingkaran
                                            val circleColor = if (schedule != null) {
                                                viewModel.shiftColors[schedule.shift_id] ?: Color.Gray
                                            } else {
                                                Color(0xFFE0E0E0) // Abu-abu jika tidak ada jadwal (Libur)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isPast) Color.Transparent else circleColor)
                                                    .clickable(enabled = !isPast) { viewModel.openEditModal(date) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = currentDay.toString(),
                                                        color = if (isPast) Color.LightGray else if (schedule != null) Color.White else Color.Black,
                                                        fontWeight = if (date == today) FontWeight.ExtraBold else FontWeight.Normal,
                                                        fontSize = 14.sp
                                                    )
                                                    
                                                    // Dot Indicator untuk Toko Lain (Collision)
                                                    if (!isPast && !others.isNullOrEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.Red)
                                                        )
                                                    }
                                                }
                                            }
                                            currentDay++
                                        } else {
                                            // Sel kosong di akhir bulan
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // MODAL EDIT SHIFT
        // ==========================================
        if (viewModel.showShiftModal && viewModel.selectedDateForEdit != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.showShiftModal = false },
                containerColor = Color.White
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                    Text(
                        text = "Atur Jadwal: ${viewModel.selectedDateForEdit!!.format(DateTimeFormatter.ofPattern("dd MMM yyyy", localeId))}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val currentSchedule = viewModel.monthlySchedules[viewModel.selectedDateForEdit!!]
                    val otherStores = viewModel.otherStoreSchedules[viewModel.selectedDateForEdit!!]

                    if (!otherStores.isNullOrEmpty()) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AddAlert, contentDescription = null, tint = WonderOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Karyawan ini juga memiliki jadwal di: ${otherStores.joinToString(", ")} pada tanggal ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    // Pilihan Shift
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(viewModel.availableShifts.size) { index ->
                            val shift = viewModel.availableShifts[index]
                            val isSelected = currentSchedule?.shift_id == shift.id
                            val shiftColor = viewModel.shiftColors[shift.id] ?: Color.Gray

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.saveScheduleOverride(currentSchedule?.schedule_id, shift.id) }
                                    .background(if (isSelected) shiftColor.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(shiftColor))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(shift.shiftName, fontWeight = FontWeight.Bold)
                                    Text("${shift.startTime.take(5)} - ${shift.endTime.take(5)}", style = MaterialTheme.typography.bodySmall, color = WonderGrey)
                                }
                            }
                        }

                        // Pilihan Libur (Hapus Jadwal)
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.saveScheduleOverride(currentSchedule?.schedule_id,null) } // Null = Libur
                                    .background(if (currentSchedule == null) Color(0xFFE0E0E0).copy(alpha = 0.5f) else Color.Transparent)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(0xFFE0E0E0)))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Libur / Kosongkan Jadwal", fontWeight = FontWeight.Bold, color = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}