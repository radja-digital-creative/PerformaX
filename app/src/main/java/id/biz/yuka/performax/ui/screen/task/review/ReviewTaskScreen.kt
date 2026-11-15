package id.biz.yuka.performax.ui.screen.task.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.performax.ui.components.GlobalErrorPopup
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewTaskScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: ReviewTaskViewModel = hiltViewModel()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadReviewTasks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.successMessage = null
        }
    }

    Scaffold(
        containerColor = WonderBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (viewModel.isMultiSelectMode) {
                        Text("${viewModel.selectedAssignmentIds.size} Tugas Dipilih", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Tinjau Tugas", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (viewModel.isMultiSelectMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal Pilih")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    if (viewModel.selectedTabIndex == 0 && viewModel.tasks.isNotEmpty()) {
                        TextButton(onClick = { viewModel.selectAllCurrentTasks() }) {
                            Text(
                                text = if (viewModel.isMultiSelectMode) "Pilih Semua" else "Pilih",
                                fontWeight = FontWeight.Bold,
                                color = WonderOrange
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            if (viewModel.isMultiSelectMode && viewModel.selectedTabIndex == 0) {
                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.verifySelectedTasks() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !viewModel.isBatchVerifying
                        ) {
                            if (viewModel.isBatchVerifying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verifikasi (${viewModel.selectedAssignmentIds.size}) Tugas",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Search Bar
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cari judul tugas...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = WonderGrey) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WonderOrange,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // Employee Filter (Slide)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = viewModel.selectedEmployeeId == null,
                        onClick = { viewModel.updateEmployeeFilter(null) },
                        label = { Text("Semua") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WonderOrange,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = null
                    )
                }
                items(viewModel.availableEmployees) { employee ->
                    FilterChip(
                        selected = viewModel.selectedEmployeeId == employee.id,
                        onClick = { viewModel.updateEmployeeFilter(employee.id) },
                        label = { Text(employee.fullName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WonderOrange,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = null
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = viewModel.selectedTabIndex,
                containerColor = WonderBg,
                contentColor = WonderOrange,
                indicator = { tabPositions ->
                    if (viewModel.selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedTabIndex]),
                            color = WonderOrange
                        )
                    }
                },
                divider = {}
            ) {
                Tab(
                    selected = viewModel.selectedTabIndex == 0,
                    onClick = { viewModel.updateTabIndex(0) },
                    text = { Text("Review", fontWeight = if(viewModel.selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = viewModel.selectedTabIndex == 1,
                    onClick = { viewModel.updateTabIndex(1) },
                    text = { Text("History", fontWeight = if(viewModel.selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadReviewTasks() },
                state = pullToRefreshState,
                modifier = Modifier.weight(1f)
            ) {
                if (!viewModel.isLoading && viewModel.tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Semua beres!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tidak ada tugas ditemukan.", color = WonderGrey, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(viewModel.tasks, key = { it.id }) { task ->
                            val completedAssignee = task.assignees.find { it.status == "completed" }
                            val targetAssignmentId = completedAssignee?.assignmentId ?: task.assignees.firstOrNull()?.assignmentId
                            val isSelected = targetAssignmentId != null && viewModel.selectedAssignmentIds.contains(targetAssignmentId)

                            ReviewTaskItem(
                                task = task,
                                isMultiSelectMode = viewModel.isMultiSelectMode,
                                isSelected = isSelected,
                                canSelect = viewModel.selectedTabIndex == 0 && targetAssignmentId != null,
                                onToggleSelect = {
                                    if (targetAssignmentId != null) {
                                        viewModel.toggleAssignmentSelection(targetAssignmentId)
                                    }
                                },
                                onClick = {
                                    if (viewModel.isMultiSelectMode && targetAssignmentId != null) {
                                        viewModel.toggleAssignmentSelection(targetAssignmentId)
                                    } else {
                                        onNavigateToDetail(task.id)
                                    }
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0xFFF0F0F0)
                            )
                        }
                    }
                }
            }
        }
    }

    GlobalErrorPopup(
        errorMessage = viewModel.errorMessage,
        onDismiss = { viewModel.errorMessage = null }
    )
}

@Composable
fun ReviewTaskItem(
    task: TaskEntity,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    canSelect: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canSelect && (isMultiSelectMode || isSelected)) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect?.invoke() },
                colors = CheckboxDefaults.colors(checkedColor = WonderOrange),
                modifier = Modifier.padding(end = 8.dp)
            )
        } else {
            // Icon leading
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = WonderOrange, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                // Status Badge
                val firstAssignee = task.assignees.firstOrNull()
                if (firstAssignee != null) {
                    val statusColor = when (firstAssignee.status.lowercase()) {
                        "verified" -> Color(0xFF2E7D32)
                        "completed" -> WonderOrange
                        else -> Color.Gray
                    }
                    val statusText = when (firstAssignee.status.lowercase()) {
                        "verified" -> "VERIFIED"
                        "completed" -> "PERLU CEK"
                        else -> firstAssignee.status.uppercase()
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

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
                    // Profile Circles: creator -> assignees -> checked_by (if verified)
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
                        
                        // Checked By (Verifier)
                        val verifier = task.assignees.firstOrNull()?.checkedBy
                        if (verifier != null) {
                            val assigneeCount = task.assignees.size
                            val offsetWidth = if (assigneeCount > 1) (minOf(assigneeCount, 4) * -6).dp else 0.dp
                            
                            Spacer(modifier = Modifier.width(offsetWidth))
                            
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            
                            AsyncImage(
                                model = verifier.profileUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5F5F5)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    val verifier = task.assignees.firstOrNull()?.checkedBy
                    val assigneeCount = task.assignees.size
                    val baseOffset = if (assigneeCount > 1) (minOf(assigneeCount, 4) * -6).dp else 0.dp
                    val finalOffset = if (verifier != null && assigneeCount <= 1) 4.dp else 8.dp + baseOffset

                    Spacer(modifier = Modifier.width(finalOffset))

                    // Deadline/Time
                    val dateText = try {
                        val date = LocalDate.parse(task.dueDate.take(10))
                        date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))
                    } catch (e: Exception) {
                        task.dueDate.take(10)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = WonderGrey, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = WonderGrey,
                            fontSize = 9.sp
                        )
                    }
                }

                // Points
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
