package id.biz.yuka.performax.ui.screen.task.settings.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.libperformx.entity.TaskRoutineEntity
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRoutineListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: TaskRoutineListViewModel = hiltViewModel()
) {
    val routines by viewModel.filteredRoutines.collectAsState()

    // ✅ Reload data setiap kali masuk ke screen ini (termasuk saat kembali dari detail)
    LaunchedEffect(Unit) {
        viewModel.loadRoutines()
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Jadwal Tugas Rutin", fontWeight = FontWeight.Bold) },
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

            if (viewModel.isLoading && routines.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WonderOrange)
                }
            } else if (routines.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, tint = WonderGrey, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada tugas rutin ditemukan.", color = WonderGrey)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(routines) { routine ->
                        RoutineItemCard(
                            routine = routine,
                            onClick = { onNavigateToDetail(routine.id) }
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
fun RoutineItemCard(
    routine: TaskRoutineEntity,
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
            Icon(Icons.Default.Autorenew, contentDescription = null, tint = WonderOrange, modifier = Modifier.size(24.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = routine.taskTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                
                // Status Badge (Aktif/Nonaktif)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if(routine.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if(routine.isActive) "Aktif" else "Nonaktif",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(routine.isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            val intervalLabel = when(routine.frequency) {
                "MONTHLY" -> "Bulan"
                "WEEKLY" -> "Minggu"
                else -> "Hari"
            }
            Text(
                text = "Frekuensi: ${routine.frequency}",
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
                    // Profile Circles: creator -> assignees
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = routine.creatorProfileUrl,
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
                            routine.assigneeProfiles.take(maxDisplay).forEachIndexed { index, url ->
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
                            if (routine.assigneeProfiles.size > maxDisplay) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(x = (maxDisplay * -6).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "+${routine.assigneeProfiles.size - maxDisplay}",
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WonderGrey
                                    )
                                }
                            }
                        }
                    }
                    
                    val assigneeCount = routine.assigneeProfiles.size
                    val offsetWidth = if (assigneeCount > 1) (minOf(assigneeCount, 4) * -6).dp else 0.dp
                    
                    Spacer(modifier = Modifier.width(12.dp + offsetWidth))
                    
                    // INTERVAL (menggantikan status)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = WonderOrange, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Setiap ${routine.interval} $intervalLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = WonderOrange,
                            fontSize = 9.sp
                        )
                    }
                }

                // NEXT RUN (menggantikan poin)
                val formattedNextRun = try {
                    val date = LocalDate.parse(routine.nextRunDate.take(10))
                    date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("id")))
                } catch (_: Exception) {
                    routine.nextRunDate.take(10)
                }
                
                Text(
                    text = formattedNextRun,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF00C4B4),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = WonderGrey, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = WonderGrey, fontSize = 9.sp)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}