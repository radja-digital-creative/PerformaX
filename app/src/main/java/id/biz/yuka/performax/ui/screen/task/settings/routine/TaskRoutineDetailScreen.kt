package id.biz.yuka.performax.ui.screen.task.settings.routine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import id.biz.yuka.performax.ui.components.GlobalErrorPopup
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskRoutineDetailScreen(
    onBack: () -> Unit,
    viewModel: TaskRoutineDetailViewModel = hiltViewModel()
) {
    val d = viewModel.detail
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Detail Jadwal Rutin", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        if (viewModel.isLoading && d == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderOrange)
            }
        } else if (d != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Status
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Status Jadwal", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                                    Text(
                                        if (d.isActive) "AKTIF BERJALAN" else "NONAKTIF",
                                        fontWeight = FontWeight.Black,
                                        color = if (d.isActive) Color(0xFF4CAF50) else Color.Red
                                    )
                                }
                                Switch(d.isActive, { viewModel.toggleRoutine(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = WonderOrange))
                            }
                            
                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFF5F5F5))
                            
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Jalankan Selamanya", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
                                    Text(
                                        if (d.isInfinite) "AKTIF" else "NONAKTIF",
                                        fontWeight = FontWeight.Black,
                                        color = if (d.isInfinite) WonderOrange else Color.Gray
                                    )
                                }
                                Switch(
                                    checked = d.isInfinite,
                                    onCheckedChange = { viewModel.toggleInfinite(it) },
                                    enabled = !viewModel.isLoading,
                                    colors = SwitchDefaults.colors(checkedTrackColor = WonderOrange)
                                )
                            }
                        }
                    }
                }

                // 2. Template
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Template Tugas", fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.openEditTemplate() }) {
                                    Icon(Icons.Default.Edit, "Edit template", tint = WonderOrange)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(d.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            if (d.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(d.description, style = MaterialTheme.typography.bodySmall, color = WonderGrey)
                            }
                            Spacer(Modifier.height(10.dp))
                            val modeLabel = if (d.isTaskTeam) "Mode Tim (Bersama)" else "Mode Individu (Mandiri)"
                            Surface(color = WonderOrange.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                                Text(modeLabel,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall, color = WonderOrange,
                                    fontWeight = FontWeight.Black)
                            }
                            if (d.labels.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(d.labels) { lbl ->
                                        Surface(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(50)) {
                                            Text(lbl.name,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Schedule
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Pola Perulangan", fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.openEditSchedule() }) {
                                    Icon(Icons.Default.Edit, "Edit jadwal", tint = WonderOrange)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            val freqLabel = when (d.frequency) {
                                "MONTHLY" -> "Bulanan"
                                "WEEKLY"  -> "Mingguan"
                                else      -> "Harian"
                            }
                            val unitLabel = when (d.frequency) {
                                "MONTHLY" -> "bulan"
                                "WEEKLY"  -> "minggu"
                                else      -> "hari"
                            }
                            
                            RecurrenceDetailItem(Icons.Default.Autorenew, "Frekuensi", freqLabel)

                            val intervalValue = "selama ${d.interval} $unitLabel"
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(WonderBg), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Update, null, tint = WonderOrange, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Interval Routine", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    Text(
                                        text = intervalValue,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (d.isInfinite) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                        ),
                                        color = if (d.isInfinite) Color.Gray else Color.Black
                                    )
                                    if (d.isInfinite) {
                                        Text(
                                            text = "per$unitLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WonderOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            
                            val dayLabel = when (d.frequency) {
                                "WEEKLY" -> "Hari: " + d.daysOfWeek.joinToString(", ") { day ->
                                    when (day) { 1->"Senin";2->"Selasa";3->"Rabu";4->"Kamis";5->"Jumat";6->"Sabtu";7->"Minggu";else->"" }
                                }
                                "MONTHLY" -> "Tanggal: ${d.daysOfMonth.joinToString(", ")}"
                                else      -> "Setiap hari"
                            }
                            RecurrenceDetailItem(Icons.Default.Event, "Pola Jadwal", dayLabel)
                            RecurrenceDetailItem(Icons.Default.DateRange, "Due Day Offset",
                                "${d.deadlineOffset} hari setelah terbit")
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("Terakhir Dibuat", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    Text(d.lastGeneratedDate?.take(10) ?: "Belum pernah", fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Jadwal Berikutnya", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    Text(d.nextRunDate.take(10), fontWeight = FontWeight.Bold, color = WonderOrange)
                                }
                            }
                        }
                    }
                }

                // 4. Assignees
                if (d.assignees.isNotEmpty()) {
                    item {
                        Text("Ditugaskan Kepada:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                    items(d.assignees) { a ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color.White).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(WonderBg)) {
                                if (!a.profileUrl.isNullOrEmpty()) {
                                    AsyncImage(a.profileUrl, null, Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, null,
                                        modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(a.userName, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(a.storeName ?: "-", style = MaterialTheme.typography.labelSmall,
                                    color = WonderGrey)
                            }
                        }
                    }
                }

                // 5. Images
                if (d.images.isNotEmpty()) {
                    item {
                        Text("Lampiran Template:", fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(d.images) { img ->
                                AsyncImage(img.imageUrl, null,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
        } else if (viewModel.errorMessage != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(viewModel.errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    // Bottom Sheet: Edit Schedule
    if (viewModel.showEditScheduleSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.showEditScheduleSheet = false }) {
            Column(
                Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Edit Jadwal Rutin", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)

                Column {
                    Text("Frekuensi", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DAILY" to "Harian", "WEEKLY" to "Mingguan", "MONTHLY" to "Bulanan").forEach { (v, l) ->
                            FilterChip(viewModel.editFrequency == v,
                                onClick = { viewModel.editFrequency = v },
                                label = { Text(l) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WonderOrange.copy(0.2f),
                                    selectedLabelColor = WonderOrange))
                        }
                    }
                }

                AnimatedVisibility(viewModel.editFrequency == "WEEKLY") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hari Pengerjaan", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val labels = listOf("", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                            (1..7).forEach { day ->
                                val isSel = viewModel.editDaysOfWeek.contains(day)
                                Box(Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (isSel) WonderOrange else Color(0xFFF0F0F0))
                                    .clickable { viewModel.toggleEditDayOfWeek(day) },
                                    contentAlignment = Alignment.Center) {
                                    Text(labels[day], color = if (isSel) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(viewModel.editFrequency == "MONTHLY") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tanggal Pengerjaan", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                        (1..31).chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                week.forEach { date ->
                                    val isSel = viewModel.editDaysOfMonth.contains(date)
                                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) WonderOrange else Color(0xFFF0F0F0))
                                        .border(1.dp, if (isSel) WonderOrange else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.toggleEditDayOfMonth(date) }
                                        .weight(1f),
                                        contentAlignment = Alignment.Center) {
                                        Text(date.toString(), color = if (isSel) Color.White else Color.Black, fontSize = 11.sp)
                                    }
                                }
                                if (week.size < 7) repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Interval:", modifier = Modifier.weight(1f))
                    IconButton({ if (viewModel.editInterval > 1) viewModel.editInterval-- }) { Icon(Icons.Default.Remove, null) }
                    Text("${viewModel.editInterval}", fontWeight = FontWeight.Bold)
                    IconButton({ viewModel.editInterval++ }) { Icon(Icons.Default.Add, null) }
                }

                Column {
                    Text("Due Day (hari setelah terbit)", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ if (viewModel.editDeadlineOffset > 0) viewModel.editDeadlineOffset-- }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Text("${viewModel.editDeadlineOffset} hari", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        IconButton({ viewModel.editDeadlineOffset++ }) { Icon(Icons.Default.Add, null) }
                    }
                }

                Button(onClick = { viewModel.saveSchedule() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isSavingSchedule) {
                    if (viewModel.isSavingSchedule)
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Simpan Perubahan Jadwal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Bottom Sheet: Edit Template
    if (viewModel.showEditTemplateSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.showEditTemplateSheet = false }) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp).also { PaddingValues(bottom = 32.dp) },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Text("Edit Template Tugas", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }

                item {
                    OutlinedTextField(viewModel.editTitle, { viewModel.editTitle = it },
                        label = { Text("Judul Tugas") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true)
                }

                item {
                    OutlinedTextField(viewModel.editDescription, { viewModel.editDescription = it },
                        label = { Text("Deskripsi (Opsional)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp))
                }

                item {
                    Text("Mode Penugasan", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(viewModel.editIsTaskTeam, { viewModel.editIsTaskTeam = true }, { Text("Tim (Bersama)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WonderOrange.copy(0.2f), selectedLabelColor = WonderOrange))
                        FilterChip(!viewModel.editIsTaskTeam, { viewModel.editIsTaskTeam = false }, { Text("Individu (Mandiri)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WonderOrange.copy(0.2f), selectedLabelColor = WonderOrange))
                    }
                }

                item {
                    Text("Label", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(viewModel.availableLabels) { lbl ->
                            val isSel = viewModel.editSelectedLabels.contains(lbl)
                            FilterChip(isSel, { viewModel.toggleEditLabel(lbl) }, { Text(lbl.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WonderOrange.copy(0.2f), selectedLabelColor = WonderOrange))
                        }
                    }
                }

                item {
                    Text("Penerima Tugas", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                    if (viewModel.isEmployeeLoading) {
                        CircularProgressIndicator(color = WonderOrange, modifier = Modifier.size(24.dp))
                    }
                }

                items(viewModel.availableEmployees) { emp ->
                    val isSel = viewModel.editSelectedAssignees.any { it.id == emp.id }
                    Row(Modifier.fillMaxWidth().clickable { viewModel.toggleEditAssignee(emp) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(isSel, { viewModel.toggleEditAssignee(emp) },
                            colors = CheckboxDefaults.colors(checkedColor = WonderOrange))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(emp.fullName, fontWeight = FontWeight.Bold)
                            Text(emp.roleName, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }

                item {
                    Button(onClick = { viewModel.saveTemplate() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !viewModel.isSavingTemplate) {
                        if (viewModel.isSavingTemplate)
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text("Simpan Template", fontWeight = FontWeight.Bold)
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
fun RecurrenceDetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(WonderBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = WonderOrange, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
