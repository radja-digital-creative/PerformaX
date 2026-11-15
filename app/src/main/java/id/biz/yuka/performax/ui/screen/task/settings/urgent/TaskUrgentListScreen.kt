package id.biz.yuka.performax.ui.screen.task.settings.urgent

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.libperformx.entity.TaskUrgentEntity
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskUrgentListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: TaskUrgentListViewModel = hiltViewModel()
) {
    val reminders by viewModel.filteredReminders.collectAsState()

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Daftar Pengingat Urgent", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            // Search Bar
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
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

            // Employee Filter (Slide/LazyRow)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = viewModel.selectedEmployeeId == null,
                        onClick = { viewModel.selectedEmployeeId = null },
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
                items(viewModel.employees) { employee ->
                    FilterChip(
                        selected = viewModel.selectedEmployeeId == employee.id,
                        onClick = { viewModel.selectedEmployeeId = employee.id },
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
                    onClick = { viewModel.selectedTabIndex = 0 },
                    text = { Text("Aktif", fontWeight = if(viewModel.selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = viewModel.selectedTabIndex == 1,
                    onClick = { viewModel.selectedTabIndex = 1 },
                    text = { Text("Nonaktif", fontWeight = if(viewModel.selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (viewModel.errorMessage != null) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = viewModel.errorMessage!!,
                        color = Color.Red,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (viewModel.isLoading && reminders.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WonderOrange)
                }
            } else if (reminders.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, tint = WonderGrey, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada pengingat ditemukan.", color = WonderGrey)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(reminders) { reminder ->
                        UrgentItemCard(
                            reminder = reminder,
                            onClick = { onNavigateToDetail(reminder.id) }
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

@Composable
fun UrgentItemCard(
    reminder: TaskUrgentEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon leading
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NotificationImportant,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.taskTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                // Status Badge (Aktif/Nonaktif)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (reminder.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (reminder.isActive) "Aktif" else "Nonaktif",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            Text(
                text = "Pengingat Urgent",
                style = MaterialTheme.typography.labelSmall,
                color = WonderGrey,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Circles: creator -> assignees
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = reminder.creatorProfileUrl,
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
                            reminder.assigneeProfiles.take(maxDisplay).forEachIndexed { index, url ->
                                AsyncImage(
                                    model = url,
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
                            if (reminder.assigneeProfiles.size > maxDisplay) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(x = (maxDisplay * -6).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "+${reminder.assigneeProfiles.size - maxDisplay}",
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WonderGrey
                                    )
                                }
                            }
                        }
                    }

                    val assigneeCount = reminder.assigneeProfiles.size
                    val offsetWidth = if (assigneeCount > 1) (minOf(assigneeCount, 4) * -6).dp else 0.dp

                    Spacer(modifier = Modifier.width(12.dp + offsetWidth))

                    // INTERVAL
                    val intervalText = if (reminder.reminderType == "HOURLY") "Setiap ${reminder.intervalValue} Jam" else "${reminder.intervalValue}x Sehari"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassBottom, null, tint = Color.Red, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = intervalText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Red,
                            fontSize = 9.sp
                        )
                    }
                }

                // SENT COUNT
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, null, tint = Color(0xFF00C4B4), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${reminder.remindersSentToday}x Terkirim",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00C4B4),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}