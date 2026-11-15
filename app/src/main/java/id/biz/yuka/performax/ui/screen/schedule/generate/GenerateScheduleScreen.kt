package id.biz.yuka.performax.ui.screen.schedule.generate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import id.biz.yuka.performax.ui.theme.WonderLime
import id.biz.yuka.performax.ui.theme.WonderOrange
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScheduleScreen(
    onBack: () -> Unit,
    viewModel: GenerateScheduleViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            delay(2000)
            onBack()
        }
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Generate Jadwal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    Text(
                        text = "Keterangan: Generate ini berefek ke jadwal hari besok dan selama bulan yang dipilih. Jadwal bulan lalu tidak berefek (karena sudah absen).",
                        style = MaterialTheme.typography.bodySmall,
                        color = WonderGrey,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = { viewModel.executeGenerate() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Generate Jadwal", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (viewModel.errorMessage != null) {
                item {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            if (viewModel.successMessage != null) {
                item {
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(viewModel.successMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    }
                }
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
                    ExposedDropdownMenu(
                        expanded = expandedEmployee,
                        onDismissRequest = { expandedEmployee = false }
                    ) {
                        if (viewModel.availableEmployees.isEmpty()) {
                            DropdownMenuItem(text = { Text("Memuat data...") }, onClick = {})
                        } else {
                            viewModel.availableEmployees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text(emp.fullName) },
                                    onClick = {
                                        viewModel.selectEmployee(emp)
                                        expandedEmployee = false
                                    }
                                )
                            }
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
                    if (stores.isEmpty()) {
                        Text("Karyawan ini belum memiliki penempatan toko.", color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val selectedIndex = stores.indexOfFirst { it.id == viewModel.selectedStoreId }.takeIf { it >= 0 } ?: 0
                        ScrollableTabRow(
                            selectedTabIndex = selectedIndex,
                            containerColor = Color.Transparent,
                            contentColor = WonderOrange,
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                    color = WonderOrange
                                )
                            }
                        ) {
                            stores.forEachIndexed { index, store ->
                                Tab(
                                    selected = selectedIndex == index,
                                    onClick = { viewModel.selectStore(store.id) },
                                    text = {
                                        Text(
                                            text = store.name,
                                            fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedIndex == index) WonderOrange else WonderGrey
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. DROPDOWN SHIFT
            if (viewModel.selectedStoreId.isNotBlank()) {
                item {
                    Text("Pilih Shift (Toko Terpilih)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    var expandedShift by remember { mutableStateOf(false) }
                    val selectedShift = viewModel.availableShifts.find { it.id == viewModel.selectedShiftId }

                    ExposedDropdownMenuBox(
                        expanded = expandedShift,
                        onExpandedChange = { expandedShift = it }
                    ) {
                        OutlinedTextField(
                            value = selectedShift?.shiftName ?: "Pilih Shift",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedShift) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedShift,
                            onDismissRequest = { expandedShift = false }
                        ) {
                            if (viewModel.availableShifts.isEmpty()) {
                                DropdownMenuItem(text = { Text("Tidak ada shift tersedia") }, onClick = {})
                            } else {
                                viewModel.availableShifts.forEach { shift ->
                                    DropdownMenuItem(
                                        text = { Text("${shift.shiftName} (${shift.startTime.take(5)} - ${shift.endTime.take(5)})") },
                                        onClick = {
                                            viewModel.selectedShiftId = shift.id
                                            expandedShift = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. HARI AKTIF (LINGKARAN INISIAL)
            item {
                Text("Hari Kerja Aktif", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // 1=Senin, 2=Selasa, 3=Rabu, 4=Kamis, 5=Jumat, 6=Sabtu, 7=Minggu
                val days = listOf("S" to 1, "S" to 2, "R" to 3, "K" to 4, "J" to 5, "S" to 6, "M" to 7)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEach { (label, value) ->
                        val isSelected = viewModel.selectedDays.contains(value)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) WonderOrange else Color(0xFFE0E0E0))
                                .clickable { viewModel.toggleDay(value) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // 5. RENTANG BULAN KEDEPAN (KAPSUL)
            item {
                Text("Berapa Bulan Kedepan?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(1, 2, 3).forEach { monthValue ->
                        val isSelected = viewModel.selectedMonthCount == monthValue
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedMonthCount = monthValue },
                            label = { Text("$monthValue Bulan", modifier = Modifier.padding(horizontal = 8.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WonderLime,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = if (isSelected) null else FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }
}