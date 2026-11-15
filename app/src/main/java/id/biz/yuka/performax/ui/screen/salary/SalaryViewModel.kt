package id.biz.yuka.performax.ui.screen.salary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.SalaryDetailEntity
import id.biz.yuka.libperformx.repository.SalaryRepository
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

import id.biz.yuka.libperformx.entity.PayrollType

import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.ui.utils.PdfSaveHelper
import id.biz.yuka.performax.ui.utils.SalaryPdfGenerator
import android.content.Context
import kotlinx.coroutines.async

data class SalaryChartItemData(
    val name: String,
    val amount: Double,
    val color: Color
)

// Data class pembantu untuk manajemen riwayat bulan/minggu
data class SalaryPeriodItem(
    val label: String,
    val periodSubtitle: String,
    val month: Int,
    val year: Int,
    val startDate: String,
    val endDate: String
)

@HiltViewModel
class SalaryViewModel @Inject constructor(
    private val salaryRepository: SalaryRepository,
    private val attendanceRepository: AttendanceRepository,
    private val employeeRepository: EmployeeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navUserId: String = savedStateHandle["userId"] ?: "me"

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    var employeeName by mutableStateOf("")

    // UI Filter & Periode Riwayat
    private var availablePeriods = listOf<SalaryPeriodItem>()
    var availableMonths by mutableStateOf<List<String>>(emptyList())

    var selectedMonth by mutableStateOf<String>("")
    var selectedMonthIndex by mutableIntStateOf(0)
    var periodString by mutableStateOf("")

    // State Filter Tipe Payroll (Monthly / Weekly)
    var activePayrollType by mutableStateOf(PayrollType.MONTHLY)

    // State Hasil Kalkulasi
    var selectedSalary by mutableStateOf<SalaryDetailEntity?>(null)
    var totalPresent by mutableIntStateOf(0)
    var totalAlfa by mutableIntStateOf(0)
    var totalSchedules by mutableIntStateOf(0)

    var totalIncome by mutableDoubleStateOf(0.0)
    var totalDeduction by mutableDoubleStateOf(0.0)
    var finalTakeHomePay by mutableDoubleStateOf(0.0)

    var incomeComponents by mutableStateOf<List<SalaryChartItemData>>(emptyList())
    var deductionComponents by mutableStateOf<List<SalaryChartItemData>>(emptyList())

    // State untuk fitur PDF
    var isPdfGenerating by mutableStateOf(false)
    var pdfMessage by mutableStateOf<String?>(null)

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            isLoading = true
            // Jika untuk user lain, ambil namanya
            if (navUserId != "me") {
                val empRes = employeeRepository.getEmployeeById(navUserId)
                if (empRes is ResultState.Success) {
                    employeeName = empRes.data.fullName
                }
            }
            
            generateHistoryCalendar()
            loadMySalaries()
            isLoading = false
        }
    }

    /**
     * Generator Periode Riwayat (Past Periods):
     * - Monthly: Dimulai dari BULAN LALU (Bulan ini tidak tampil karena masih berjalan).
     *   Subtitle: "1 [Bulan] [Tahun] - [AkhirBulan] [Bulan] [Tahun]"
     * - Weekly: Dimulai dari MINGGU LALU (Minggu ini tidak tampil karena masih berjalan).
     *   Subtitle: "[TglAwalMinggu] - [TglAkhirMinggu]"
     */
    private fun generateHistoryCalendar() {
        val today = LocalDate.now()
        val periods = mutableListOf<SalaryPeriodItem>()
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))
        val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))
        val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))

        if (activePayrollType == PayrollType.MONTHLY) {
            // BULANAN: Mundur 1 s/d 12 bulan (Dimulai dari bulan lalu)
            for (i in 1..12) {
                val date = today.minusMonths(i.toLong())
                val startOfMonth = date.withDayOfMonth(1)
                val endOfMonth = date.withDayOfMonth(date.lengthOfMonth())

                val label = date.format(monthYearFormatter)
                val subtitle = "${startOfMonth.format(dateFormatter)} - ${endOfMonth.format(dateFormatter)}"

                periods.add(
                    SalaryPeriodItem(
                        label = label,
                        periodSubtitle = subtitle,
                        month = date.monthValue,
                        year = date.year,
                        startDate = startOfMonth.toString(),
                        endDate = endOfMonth.toString()
                    )
                )
            }
        } else {
            // MINGGUAN: Dimulai dari minggu sebelumnya (yang sudah berlalu)
            // 7 hari per minggu (Senin - Minggu)
            val dayOfWeekVal = today.dayOfWeek.value
            val currentMonday = today.minusDays((dayOfWeekVal - 1).toLong())
            val lastCompletedSunday = currentMonday.minusDays(1)
            val lastCompletedMonday = lastCompletedSunday.minusDays(6)

            for (i in 0..11) {
                val startOfWeek = lastCompletedMonday.minusWeeks(i.toLong())
                val endOfWeek = lastCompletedSunday.minusWeeks(i.toLong())

                val weekOfMonth = maxOf(1, minOf(4, ((startOfWeek.dayOfMonth - 1) / 7) + 1))
                val monthLabel = startOfWeek.format(monthYearFormatter)
                val label = "Minggu ke-$weekOfMonth ($monthLabel)"
                val subtitle = "${startOfWeek.format(shortDateFormatter)} - ${endOfWeek.format(shortDateFormatter)}"

                periods.add(
                    SalaryPeriodItem(
                        label = label,
                        periodSubtitle = subtitle,
                        month = startOfWeek.monthValue,
                        year = startOfWeek.year,
                        startDate = startOfWeek.toString(),
                        endDate = endOfWeek.toString()
                    )
                )
            }
        }

        availablePeriods = periods
        availableMonths = periods.map { it.label }

        selectedMonthIndex = 0
        if (availablePeriods.isNotEmpty()) {
            selectedMonth = availablePeriods[0].label
            periodString = availablePeriods[0].periodSubtitle
        }
    }

    fun loadMySalaries() {
        if (availablePeriods.isNotEmpty()) {
            calculateSalaryForSelectedMonth()
        }
    }

    fun switchPayrollType(type: PayrollType) {
        if (activePayrollType != type) {
            activePayrollType = type
            generateHistoryCalendar()
            calculateSalaryForSelectedMonth()
        }
    }

    // Navigasi Periode: Kanan (Lebih Baru)
    fun nextMonth() {
        if (selectedMonthIndex > 0) {
            selectedMonthIndex--
            selectedMonth = availableMonths[selectedMonthIndex]
            calculateSalaryForSelectedMonth()
        }
    }

    // Navigasi Periode: Kiri (Lebih Lampau)
    fun previousMonth() {
        if (selectedMonthIndex < availableMonths.size - 1) {
            selectedMonthIndex++
            selectedMonth = availableMonths[selectedMonthIndex]
            calculateSalaryForSelectedMonth()
        }
    }

    private fun calculateSalaryForSelectedMonth() {
        val currentId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
        val targetUserId = if (navUserId == "me") currentId else navUserId
        
        if (selectedMonthIndex >= availablePeriods.size) return

        val period = availablePeriods[selectedMonthIndex]
        selectedMonth = period.label
        periodString = period.periodSubtitle

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val salaryJob = async {
                salaryRepository.getHistoricalSalaryByUser(
                    userId = targetUserId,
                    month = period.month,
                    year = period.year,
                    startDate = period.startDate,
                    endDate = period.endDate
                )
            }
            val schedulesJob = async {
                attendanceRepository.getMonthlySchedulesList(
                    userId = targetUserId,
                    startDate = period.startDate,
                    endDate = period.endDate
                )
            }

            val result = salaryJob.await()
            val schedulesRes = schedulesJob.await()

            if (result is ResultState.Success) {
                val salary = result.data
                selectedSalary = salary

                // Jika tipe payroll karyawan dari DB (misal: Weekly) berbeda dari state aktif (Monthly),
                // update activePayrollType, generate ulang kalender periode (Mingguan), lalu muat ulang data.
                if (activePayrollType != salary.payrollType) {
                    activePayrollType = salary.payrollType
                    generateHistoryCalendar()
                    calculateSalaryForSelectedMonth()
                    return@launch
                }

                totalPresent = salary.totalPresent
                totalAlfa = salary.totalAlfa

                if (schedulesRes is ResultState.Success) {
                    totalSchedules = schedulesRes.data.size
                } else {
                    totalSchedules = totalPresent + totalAlfa
                }

                // 1. Hitung Pendapatan berdasarkan Tipe Payroll (Weekly vs Monthly)
                val basic: Double
                val transport: Double
                val meal: Double
                val position: Double

                if (salary.payrollType == PayrollType.WEEKLY) {
                    // MINGGUAN: Karyawan HANYA dibayar berdasarkan hari HADIR (payPerDay * totalPresent)
                    // Jika Alfa / Mangkir / 0 Hadir, Gaji = 0!
                    basic = salary.payPerDay * salary.totalPresent
                    transport = 0.0
                    meal = 0.0
                    position = 0.0
                } else {
                    // BULANAN: Gaji Pokok & Tunjangan Bulanan Penuh
                    basic = salary.basicSalary
                    transport = salary.transportAllowance
                    meal = salary.mealAllowance
                    position = salary.positionAllowance
                }

                val bonus = salary.totalBonus
                totalIncome = basic + transport + meal + position + bonus

                // 2. Hitung Total Potongan
                val alphaDeduction = if (salary.payrollType == PayrollType.MONTHLY) {
                    salary.totalAlfa * salary.payPerDay
                } else 0.0 // Pada weekly, alfa otomatis tidak dibayar (tidak menambah pendapatan)

                totalDeduction = salary.totalDeduction + alphaDeduction + salary.totalPenalty

                // 3. Take Home Pay Bersih
                finalTakeHomePay = maxOf(0.0, totalIncome - totalDeduction)

                // ==========================================
                // PECAH KOMPONEN PENGHASILAN UNTUK CHART
                // ==========================================
                val incomeList = mutableListOf<SalaryChartItemData>()
                if (basic > 0) {
                    val basicLabel = if (salary.payrollType == PayrollType.WEEKLY) {
                        "Gaji Harian ($totalPresent Hari)"
                    } else {
                        "Gaji Pokok"
                    }
                    incomeList.add(SalaryChartItemData(basicLabel, basic, Color(0xFF4DB6AC)))
                }
                if (transport > 0) incomeList.add(SalaryChartItemData("Tunjangan Transport", transport, Color(0xFFFFB74D)))
                if (meal > 0) incomeList.add(SalaryChartItemData("Tunjangan Makan", meal, Color(0xFFFFF176)))
                if (position > 0) incomeList.add(SalaryChartItemData("Tunjangan Posisi", position, Color(0xFF9575CD)))

                if (bonus > 0) {
                    incomeList.add(SalaryChartItemData("Bonus & Insentif", bonus, Color(0xFF00897B)))
                }
                incomeComponents = incomeList

                // ==========================================
                // PECAH KOMPONEN POTONGAN UNTUK CHART
                // ==========================================
                val deductionList = mutableListOf<SalaryChartItemData>()
                val colors = listOf(Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8))

                salary.deductions.forEachIndexed { index, ded ->
                    val amount = if (ded.type == "fixed") ded.fixedAmount else (basic * (ded.percentageValue / 100))
                    if (amount > 0) {
                        deductionList.add(SalaryChartItemData(ded.name, amount, colors[index % colors.size]))
                    }
                }

                if (alphaDeduction > 0) {
                    deductionList.add(SalaryChartItemData("Potongan Mangkir/Alfa", alphaDeduction, Color(0xFFD32F2F)))
                }

                if (salary.totalPenalty > 0) {
                    deductionList.add(SalaryChartItemData("Sanksi Pelanggaran", salary.totalPenalty, Color(0xFFC62828)))
                }

                deductionComponents = deductionList

            } else if (result is ResultState.Error) {
                errorMessage = result.message
            }

            isLoading = false
        }
    }

    /** Ambil nama toko primary user untuk header PDF */
    private suspend fun fetchPrimaryStoreName(): String {
        return try {
            val currentId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return "PerformaX"
            val targetId = if (navUserId == "me") currentId else navUserId
            val empRes = employeeRepository.getEmployeeById(targetId)
            if (empRes is ResultState.Success) {
                val stores = empRes.data.stores
                (stores.firstOrNull { it.isPrimary } ?: stores.firstOrNull())?.name ?: "PerformaX"
            } else "PerformaX"
        } catch (e: Exception) { "PerformaX" }
    }

    /**
     * Generate PDF slip gaji untuk periode yang sedang aktif, lalu simpan ke Downloads.
     */
    fun generateAndSavePdf(context: android.content.Context) {
        val salary = selectedSalary ?: return
        viewModelScope.launch {
            isPdfGenerating = true
            pdfMessage = null
            try {
                val storeName = fetchPrimaryStoreName()

                // Kumpulkan komponen pemasukan
                val incomeItems = incomeComponents.map { it.name to it.amount }
                // Kumpulkan komponen potongan
                val deductionItems = deductionComponents.map { it.name to it.amount }

                val pdf = SalaryPdfGenerator.generateSlipGaji(
                    salary = salary,
                    employeeName = employeeName.ifBlank { salary.userName },
                    periodLabel = selectedMonth,
                    periodRange = periodString,
                    storeName = storeName,
                    totalPresent = totalPresent,
                    totalAlfa = totalAlfa,
                    totalSchedules = totalSchedules,
                    totalIncome = totalIncome,
                    totalDeduction = totalDeduction,
                    takeHomePay = finalTakeHomePay,
                    incomeItems = incomeItems,
                    deductionItems = deductionItems
                )

                val safeName = selectedMonth.replace(" ", "-").lowercase()
                val fileName = "slip-gaji-$safeName.pdf"
                val result = PdfSaveHelper.savePdfToDownloads(context, pdf, fileName)
                pdfMessage = if (result != null) {
                    "✅ Slip gaji disimpan ke Downloads/$fileName"
                } else {
                    "❌ Gagal menyimpan PDF. Coba lagi."
                }
            } catch (e: Exception) {
                pdfMessage = "❌ Error: ${e.message}"
            }
            isPdfGenerating = false
        }
    }
}