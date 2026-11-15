package id.biz.yuka.performax.ui.screen.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderLime
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    onNavigateToShiftList: () -> Unit,
    onNavigateToGenerate: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val dateListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    var stickyHeaderHeight by remember { mutableStateOf(0.dp) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState()
    
    val groupedSchedules = viewModel.getGroupedSchedules()
    val dates = groupedSchedules.keys.toList()
    
    val dateToScrollIndex = remember(groupedSchedules) {
        val mapping = mutableMapOf<LocalDate, Int>()
        var currentIndex = 1 // After sticky header
        dates.forEach { date ->
            mapping[date] = currentIndex
            currentIndex += 1 + (groupedSchedules[date]?.size ?: 0)
        }
        mapping
    }

    LaunchedEffect(groupedSchedules, stickyHeaderHeight) {
        if (groupedSchedules.isNotEmpty() && stickyHeaderHeight > 0.dp) {
            val today = LocalDate.now()
            val targetDate = dates.minByOrNull { Math.abs(it.toEpochDay() - today.toEpochDay()) }
            targetDate?.let { date ->
                dateToScrollIndex[date]?.let { index ->
                    listState.scrollToItem(index, -with(density) { stickyHeaderHeight.roundToPx() })
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstIndex ->
                if (firstIndex >= 1) {
                    val activeDate = dateToScrollIndex.entries
                        .lastOrNull { it.value <= firstIndex }
                        ?.key
                    
                    activeDate?.let { date ->
                        viewModel.updateSelectedDate(date)
                        val dateIndex = viewModel.dateList.indexOf(date)
                        if (dateIndex != -1) {
                            dateListState.animateScrollToItem(maxOf(0, dateIndex - 2))
                        }
                    }
                }
            }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Jadwal Kerja", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Info, null) }
                    IconButton(onClick = { }) { Icon(Icons.Default.Settings, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadSchedules() },
                state = pullToRefreshState,
                indicator = {
                    if (viewModel.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = WonderOrange,
                            trackColor = WonderLime
                        )
                    }
                }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    stickyHeader {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    stickyHeaderHeight = with(density) { coordinates.size.height.toDp() }
                                },
                            color = WonderBg,
                            shadowElevation = 2.dp
                        ) {
                            Column {
                                if (viewModel.userRole == "owner" || viewModel.userRole == "manager") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        ScheduleQuickAction(
                                            label = "Shift",
                                            icon = Icons.Default.Schedule,
                                            color = Color(0xFFE0F2F1),
                                            onClick = { onNavigateToShiftList() }
                                        )
                                        // ✅ UBAH DUA TOMBOL INI
                                        ScheduleQuickAction(
                                            label = "Generate",
                                            icon = Icons.Default.AutoMode,
                                            color = Color(0xFFFFF3E0),
                                            onClick = { onNavigateToGenerate() }
                                        )
                                        ScheduleQuickAction(
                                            label = "Advanced",
                                            icon = Icons.Default.EditCalendar,
                                            color = Color(0xFFE1F5FE),
                                            onClick = { onNavigateToAdvanced() }
                                        )
                                    }
                                } else {
                                    // Berikan sedikit jarak atas agar UI tidak terlalu mepet untuk karyawan biasa
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Jadwal Saya",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    Text(
                                        text = "Filter",
                                        style = MaterialTheme.typography.labelLarge.copy(color = WonderOrange, fontWeight = FontWeight.Bold)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                val localeId = Locale("id", "ID")
                                val dayFormatter = DateTimeFormatter.ofPattern("d MMM", localeId)
                                
                                LazyRow(
                                    state = dateListState,
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(viewModel.dateList) { date ->
                                        val isSelected = date == viewModel.selectedDate
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { 
                                                viewModel.updateSelectedDate(date)
                                                dateToScrollIndex[date]?.let { index ->
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(index, -with(density) { stickyHeaderHeight.roundToPx() })
                                                    }
                                                }
                                            },
                                            label = { Text(date.format(dayFormatter)) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = WonderLime,
                                                selectedLabelColor = Color.Black,
                                                containerColor = Color(0xFFF0F0F0)
                                            ),
                                            border = null,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    }
                                    item {
                                        IconButton(onClick = { showDatePicker = true }) {
                                            Icon(
                                                Icons.Default.FilterList, 
                                                contentDescription = "Filter Tanggal", 
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    if (groupedSchedules.isEmpty() && !viewModel.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.5f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada jadwal",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WonderGrey
                                )
                            }
                        }
                    } else {
                        groupedSchedules.forEach { (date, items) ->
                            item(key = "header_$date") {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    val localeId = Locale("id", "ID")
                                    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", localeId)
                                    Text(
                                        text = date.format(formatter),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                            items(items, key = { it.schedule_id }) { item ->
                                ScheduleMutasiItem(item)
                            }
                        }
                    }
                }
            }

            if (showDatePicker) {
                ModalBottomSheet(
                    onDismissRequest = { showDatePicker = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Pilih Rentang Jadwal", 
                                    style = MaterialTheme.typography.titleLarge, 
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Minimal rentang 7 hari",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WonderGrey
                                )
                            }
                            
                            val isRangeValid = datePickerState.selectedStartDateMillis != null && 
                                             datePickerState.selectedEndDateMillis != null &&
                                             ChronoUnit.DAYS.between(
                                                 Instant.ofEpochMilli(datePickerState.selectedStartDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate(),
                                                 Instant.ofEpochMilli(datePickerState.selectedEndDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate()
                                             ) >= 6

                            Button(
                                onClick = { 
                                    val start = Instant.ofEpochMilli(datePickerState.selectedStartDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate()
                                    val end = Instant.ofEpochMilli(datePickerState.selectedEndDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate()
                                    viewModel.updateDateRange(start, end)
                                    showDatePicker = false
                                },
                                enabled = isRangeValid,
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Terapkan", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        HorizontalDivider(color = Color(0xFFF0F0F0))

                        DateRangePicker(
                            state = datePickerState,
                            modifier = Modifier.height(450.dp),
                            title = null,
                            headline = null,
                            showModeToggle = false,
                            colors = DatePickerDefaults.colors(
                                selectedDayContainerColor = WonderOrange,
                                selectedDayContentColor = Color.White,
                                todayContentColor = WonderOrange,
                                todayDateBorderColor = WonderOrange,
                                dayContentColor = Color.Black,
                                containerColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleQuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .size(56.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ScheduleMutasiItem(item: id.biz.yuka.libperformx.response.ScheduleDetailResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {


            // ✅ 2. TAMPILKAN NAMA TOKO / CABANG (Khusus Owner View)
            if (!item.store_name.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Store, null, tint = WonderGrey, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.store_name!!,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = WonderGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 3. Nama Shift & Jam Operasional
            // ✅ 1. TAMPILKAN NAMA KARYAWAN (Khusus Owner View)
            if (!item.employee_name.isNullOrBlank()) {
                Text(
                    text = item.employee_name!!,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (!item.employee_name.isNullOrBlank()) FontWeight.Medium else FontWeight.Bold
                    ),
                    color = Color.Black
                )
            }
            Text(
                text = "${item.start_time?.take(5) ?: "--:--"} - ${item.end_time?.take(5) ?: "--:--"}",
                style = MaterialTheme.typography.labelSmall,
                color = WonderGrey
            )
        }
        Surface(
            color = WonderLime.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = item.shift_name ?: "Shift Tidak Ditemukan",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black)
            )
        }
    }
}