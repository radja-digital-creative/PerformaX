package id.biz.yuka.performax.ui.screen.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.SalaryDetailEntity
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.SalaryRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import id.biz.yuka.libperformx.entity.PayrollType
import id.biz.yuka.libperformx.repository.AuthRepository
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val salaryRepository: SalaryRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var isInitialRoleChecking by mutableStateOf(true)
    var userRole by mutableStateOf("employee")
    val isOwner: Boolean get() = userRole == "owner" || userRole == "admin"

    var salaryDetail by mutableStateOf<SalaryDetailEntity?>(null)
    
    // Aggregates for Owner
    var totalOwnerIncome by mutableStateOf(0.0)
    var totalOwnerDeduction by mutableStateOf(0.0)
    var totalOwnerTakeHomePay by mutableStateOf(0.0)

    var monthlyAttendanceCount by mutableIntStateOf(0)
    var monthlyScheduleCount by mutableIntStateOf(0)
    var passedScheduleCount by mutableIntStateOf(0)
    var periodString by mutableStateOf("")

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
                    fetchInsightData()
                }
                is ResultState.Error -> {
                    userRole = "employee"
                    fetchInsightData()
                }
            }
            isInitialRoleChecking = false
        }
    }

    fun fetchInsightData() {
        viewModelScope.launch {
            isLoading = true
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch

            val today = LocalDate.now()
            val localeId = Locale("id", "ID")
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", localeId)

            // Setup Periode (Monthly default for aggregate, specific for user)
            val startOfMonth = today.withDayOfMonth(1)
            val startDateStr = startOfMonth.toString()
            val endDateStr = today.toString()
            
            periodString = "Periode ${startOfMonth.format(formatter)} - ${today.format(formatter)}"

            if (isOwner) {
                // DATA AGREGAT UNTUK OWNER (Semua Toko)
                val salariesRes = salaryRepository.getHistoricalSalaries(
                    startDate = startDateStr,
                    endDate = endDateStr
                )
                
                if (salariesRes is ResultState.Success) {
                    val allSalaries = salariesRes.data
                    
                    var aggIncome = 0.0
                    var aggDeduction = 0.0
                    
                    allSalaries.forEach { salary ->
                        // Hitung proporsional (sederhana untuk aggregate)
                        val dailyRate = salary.payPerDay
                        // Asumsi aggregate menggunakan Passed Schedules rata-rata atau real-time dari DB
                        // Karena kita tidak punya passedScheduleCount per user di sini tanpa query tambahan,
                        // kita gunakan data 'totalPresent' & 'totalAlfa' yang sudah dihitung RPC per rentang waktu.
                        
                        val income = if (salary.payrollType == PayrollType.WEEKLY) {
                            (salary.totalPresent * dailyRate) + salary.totalBonus
                        } else {
                            // Monthly: Basic prorated + allowances + bonus
                            // Untuk insight ringkas, kita ambil THP real dari DB hasil RPC
                            (salary.totalPresent * dailyRate) + salary.totalBonus 
                        }
                        
                        val alphaDeduction = if (salary.payrollType == PayrollType.MONTHLY) salary.totalAlfa * dailyRate else 0.0
                        val deduction = salary.totalDeduction + salary.totalPenalty + alphaDeduction
                        
                        aggIncome += income
                        aggDeduction += deduction
                    }
                    
                    totalOwnerIncome = aggIncome
                    totalOwnerDeduction = aggDeduction
                    totalOwnerTakeHomePay = maxOf(0.0, aggIncome - aggDeduction)
                }
            } else {
                // DATA INDIVIDU UNTUK KARYAWAN
                // (Logic lama tetap dipertahankan)
                val firstSalaryRes = salaryRepository.getHistoricalSalaryByUser(userId)

                if (firstSalaryRes is ResultState.Success) {
                    val isWeekly = firstSalaryRes.data.payrollType == PayrollType.WEEKLY
                    
                    val queryStartDateStr: String
                    val queryEndDateStr: String

                    if (isWeekly) {
                        val dayOfWeekVal = today.dayOfWeek.value
                        val currentMonday = today.minusDays((dayOfWeekVal - 1).toLong())
                        val currentSunday = currentMonday.plusDays(6)
                        queryStartDateStr = currentMonday.toString()
                        queryEndDateStr = currentSunday.toString()
                        periodString = "Periode ${currentMonday.format(formatter)} - ${currentSunday.format(formatter)}"
                    } else {
                        queryStartDateStr = startDateStr
                        queryEndDateStr = endDateStr
                        periodString = "Periode ${startOfMonth.format(formatter)} - ${today.format(formatter)}"
                    }

                    val salaryJob = async {
                        salaryRepository.getHistoricalSalaryByUser(
                            userId = userId,
                            startDate = queryStartDateStr,
                            endDate = queryEndDateStr
                        )
                    }
                    val monthlyAttJob = async { attendanceRepository.getAllAttendances(queryStartDateStr, queryEndDateStr) }
                    val schedulesJob = async { attendanceRepository.getMonthlySchedulesList(userId, queryStartDateStr, queryEndDateStr) }

                    val salaryRes = salaryJob.await()
                    if (salaryRes is ResultState.Success) salaryDetail = salaryRes.data

                    val monthlyAttRes = monthlyAttJob.await()
                    if (monthlyAttRes is ResultState.Success) {
                        monthlyAttendanceCount = monthlyAttRes.data.count { it.userId == userId && !it.checkInTime.isNullOrBlank() && it.checkInTime != "-" }
                    }

                    val schedulesRes = schedulesJob.await()
                    if (schedulesRes is ResultState.Success) {
                        val datesList = schedulesRes.data
                        monthlyScheduleCount = datesList.size
                        passedScheduleCount = datesList.count { dateStr ->
                            try {
                                val cleanDateStr = dateStr.substringBefore("T").substringBefore(" ")
                                val scheduleDate = LocalDate.parse(cleanDateStr)
                                !scheduleDate.isAfter(today)
                            } catch (e: Exception) { false }
                        }
                    }
                }
            }

            isLoading = false
        }
    }
}