package id.biz.yuka.performax.ui.screen.employee.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
internal fun EmployeeAttendanceSettings(viewModel: EmployeeDetailViewModel) {
    val prefs = viewModel.attendancePreferences ?: return
    var officeMenu by remember { mutableStateOf(false) }
    var shiftMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Aturan Absensi Karyawan", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(prefs.require_photo, { viewModel.changeAttendancePreferences(prefs.copy(require_photo = it)) })
                Text(if (prefs.require_photo) "Wajib foto" else "Tanpa foto — cukup klik absen", Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(prefs.allow_any_time, { viewModel.changeAttendancePreferences(prefs.copy(allow_any_time = it, max_check_in_time = if (it) null else prefs.max_check_in_time)) })
                Text("Bisa absen jam berapa saja", Modifier.weight(1f))
            }
            Text("Keterlambatan tetap dihitung sesuai jam shift dan aturan potongan gaji.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = prefs.max_check_in_time.orEmpty(),
                onValueChange = { viewModel.changeAttendancePreferences(prefs.copy(max_check_in_time = it)) },
                enabled = !prefs.allow_any_time,
                label = { Text("Maksimal absen masuk (HH:mm)") },
                supportingText = { Text(if (prefs.allow_any_time) "Tidak dibatasi" else "Kosong: mengikuti batas waktu cabang") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Box {
                OutlinedButton(onClick = { officeMenu = true }) {
                    val office = viewModel.employee?.stores?.find { it.id == prefs.default_store_id }
                    Text("Kantor default: ${office?.name ?: "Ikuti kantor utama"}")
                }
                DropdownMenu(officeMenu, { officeMenu = false }) {
                    viewModel.employee?.stores?.forEach { store ->
                        DropdownMenuItem(text = { Text(store.name) }, onClick = {
                            officeMenu = false
                            viewModel.changeAttendancePreferences(prefs.copy(default_store_id = store.id, default_shift_id = null))
                        })
                    }
                }
            }
            if (prefs.default_store_id != null) {
                Box {
                    OutlinedButton(onClick = { shiftMenu = true }) {
                        Text("Shift: ${viewModel.availableShifts.find { it.id == prefs.default_shift_id }?.shiftName ?: "Pilih shift"}")
                    }
                    DropdownMenu(shiftMenu, { shiftMenu = false }) {
                        viewModel.availableShifts.filter { it.storeId == prefs.default_store_id }.forEach { shift ->
                            DropdownMenuItem(text = { Text("${shift.shiftName} (${shift.startTime.take(5)}–${shift.endTime.take(5)})") }, onClick = {
                                shiftMenu = false
                                viewModel.changeAttendancePreferences(prefs.copy(default_shift_id = shift.id))
                            })
                        }
                    }
                }
            }
            Text("Hari masuk default")
            listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu").forEachIndexed { index, label ->
                val day = index + 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(day in prefs.work_days, { selected ->
                        val days = if (selected) prefs.work_days + day else prefs.work_days - day
                        viewModel.changeAttendancePreferences(prefs.copy(work_days = days.distinct().sorted()))
                    })
                    Text(label)
                }
            }
            Text("Perubahan hari/kantor berlaku untuk jadwal otomatis mulai besok. Jadwal manual dan riwayat absensi tetap dipertahankan.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = viewModel::saveAttendancePreferences, enabled = !viewModel.isSavingAttendance) {
                Text(if (viewModel.isSavingAttendance) "Menyimpan…" else "Simpan Aturan Absensi")
            }
        }
    }
}
