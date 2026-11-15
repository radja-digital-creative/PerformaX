package id.biz.yuka.performax.ui.screen.home.salary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.PayrollType
import id.biz.yuka.libperformx.entity.SalaryDetailEntity
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.SalaryRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class ChartItemData(
    val name: String,
    val amount: Double,
    val color: Color
)

data class EmployeeRecapItem(
    val employee: EmployeeEntity,
    val income: Double,
    val deduction: Double,
    val net: Double
)

@HiltViewModel
class IncomeRecapViewModel @Inject constructor(
    private val salaryRepository: SalaryRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var isInitialRoleChecking by mutableStateOf(true)
    var userRole by mutableStateOf("employee")
    val isOwner: Boolean get() = userRole == "owner" || userRole == "admin"

    var salaryDetail by mutableStateOf<SalaryDetailEntity?>(null)
    var employeeRecaps = mutableStateListOf<EmployeeRecapItem>()

    var periodString by mutableStateOf("")
    var totalIncome by mutableStateOf(0.0)
    var totalDeduction by mutableStateOf(0.0)
    var takeHomePay by mutableStateOf(0.0)

    var incomeComponents by mutableStateOf<List<ChartItemData>>(emptyList())
    var deductionComponents by mutableStateOf<List<ChartItemData>>(emptyList())

    init {
        checkRoleAndFetchData()
    }

    private fun checkRoleAndFetchData() {
        viewModelScope.launch {
            isInitialRoleChecking = true
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
            
            when (val roleRes = authRepository.getUserRole(currentUserId)) {
                is ResultState.Success -> {
                    userRole = roleRes.data.lowercase()
                    fetchData()
                }
                is ResultState.Error -> {
                    userRole = "employee"
                    fetchData()
                }
            }
            isInitialRoleChecking = false
        }
    }

    fun fetchData() {
        viewModelScope.launch {
            isLoading = true

            val localeId = Locale("id", "ID")
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", localeId)

            val startOfMonth = today.withDayOfMonth(1)
            val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
            val startDateStr = startOfMonth.toString()
            val endDateStr = today.toString()
            val queryEndDateStr = endOfMonth.toString()

            periodString = "${startOfMonth.format(formatter)} - ${today.format(formatter)}"

            if (isOwner) {
                // DATA AGREGAT UNTUK OWNER
                val salariesRes = salaryRepository.getHistoricalSalaries(
                    startDate = startDateStr,
                    endDate = endDateStr
                )
                
                if (salariesRes is ResultState.Success) {
                    val allSalaries = salariesRes.data
                    val recaps = mutableListOf<EmployeeRecapItem>()
                    
                    var aggIncome = 0.0
                    var aggDeduction = 0.0
                    
                    allSalaries.forEach { salary ->
                        val dailyRate = salary.payPerDay
                        val income = if (salary.payrollType == PayrollType.WEEKLY) {
                            (salary.totalPresent * dailyRate) + salary.totalBonus
                        } else {
                            (salary.totalPresent * dailyRate) + salary.totalBonus 
                        }
                        
                        val alphaDeduction = if (salary.payrollType == PayrollType.MONTHLY) salary.totalAlfa * dailyRate else 0.0
                        val deduction = salary.totalDeduction + salary.totalPenalty + alphaDeduction
                        
                        val net = maxOf(0.0, income - deduction)
                        
                        aggIncome += income
                        aggDeduction += deduction
                        
                        // Cari data employee untuk profil
                        val empRes = employeeRepository.getEmployeeById(salary.userId)
                        if (empRes is ResultState.Success) {
                            recaps.add(EmployeeRecapItem(empRes.data, income, deduction, net))
                        }
                    }
                    
                    employeeRecaps.clear()
                    employeeRecaps.addAll(recaps)
                    
                    totalIncome = aggIncome
                    totalDeduction = aggDeduction
                    takeHomePay = maxOf(0.0, aggIncome - aggDeduction)
                    
                    // Components for Chart (Global)
                    incomeComponents = listOf(ChartItemData("Gaji Terhitung", aggIncome, Color(0xFF4DB6AC)))
                    deductionComponents = listOf(ChartItemData("Total Potongan", aggDeduction, Color(0xFFE57373)))
                }
            } else {
                // DATA INDIVIDU UNTUK KARYAWAN
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val firstSalaryRes = salaryRepository.getHistoricalSalaryByUser(userId)
                    if (firstSalaryRes is ResultState.Success) {
                        val isWeekly = firstSalaryRes.data.payrollType == PayrollType.WEEKLY
                        
                        val qStart: String
                        val qEnd: String
                        val qLimit: String
                        
                        if (isWeekly) {
                            val dayOfWeekVal = today.dayOfWeek.value
                            val currentMonday = today.minusDays((dayOfWeekVal - 1).toLong())
                            val currentSunday = currentMonday.plusDays(6)
                            qStart = currentMonday.toString()
                            qEnd = currentSunday.toString()
                            qLimit = currentSunday.toString()
                            periodString = "${currentMonday.format(formatter)} - ${currentSunday.format(formatter)}"
                        } else {
                            qStart = startDateStr
                            qEnd = endDateStr
                            qLimit = queryEndDateStr
                        }

                        val salaryJob = async {
                            salaryRepository.getHistoricalSalaryByUser(userId, startDate = qStart, endDate = qEnd)
                        }
                        val schedulesJob = async {
                            attendanceRepository.getMonthlySchedulesList(userId, startDate = qStart, endDate = qLimit)
                        }

                        val salaryRes = salaryJob.await()
                        val schedulesRes = schedulesJob.await()

                        if (salaryRes is ResultState.Success) {
                            val data = salaryRes.data
                            salaryDetail = data

                            var passedScheduleCount = 0
                            if (schedulesRes is ResultState.Success) {
                                passedScheduleCount = schedulesRes.data.count { dateStr ->
                                    try {
                                        val cleanDateStr = dateStr.substringBefore("T").substringBefore(" ")
                                        val scheduleDate = LocalDate.parse(cleanDateStr)
                                        !scheduleDate.isAfter(today)
                                    } catch (e: Exception) { false }
                                }
                            }

                            val dailyRate = data.payPerDay
                            val passedIncome = if (isWeekly && dailyRate > 0) {
                                data.totalPresent * dailyRate
                            } else {
                                passedScheduleCount * dailyRate
                            }

                            totalIncome = passedIncome + data.totalBonus
                            val alphaDeduction = if (!isWeekly) data.totalAlfa * dailyRate else 0.0
                            totalDeduction = data.totalDeduction + alphaDeduction + data.totalPenalty
                            takeHomePay = maxOf(0.0, totalIncome - totalDeduction)

                            // Income components mapping
                            val expectedIncome = data.basicSalary + data.totalAllowance
                            val proratedBasic = if (isWeekly) passedIncome else if (expectedIncome > 0) (data.basicSalary / expectedIncome) * passedIncome else 0.0
                            val proratedTransport = if (!isWeekly && expectedIncome > 0) (data.transportAllowance / expectedIncome) * passedIncome else data.transportAllowance
                            val proratedMeal = if (!isWeekly && expectedIncome > 0) (data.mealAllowance / expectedIncome) * passedIncome else data.mealAllowance
                            val proratedPosition = if (!isWeekly && expectedIncome > 0) (data.positionAllowance / expectedIncome) * passedIncome else data.positionAllowance

                            val incomeList = mutableListOf<ChartItemData>()
                            if (proratedBasic > 0) incomeList.add(ChartItemData(if (isWeekly) "Gaji Harian (${data.totalPresent} Hari)" else "Gaji Pokok Berjalan", proratedBasic, Color(0xFF4DB6AC)))
                            if (proratedTransport > 0) incomeList.add(ChartItemData("Tunjangan Transport", proratedTransport, Color(0xFFFFB74D)))
                            if (proratedMeal > 0) incomeList.add(ChartItemData("Tunjangan Makan", proratedMeal, Color(0xFFFFF176)))
                            if (proratedPosition > 0) incomeList.add(ChartItemData("Tunjangan Posisi", proratedPosition, Color(0xFF9575CD)))
                            if (data.totalBonus > 0) incomeList.add(ChartItemData("Bonus & Insentif", data.totalBonus, Color(0xFF00897B)))
                            incomeComponents = incomeList

                            val deductionList = mutableListOf<ChartItemData>()
                            val colors = listOf(Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8))
                            data.deductions.forEachIndexed { index, ded ->
                                val amount = if (ded.type == "fixed") ded.fixedAmount else (data.basicSalary * (ded.percentageValue / 100))
                                if (amount > 0) deductionList.add(ChartItemData(ded.name, amount, colors[index % colors.size]))
                            }
                            if (alphaDeduction > 0) deductionList.add(ChartItemData("Potongan Mangkir/Alfa", alphaDeduction, Color(0xFFD32F2F)))
                            if (data.totalPenalty > 0) deductionList.add(ChartItemData("Sanksi Pelanggaran", data.totalPenalty, Color(0xFFC62828)))
                            deductionComponents = deductionList
                        }
                    }
                }
            }
            isLoading = false
        }
    }
}