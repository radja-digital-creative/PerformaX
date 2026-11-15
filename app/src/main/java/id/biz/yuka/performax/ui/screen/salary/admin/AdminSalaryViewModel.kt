package id.biz.yuka.performax.ui.screen.salary.admin

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.entity.PayrollType
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.SalaryRepository
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.ui.screen.salary.SalaryPeriodItem
import id.biz.yuka.performax.ui.utils.PdfSaveHelper
import id.biz.yuka.performax.ui.utils.SalaryPdfGenerator
import id.biz.yuka.performax.ui.utils.SlipData
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AdminSalaryViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val salaryRepository: SalaryRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var employees = mutableStateListOf<EmployeeEntity>()
    var errorMessage by mutableStateOf<String?>(null)

    // ── Filter Periode (mirip SalaryViewModel) ──────────────────────────────
    private var availablePeriods = listOf<SalaryPeriodItem>()
    var availableMonths by mutableStateOf<List<String>>(emptyList())
    var selectedMonth by mutableStateOf("")
    var selectedMonthIndex by mutableIntStateOf(0)
    var periodString by mutableStateOf("")

    // ── State PDF ────────────────────────────────────────────────────────────
    var isPdfGenerating by mutableStateOf(false)
    var pdfMessage by mutableStateOf<String?>(null)

    init {
        generateHistoryCalendar()
        loadEmployees()
    }

    /** Bangun daftar bulan 12 bulan ke belakang (mulai dari bulan lalu) */
    fun generateHistoryCalendar() {
        val today = LocalDate.now()
        val periods = mutableListOf<SalaryPeriodItem>()
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))
        val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))

        for (i in 1..12) {
            val date = today.minusMonths(i.toLong())
            val startOfMonth = date.withDayOfMonth(1)
            val endOfMonth = date.withDayOfMonth(date.lengthOfMonth())
            periods.add(
                SalaryPeriodItem(
                    label = date.format(monthYearFormatter),
                    periodSubtitle = "${startOfMonth.format(dateFormatter)} - ${endOfMonth.format(dateFormatter)}",
                    month = date.monthValue,
                    year = date.year,
                    startDate = startOfMonth.toString(),
                    endDate = endOfMonth.toString()
                )
            )
        }
        availablePeriods = periods
        availableMonths = periods.map { it.label }
        selectedMonthIndex = 0
        if (periods.isNotEmpty()) {
            selectedMonth = periods[0].label
            periodString = periods[0].periodSubtitle
        }
    }

    fun nextMonth() {
        if (selectedMonthIndex > 0) {
            selectedMonthIndex--
            selectedMonth = availableMonths[selectedMonthIndex]
            periodString = availablePeriods[selectedMonthIndex].periodSubtitle
        }
    }

    fun previousMonth() {
        if (selectedMonthIndex < availableMonths.size - 1) {
            selectedMonthIndex++
            selectedMonth = availableMonths[selectedMonthIndex]
            periodString = availablePeriods[selectedMonthIndex].periodSubtitle
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = employeeRepository.getAllEmployees()) {
                is ResultState.Success -> {
                    employees.clear()
                    employees.addAll(result.data)
                }
                is ResultState.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    /** Ambil nama toko primary owner untuk header PDF */
    private suspend fun fetchOwnerPrimaryStore(): String {
        return try {
            val ownerId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return "PerformaX"
            val empRes = employeeRepository.getEmployeeById(ownerId)
            if (empRes is ResultState.Success) {
                val stores = empRes.data.stores
                (stores.firstOrNull { it.isPrimary } ?: stores.firstOrNull())?.name ?: "PerformaX"
            } else "PerformaX"
        } catch (e: Exception) { "PerformaX" }
    }

    /**
     * Generate rekap PDF semua karyawan untuk periode yang aktif, lalu simpan ke Downloads.
     * Setiap karyawan = 1 halaman di PDF.
     */
    fun generateRekapPdf(context: Context) {
        val period = if (availablePeriods.isNotEmpty()) availablePeriods[selectedMonthIndex] else return
        viewModelScope.launch {
            isPdfGenerating = true
            pdfMessage = null
            try {
                val storeName = fetchOwnerPrimaryStore()

                // Fetch salary data semua karyawan untuk periode ini
                val salaryResult = salaryRepository.getHistoricalSalaries(
                    month = period.month,
                    year = period.year,
                    startDate = period.startDate,
                    endDate = period.endDate
                )

                if (salaryResult is ResultState.Error) {
                    pdfMessage = "❌ Gagal fetch data gaji: ${salaryResult.message}"
                    isPdfGenerating = false
                    return@launch
                }

                val salaries = (salaryResult as ResultState.Success).data
                if (salaries.isEmpty()) {
                    pdfMessage = "⚠️ Tidak ada data gaji untuk periode $selectedMonth."
                    isPdfGenerating = false
                    return@launch
                }

                // Konversi tiap salary entity ke SlipData
                val slips = salaries.map { salary ->
                    val isWeekly = salary.payrollType == PayrollType.WEEKLY
                    val basic = if (isWeekly) salary.payPerDay * salary.totalPresent else salary.basicSalary
                    val transport = if (isWeekly) 0.0 else salary.transportAllowance
                    val meal = if (isWeekly) 0.0 else salary.mealAllowance
                    val position = if (isWeekly) 0.0 else salary.positionAllowance
                    val bonus = salary.totalBonus
                    val totalIncome = basic + transport + meal + position + bonus

                    val alphaDeduction = if (!isWeekly) salary.totalAlfa * salary.payPerDay else 0.0
                    val totalDeduction = salary.totalDeduction + alphaDeduction + salary.totalPenalty
                    val takeHomePay = maxOf(0.0, totalIncome - totalDeduction)

                    val incomeItems = buildList {
                        if (basic > 0) add((if (isWeekly) "Gaji Harian (${salary.totalPresent} Hari)" else "Gaji Pokok") to basic)
                        if (transport > 0) add("Tunjangan Transport" to transport)
                        if (meal > 0) add("Tunjangan Makan" to meal)
                        if (position > 0) add("Tunjangan Jabatan" to position)
                        if (bonus > 0) add("Bonus & Insentif" to bonus)
                    }
                    val deductionItems = buildList {
                        salary.deductions.forEach { ded ->
                            val amount = if (ded.type == "fixed") ded.fixedAmount else (basic * (ded.percentageValue / 100))
                            if (amount > 0) add(ded.name to amount)
                        }
                        if (alphaDeduction > 0) add("Potongan Mangkir/Alfa" to alphaDeduction)
                        if (salary.totalPenalty > 0) add("Sanksi Pelanggaran" to salary.totalPenalty)
                    }

                    SlipData(
                        employeeName = salary.userName,
                        payrollType = salary.payrollType,
                        totalPresent = salary.totalPresent,
                        totalAlfa = salary.totalAlfa,
                        totalSchedules = salary.totalPresent + salary.totalAlfa,
                        totalIncome = totalIncome,
                        totalDeduction = totalDeduction,
                        takeHomePay = takeHomePay,
                        incomeItems = incomeItems,
                        deductionItems = deductionItems
                    )
                }

                val pdf = SalaryPdfGenerator.generateRekapSlipGaji(
                    slips = slips,
                    periodLabel = selectedMonth,
                    periodRange = periodString,
                    storeName = storeName
                )

                val safeName = selectedMonth.replace(" ", "-").lowercase()
                val fileName = "rekap-gaji-$safeName.pdf"
                val result = PdfSaveHelper.savePdfToDownloads(context, pdf, fileName)
                pdfMessage = if (result != null) {
                    "✅ Rekap gaji ${slips.size} karyawan disimpan ke Downloads/$fileName"
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
