package id.biz.yuka.performax.ui.screen.task.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskScreen(
    onBack: () -> Unit,
    onSuccessReturn: (id: String, isRoutine: Boolean) -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val datePickerState = rememberDatePickerState()

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addAttachments(context, uris)
    }

    LaunchedEffect(viewModel.successResult) {
        viewModel.successResult?.let { (id, isRoutine) -> onSuccessReturn(id, isRoutine) }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Langkah ${viewModel.currentStep} dari 5", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = { if (viewModel.currentStep > 1) viewModel.prevStep() else onBack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (viewModel.currentStep > 1) {
                        OutlinedButton(onClick = { viewModel.prevStep() }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Kembali") }
                    }
                    Button(
                        onClick = { if (viewModel.currentStep < 5) viewModel.nextStep() else viewModel.submitTask(context) },
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Text(if (viewModel.currentStep < 5) "Lanjut" else "Terbitkan Tugas", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // PROGRESS BAR (Update total steps to 6)
            LinearProgressIndicator(progress = { viewModel.currentStep / 6f }, modifier = Modifier.fillMaxWidth(), color = WonderOrange, trackColor = WonderOrange.copy(0.1f))

            if (viewModel.errorMessage != null) {
                Text(viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = viewModel.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    }, label = "wizard_step"
                ) { step ->
                    when (step) {
                        1 -> Step1BasicInfo(viewModel)
                        2 -> Step2Attachments(viewModel, filePickerLauncher)
                        3 -> Step3Assignees(viewModel)
                        4 -> Step4Configuration(viewModel, datePickerState)
                        5 -> Step5TeamOrIndividual(viewModel)
                        6 -> Step6Summary(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Step1BasicInfo(viewModel: CreateTaskViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Penjelasan Tugas & Label", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text("Tentukan apa yang harus dikerjakan dan kategori urgensinya.", color = WonderGrey, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(value = viewModel.title, onValueChange = { viewModel.title = it }, label = { Text("Judul Tugas") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = viewModel.description, onValueChange = { viewModel.description = it }, label = { Text("Deskripsi (Opsional)") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
                }
            }
        }
        item {
            Text("Pilih Label (Bisa Multi)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // GANTI FlowRow dengan LazyRow agar tidak crash (NoSuchMethodError)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(viewModel.availableLabels) { label ->
                    val isSelected = viewModel.selectedLabels.contains(label)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleLabel(label) },
                        label = { Text(label.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WonderOrange.copy(0.2f),
                            selectedLabelColor = WonderOrange
                        ),
                        shape = RoundedCornerShape(50)
                    )
                }
            }
        }
    }
}

@Composable
fun Step2Attachments(viewModel: CreateTaskViewModel, launcher: androidx.activity.result.ActivityResultLauncher<String>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Isian Tugas (Lampiran)", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text("Tambahkan foto atau dokumen pendukung untuk memperjelas instruksi.", color = WonderGrey, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Button(onClick = { launcher.launch("*/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Tambah Lampiran")
            }
        }
        items(viewModel.attachments) { file ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(WonderBg), contentAlignment = Alignment.Center) {
                    if (file.isImage) AsyncImage(file.uri, null, contentScale = ContentScale.Crop) else Icon(Icons.Default.InsertDriveFile, null)
                }
                Spacer(Modifier.width(12.dp))
                Text(file.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                IconButton(onClick = { viewModel.attachments.remove(file) }) { Icon(Icons.Default.Close, null, tint = Color.Red) }
            }
        }
    }
}

@Composable
fun Step3Assignees(viewModel: CreateTaskViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Ditugaskan Ke Siapa?", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text("Pilih satu atau beberapa karyawan yang harus mengerjakan.", color = WonderGrey, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }
        if (viewModel.isEmployeeLoading) { item { CircularProgressIndicator(color = WonderOrange, modifier = Modifier.padding(32.dp)) } }
        items(viewModel.employees) { emp ->
            val isSelected = viewModel.selectedAssignees.any { it.id == emp.id }
            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleAssignee(emp) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(isSelected, { viewModel.toggleAssignee(emp) }, colors = CheckboxDefaults.colors(checkedColor = WonderOrange))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(emp.fullName, fontWeight = FontWeight.Bold)
                    Text(emp.roleName, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                }
            }
            HorizontalDivider(color = Color(0xFFF0F0F0))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Step4Configuration(viewModel: CreateTaskViewModel, datePickerState: DatePickerState) {
    var showDateModal by remember { mutableStateOf(false) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Pengaturan Label", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text("Kustomisasi alur pengerjaan berdasarkan label yang Anda pilih.", color = WonderGrey, style = MaterialTheme.typography.bodySmall)
        }
        // URGENT CONFIG
        if (viewModel.isUrgentTask) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.NotificationImportant, null, tint = Color.Red); Spacer(Modifier.width(8.dp)); Text("Pengingat Urgent", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("HOURLY" to "Per Jam", "DAILY_X_TIMES" to "X Kali").forEach { (v, l) ->
                                FilterChip(viewModel.urgentReminderType == v, { viewModel.urgentReminderType = v }, { Text(l) })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Interval:", modifier = Modifier.weight(1f))
                            IconButton(onClick = { if(viewModel.urgentInterval > 1) viewModel.urgentInterval-- }) { Icon(Icons.Default.Remove, null) }
                            Text("${viewModel.urgentInterval}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if(viewModel.urgentInterval < 24) viewModel.urgentInterval++ }) { Icon(Icons.Default.Add, null) }
                        }
                    }
                }
            }
        }
        // ROUTINE CONFIG
        if (viewModel.isRoutineTask) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Autorenew, null, tint = Color(0xFFF57F17)); Spacer(Modifier.width(8.dp)); Text("Jadwal Rutin", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DAILY" to "Harian", "WEEKLY" to "Minggu", "MONTHLY" to "Bulan").forEach { (v, l) ->
                                FilterChip(viewModel.routineFrequency == v, { viewModel.routineFrequency = v }, { Text(l) })
                            }
                        }
                        // Render hari/tanggal pilihan (logika ringkas)
                        if (viewModel.routineFrequency == "WEEKLY") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                (1..7).forEach { d ->
                                    val isS = viewModel.routineDaysOfWeek.contains(d)
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if(isS) WonderOrange else Color.White).clickable { viewModel.toggleDayOfWeek(d) }, contentAlignment = Alignment.Center) {
                                        Text(listOf("","S","S","R","K","J","S","M")[d], color = if(isS) Color.White else Color.Black)
                                    }
                                }
                            }
                        }

                        if (viewModel.routineFrequency == "MONTHLY") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..31).chunked(7).forEach { week ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        week.forEach { date ->
                                            val isSelected = viewModel.routineDaysOfMonth.contains(date)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) WonderOrange else Color.White)
                                                    .border(1.dp, if (isSelected) WonderOrange else Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.toggleDayOfMonth(date) }
                                                    .weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = date.toString(), color = if (isSelected) Color.White else Color.Black, fontSize = 12.sp)
                                            }
                                        }
                                        // Fill empty space if the last week is not full
                                        if (week.size < 7) {
                                            repeat(7 - week.size) {
                                                Spacer(modifier = Modifier.size(36.dp).weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // DEADLINE CONFIG
        item {
            Text("Tenggat Waktu", fontWeight = FontWeight.Bold)
            if (viewModel.isRoutineTask) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Due Day (hari setelah terbit)", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (viewModel.routineDeadlineOffset > 0) viewModel.routineDeadlineOffset-- }) { Icon(Icons.Default.Remove, null) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("${viewModel.routineDeadlineOffset}", fontWeight = FontWeight.Black, fontSize = 28.sp)
                                Text("hari", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                            }
                            IconButton(onClick = { viewModel.routineDeadlineOffset++ }) { Icon(Icons.Default.Add, null) }
                        }
                        Spacer(Modifier.height(8.dp))
                        val previewDate = java.time.LocalDate.now().plusDays(viewModel.routineDeadlineOffset.toLong())
                        val previewText = previewDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("id")))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, tint = WonderOrange, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Deadline pertama: $previewText", style = MaterialTheme.typography.bodySmall, color = WonderOrange, fontWeight = FontWeight.Medium)
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(Modifier.height(16.dp))

                        // INTERVAL CONFIG
                        Text("Interval Routine", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (viewModel.routineInterval > 1) viewModel.routineInterval-- }) { Icon(Icons.Default.Remove, null) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("${viewModel.routineInterval}", fontWeight = FontWeight.Black, fontSize = 28.sp)
                                val unitLabel = when(viewModel.routineFrequency) {
                                    "DAILY" -> "Hari"
                                    "WEEKLY" -> "Minggu"
                                    "MONTHLY" -> "Bulan"
                                    else -> "Kali"
                                }
                                Text(unitLabel, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                            }
                            IconButton(onClick = { viewModel.routineInterval++ }) { Icon(Icons.Default.Add, null) }
                        }
                        
                        val intervalUnit = when(viewModel.routineFrequency) {
                            "DAILY" -> "Hari"
                            "WEEKLY" -> "Minggu"
                            "MONTHLY" -> "Bulan"
                            else -> ""
                        }
                        Text(
                            text = "Terbit selama: ${viewModel.routineInterval} $intervalUnit",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        // IS INFINITE CONFIG
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.routineIsInfinite = !viewModel.routineIsInfinite }) {
                            Checkbox(
                                checked = viewModel.routineIsInfinite,
                                onCheckedChange = { viewModel.routineIsInfinite = it },
                                colors = CheckboxDefaults.colors(checkedColor = WonderOrange)
                            )
                            Text("Jalankan Selamanya", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        if (viewModel.routineIsInfinite) {
                            Text(
                                "Dengan mengaktifkan ini maka Pengaturan Interval Routine diatas akan diabaikan dan tugas terjadwal setiap $intervalUnit hingga dinonaktifkan secara manual",
                                style = MaterialTheme.typography.labelSmall,
                                color = WonderGrey,
                                modifier = Modifier.padding(start = 48.dp)
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(value = viewModel.deadline?.toString() ?: "Pilih Tanggal", onValueChange = {}, readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth().clickable { showDateModal = true }, trailingIcon = { Icon(Icons.Default.DateRange, null) }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black))
            }
        }
    }
    if (showDateModal) {
        DatePickerDialog(onDismissRequest = { showDateModal = false }, confirmButton = { TextButton(onClick = { viewModel.deadline = Instant.ofEpochMilli(datePickerState.selectedDateMillis ?: 0).atZone(ZoneId.systemDefault()).toLocalDate(); showDateModal = false }) { Text("Pilih") } }) {
            DatePicker(datePickerState)
        }
    }
}

@Composable
fun Step5TeamOrIndividual(viewModel: CreateTaskViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Mode Penugasan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("Tentukan bagaimana tugas ini akan didistribusikan kepada ${viewModel.selectedAssignees.size} karyawan.", style = MaterialTheme.typography.bodySmall, color = WonderGrey)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            ModeSelectionCard(
                title = "Tugas Tim (Bersama)",
                description = "Hanya 1 tugas yang dibuat. Jika salah satu karyawan menyelesaikan, maka tugas dianggap selesai untuk semuanya.",
                isSelected = viewModel.isTeamTask,
                onClick = { viewModel.isTeamTask = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ModeSelectionCard(
                title = "Tugas Individu (Mandiri)",
                description = "Sistem akan membuatkan 1 tugas khusus untuk masing-masing karyawan. Setiap orang bertanggung jawab atas tugasnya sendiri.",
                isSelected = !viewModel.isTeamTask,
                onClick = { viewModel.isTeamTask = false }
            )
        }
    }
}

@Composable
fun ModeSelectionCard(title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) WonderOrange.copy(0.1f) else Color.White),
        border = BorderStroke(2.dp, if (isSelected) WonderOrange else Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = WonderOrange))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(description, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun Step6Summary(viewModel: CreateTaskViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Ringkasan Tugas", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text("Tinjau kembali data sebelum benar-benar diterbitkan.", color = WonderGrey, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(viewModel.title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(viewModel.description.ifBlank { "Tidak ada deskripsi" }, color = Color.Gray)
                    
                    Spacer(Modifier.height(16.dp))
                    if (viewModel.selectedAssignees.size > 1) {
                        val modeText = if (viewModel.isTeamTask) "Penugasan Tim (Bersama)" else "Penugasan Individu (Mandiri)"
                        Surface(color = WonderOrange.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                            Text(modeText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = WonderOrange, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text("Penerima:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(viewModel.selectedAssignees.joinToString { it.fullName }, color = WonderOrange, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Label:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(viewModel.selectedLabels.joinToString { it.name })

                    if (viewModel.isRoutineTask) {
                        Spacer(Modifier.height(16.dp))
                        Text("Konfigurasi Rutin:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val freqText = when(viewModel.routineFrequency) {
                            "DAILY" -> "Hari"
                            "WEEKLY" -> "Minggu"
                            "MONTHLY" -> "Bulan"
                            else -> ""
                        }
                        Text("Terbit setiap: ${viewModel.routineInterval} $freqText")
                        Text("Tenggat: ${viewModel.routineDeadlineOffset} Hari")
                        if (viewModel.routineIsInfinite) {
                            Text("Mode: Berjalan Selamanya", color = WonderOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(12.dp))
                    Text("Pastikan instruksi sudah jelas agar tidak terjadi kebingungan di lapangan.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
