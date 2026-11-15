package id.biz.yuka.performax.ui.screen.task

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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import id.biz.yuka.libperformx.entity.TaskEntity
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
fun TaskScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit, // ✅ TAMBAHAN CALLBACK
    viewModel: TaskViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val dateListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var stickyHeaderHeight by remember { mutableStateOf(0.dp) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState()

    val groupedTasks = viewModel.getGroupedTasks()
    val dates = groupedTasks.keys.toList()

    // ✅ FITUR AUTO-RELOAD SAAT KEMBALI KE HALAMAN INI
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadTasks() // Segarkan data diam-diam
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val dateToScrollIndex = remember(groupedTasks) {
        val mapping = mutableMapOf<LocalDate, Int>()
        var currentIndex = 1
        dates.forEach { date ->
            mapping[date] = currentIndex
            currentIndex += 1 + (groupedTasks[date]?.size ?: 0)
        }
        mapping
    }

    LaunchedEffect(groupedTasks, stickyHeaderHeight) {
        if (groupedTasks.isNotEmpty() && stickyHeaderHeight > 0.dp) {
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
                title = { 
                    if (isSearchMode) {
                        TextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.searchQuery = it },
                            placeholder = { Text("Cari tugas...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { 
                                    viewModel.searchQuery = ""
                                    isSearchMode = false 
                                }) { Icon(Icons.Default.Close, null) }
                            }
                        )
                    } else {
                        Text("Info tugas", fontWeight = FontWeight.Bold) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (!isSearchMode) {
                        IconButton(onClick = { isSearchMode = true }) { Icon(Icons.Default.Search, "Cari") }
                    }
                    if (viewModel.isManagerOrOwner) {
                        IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadTasks() },
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TaskQuickAction(
                                        label = "Assign",
                                        icon = Icons.Default.Assignment,
                                        color = Color(0xFFE0F2F1),
                                        onClick = onNavigateToCreate
                                    )
                                    TaskQuickAction(
                                        label = "Review",
                                        icon = Icons.Default.FilterList,
                                        color = Color(0xFFFFF3E0),
                                        onClick = onNavigateToReview
                                    )
                                    TaskQuickAction(
                                        label = "History",
                                        icon = Icons.Default.Assignment,
                                        color = Color(0xFFE1F5FE),
                                        onClick = onNavigateToHistory
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Semua tugas",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    Text(
                                        text = "Lihat Laporan",
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
                                
                                if (viewModel.isOwner && viewModel.employees.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Filter Karyawan",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = viewModel.selectedEmployeeId == null,
                                                onClick = { viewModel.updateEmployeeFilter(null) },
                                                label = { Text("Semua") },
                                                leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = WonderOrange,
                                                    selectedLabelColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                        items(viewModel.employees) { employee ->
                                            FilterChip(
                                                selected = viewModel.selectedEmployeeId == employee.id,
                                                onClick = { viewModel.updateEmployeeFilter(employee.id) },
                                                label = { Text(employee.fullName) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = WonderOrange,
                                                    selectedLabelColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    if (groupedTasks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.5f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada tugas",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WonderGrey
                                )
                            }
                        }
                    } else {
                        groupedTasks.forEach { (date, tasks) ->
                            item(key = "header_$date") {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    val localeId = Locale("id", "ID")
                                    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", localeId)
                                    Text(
                                        text = date.format(formatter),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                            items(tasks, key = { it.id }) { task ->
                                TaskMutasiItem(
                                    task = task,
                                    onClick = { onNavigateToDetail(task.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (showDatePicker) {
                ModalBottomSheet(
                    onDismissRequest = { showDatePicker = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
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
                                    "Pilih Rentang Tanggal",
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
fun TaskQuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
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
fun TaskMutasiItem(
    task: TaskEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
            Icon(Icons.Default.Assignment, contentDescription = null, tint = Color.Gray)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = task.description,
                style = MaterialTheme.typography.labelSmall,
                color = WonderGrey,
                maxLines = 1
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Circles: assigned_by -> assigned_to
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = task.creatorProfileUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5F5F5)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val maxDisplay = 3
                            task.assignees.take(maxDisplay).forEachIndexed { index, assignee ->
                                AsyncImage(
                                    model = assignee.profileUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(x = (index * -6).dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(if (index > 0) 1.dp else 0.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (task.assignees.size > maxDisplay) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(x = (maxDisplay * -6).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "+${task.assignees.size - maxDisplay}",
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WonderGrey
                                    )
                                }
                            }
                        }
                    }
                    
                    val assigneeCount = task.assignees.size
                    val offsetWidth = if (assigneeCount > 1) (minOf(assigneeCount, 4) * -6).dp else 0.dp
                    
                    Spacer(modifier = Modifier.width(12.dp + offsetWidth))
                    
                    // STATUS TUGAS
                    val firstAssignee = task.assignees.firstOrNull()
                    if (firstAssignee != null) {
                        val statusColor = when (firstAssignee.status.lowercase()) {
                            "completed" -> Color(0xFF4CAF50)
                            "in_progress" -> WonderOrange
                            "pending" -> Color.Gray
                            else -> Color.Gray
                        }
                        val statusIcon = when (firstAssignee.status.lowercase()) {
                            "completed" -> Icons.Default.CheckCircle
                            "in_progress" -> Icons.Default.Schedule
                            else -> Icons.Default.Info
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = firstAssignee.status.replace("_", " ").uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // POIN
                val points = task.assignees.firstOrNull()?.earnedPoints ?: 0
                Text(
                    text = "+${points} Poin",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF00C4B4),
                    fontSize = 9.sp
                )
            }
        }
    }
}