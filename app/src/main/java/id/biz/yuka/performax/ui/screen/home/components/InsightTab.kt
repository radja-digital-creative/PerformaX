package id.biz.yuka.performax.ui.screen.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biz.yuka.libperformx.entity.BonusItemEntity
import id.biz.yuka.libperformx.entity.DeductionItemEntity
import id.biz.yuka.libperformx.entity.PenaltyItemEntity
import id.biz.yuka.libperformx.entity.SalaryDetailEntity
import id.biz.yuka.performax.ui.screen.home.InsightViewModel
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import java.text.NumberFormat
import java.time.LocalDate
import java.util.*

fun LazyListScope.insightTab(viewModel: InsightViewModel, onNavigateToMenu: (String) -> Unit) {
    if (viewModel.isInitialRoleChecking) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderOrange, modifier = Modifier.size(32.dp))
            }
        }
        return
    }

    val salary = viewModel.salaryDetail
    val isOwner = viewModel.isOwner

    item {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val title = if (isOwner) "Total pengeluaran gaji" else "Rekap penghasilanmu"
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = WonderGrey)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Selengkapnya",
                    style = MaterialTheme.typography.labelLarge.copy(color = WonderOrange),
                    modifier = Modifier.clickable {
                        onNavigateToMenu(id.biz.yuka.performax.ui.navigation.Screen.IncomeRecap.route)
                    }
                )
            }
            Text(text = viewModel.periodString.ifEmpty { "Periode Berjalan" }, style = MaterialTheme.typography.bodySmall, color = WonderGrey)
        }
    }

    item {
        if (isOwner) {
            OwnerSalaryInsightCard(
                income = viewModel.totalOwnerIncome,
                deduction = viewModel.totalOwnerDeduction,
                net = viewModel.totalOwnerTakeHomePay
            )
        } else {
            val monthlyAttendanceCount = viewModel.monthlyAttendanceCount
            val monthlyScheduleCount = viewModel.monthlyScheduleCount
            val passedScheduleCount = viewModel.passedScheduleCount
            SalaryInsightCard(salary, monthlyAttendanceCount, monthlyScheduleCount, passedScheduleCount)
        }
    }

    if (!isOwner && salary != null) {
        // ✅ Tampilkan Daftar Bonus (Jika Ada) - Hanya untuk karyawan
        if (salary.bonuses.isNotEmpty()) {
            item { Text(text = "Rincian Bonus & Insentif", modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) }
            items(salary.bonuses) { bonus -> BonusListItem(bonus) }
        }

        // ✅ Tampilkan Daftar Potongan Master (Jika Ada) - Hanya untuk karyawan
        if (salary.deductions.isNotEmpty() || salary.penalties.isNotEmpty()) {
            item { Text(text = "Rincian Potongan & Penalti", modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) }
            items(salary.deductions) { deduction -> DeductionListItem(deduction, salary.basicSalary) }
            items(salary.penalties) { penalty -> PenaltyListItem(penalty) }
        }
    }
}

@Composable
fun OwnerSalaryInsightCard(
    income: Double,
    deduction: Double,
    net: Double
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Pengeluaran Gaji") }

    ElevatedCard(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clickable { expanded = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    color = Color.White
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = selectedMode, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
                    DropdownMenuItem(text = { Text("Pengeluaran Gaji") }, onClick = { selectedMode = "Pengeluaran Gaji"; expanded = false })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Total Pengeluaran Bulan Ini (Semua Karyawan)", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00C4B4), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Total Gaji Bruto", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(text = income.formatRupiah(), fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFE57373), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Total Potongan", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(text = deduction.formatRupiah(), fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Total Gaji Bersih Terbayar", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
            Text(text = net.formatRupiah(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))

            Spacer(modifier = Modifier.height(32.dp))

            // Graph
            Row(
                modifier = Modifier.height(100.dp).fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.width(48.dp).fillMaxHeight(0.8f).background(Color(0xFF00C4B4), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                Spacer(modifier = Modifier.width(32.dp))
                val deductionHeight = if (income > 0) (deduction / income).toFloat().coerceIn(0.1f, 0.8f) else 0.1f
                Box(modifier = Modifier.width(48.dp).fillMaxHeight(deductionHeight).background(Color(0xFFE57373), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                Text(text = "Bruto", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(32.dp))
                Text(text = "Potongan", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SalaryInsightCard(
    salary: SalaryDetailEntity?,
    monthlyAttendanceCount: Int,
    monthlyScheduleCount: Int,
    passedScheduleCount: Int
) {
    val isWeekly = salary?.payrollType == id.biz.yuka.libperformx.entity.PayrollType.WEEKLY
    val dailyRate = salary?.payPerDay ?: 0.0
    val totalBonus = salary?.totalBonus ?: 0.0

    // 1. DATA PENDAPATAN
    val displayIncome = if (isWeekly && dailyRate > 0) {
        (monthlyAttendanceCount * dailyRate) + totalBonus
    } else {
        (passedScheduleCount * dailyRate) + totalBonus
    }

    // 2. DATA POTONGAN
    val absentDays = salary?.totalAlfa ?: 0
    val alphaDeduction = if (!isWeekly) absentDays * dailyRate else 0.0
    val totalPenalty = salary?.totalPenalty ?: 0.0
    val baseDeduction = salary?.totalDeduction ?: 0.0
    val displayDeduction = baseDeduction + totalPenalty + alphaDeduction

    // 3. TAKE HOME PAY ESTIMASI
    val net = displayIncome - displayDeduction

    var expanded by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Pendapatan & Potongan") }

    ElevatedCard(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clickable { expanded = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    color = Color.White
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = selectedMode, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
                    DropdownMenuItem(text = { Text("Pendapatan & Potongan") }, onClick = { selectedMode = "Pendapatan & Potongan"; expanded = false })
                    DropdownMenuItem(text = { Text("Hanya Pendapatan") }, onClick = { selectedMode = "Hanya Pendapatan"; expanded = false })
                    DropdownMenuItem(text = { Text("Hanya Potongan") }, onClick = { selectedMode = "Hanya Potongan"; expanded = false })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$monthlyAttendanceCount Hadir | $absentDays Alfa | $monthlyScheduleCount Jadwal",
                style = MaterialTheme.typography.labelSmall,
                color = WonderGrey
            )

            Spacer(modifier = Modifier.height(16.dp))

            val showIncomeText = selectedMode == "Pendapatan & Potongan" || selectedMode == "Hanya Pendapatan"
            val showDeductionText = selectedMode == "Pendapatan & Potongan" || selectedMode == "Hanya Potongan"

            Row(modifier = Modifier.fillMaxWidth()) {
                if (showIncomeText) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF00C4B4), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Pendapatan", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(text = displayIncome.formatRupiah(), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }

                if (showIncomeText && showDeductionText) {
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
                }

                if (showDeductionText) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFE57373), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Potongan", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(text = displayDeduction.formatRupiah(), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Take Home Pay Estimasi", style = MaterialTheme.typography.labelMedium, color = WonderGrey)
            Text(text = (if (net < 0) 0.0 else net).formatRupiah(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))

            Spacer(modifier = Modifier.height(32.dp))

            val rawHeightIncome = if (displayIncome <= 0.0) 0.02f else if (selectedMode == "Hanya Pendapatan") 1.0f else 0.8f
            val rawHeightDeduction = if (displayDeduction <= 0.0) {
                0.02f
            } else if (selectedMode == "Hanya Potongan") {
                1.0f
            } else {
                if (displayIncome > 0) (displayDeduction / displayIncome).toFloat().coerceIn(0.1f, 0.8f) else 0.1f
            }

            var startAnimation by remember { mutableStateOf(false) }
            LaunchedEffect(selectedMode) {
                startAnimation = false
                kotlinx.coroutines.delay(50)
                startAnimation = true
            }

            val animatedHeightIncome by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (startAnimation && showIncomeText) rawHeightIncome else 0.0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "income_anim"
            )

            val animatedHeightDeduction by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (startAnimation && showDeductionText) rawHeightDeduction else 0.0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "deduction_anim"
            )

            Row(
                modifier = Modifier.height(100.dp).fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                if (showIncomeText) {
                    Box(modifier = Modifier.width(48.dp).fillMaxHeight(animatedHeightIncome.coerceAtLeast(0.001f)).background(Color(0xFF00C4B4), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                }

                if (showIncomeText && showDeductionText) {
                    Spacer(modifier = Modifier.width(32.dp))
                }

                if (showDeductionText) {
                    Box(modifier = Modifier.width(48.dp).fillMaxHeight(animatedHeightDeduction.coerceAtLeast(0.001f)).background(Color(0xFFE57373), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                if (showIncomeText) Text(text = "Pendapatan", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                if (showIncomeText && showDeductionText) Spacer(modifier = Modifier.width(32.dp))
                if (showDeductionText) Text(text = "Potongan", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun BonusListItem(bonus: BonusItemEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFE0F2F1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Bonus / Insentif", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = bonus.notes, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
        }
        Text(text = "+ ${bonus.amount.formatRupiah()}", color = Color(0xFF00C4B4), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PenaltyListItem(penalty: PenaltyItemEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFFFEBEE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "-", fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Sanksi Pelanggaran", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = penalty.penaltyType, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
        }
        Text(text = "- ${penalty.penaltyAmount.formatRupiah()}", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DeductionListItem(deduction: DeductionItemEntity, basicSalary: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFF5F5F5), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = deduction.name.take(1), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = deduction.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = if (deduction.type == "percentage") "${deduction.percentageValue}% dari Gaji Pokok" else "Potongan Tetap", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
        }
        val amount = if (deduction.type == "fixed") deduction.fixedAmount else (basicSalary * (deduction.percentageValue / 100))
        Text(text = "- ${amount.formatRupiah()}", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
    }
}

fun Double.formatRupiah(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(this).replace("Rp", "Rp ").substringBefore(",")
}