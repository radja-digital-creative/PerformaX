package id.biz.yuka.performax.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.SummaryPerformaEntity
import id.biz.yuka.libperformx.entity.UserProfileEntity
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.SummaryPerformaRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.repository.UserRepository
import id.biz.yuka.libperformx.summary.DailyReportCardState
import id.biz.yuka.libperformx.summary.DailyReportRepository
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.ui.navigation.Screen
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OperasionalViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val taskRepository: TaskRepository,
    private val summaryPerformaRepository: SummaryPerformaRepository,
    private val userRepository: UserRepository,
    private val dailyReportRepository: DailyReportRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var userProfile by mutableStateOf<UserProfileEntity?>(null)

    // Absensi Hari Ini
    var checkInTime by mutableStateOf("--:--")
    var checkOutTime by mutableStateOf("--:--")

    // Ringkasan
    var performanceSummary by mutableStateOf<SummaryPerformaEntity?>(null)
    var pendingTasksCount by mutableIntStateOf(0)

    // Daily Report (owner only)
    var dailyReportState by mutableStateOf<DailyReportCardState>(DailyReportCardState.Idle)

    init { fetchOperasionalData() }

    fun fetchOperasionalData() {
        viewModelScope.launch {
            isLoading = true
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
            val todayStr = LocalDate.now().toString()

            val profileJob = async { userRepository.getUserProfile(userId) }
            val attJob = async { attendanceRepository.getAllAttendances(todayStr, todayStr) }
            val taskJob = async { taskRepository.getTasks() }
            val summaryJob = async { summaryPerformaRepository.getPerformanceSummary(userId) }

            val profileRes = profileJob.await()
            if (profileRes is ResultState.Success) userProfile = profileRes.data

            val attRes = attJob.await()
            if (attRes is ResultState.Success) {
                val myAtt = attRes.data.find { it.userId == userId }
                checkInTime = if (!myAtt?.checkInTime.isNullOrBlank() && myAtt?.checkInTime != "-") myAtt!!.checkInTime else "--:--"
                checkOutTime = if (!myAtt?.checkOutTime.isNullOrBlank() && myAtt?.checkOutTime != "-") myAtt!!.checkOutTime else "--:--"
            }

            val taskRes = taskJob.await()
            if (taskRes is ResultState.Success) {
                pendingTasksCount = taskRes.data.count { task -> task.assignees.any { it.userId == userId && !it.isCompleted } }
            }

            val summaryRes = summaryJob.await()
            if (summaryRes is ResultState.Success) performanceSummary = summaryRes.data

            // Cek Daily Report — hanya untuk role owner
            val role = userProfile?.role?.lowercase() ?: "employee"
            if (role == "owner") {
                dailyReportState = DailyReportCardState.Loading
                val reportRes = dailyReportRepository.checkTodayReport(todayStr)
                dailyReportState = when (reportRes) {
                    is ResultState.Success -> {
                        val entity = reportRes.data
                        if (entity != null) {
                            DailyReportCardState.Available(
                                generatedAt = entity.generatedAt,
                                fileUrl = entity.fileUrl
                            )
                        } else {
                            DailyReportCardState.NotAvailable
                        }
                    }
                    is ResultState.Error -> DailyReportCardState.NotAvailable
                }
            }

            isLoading = false
        }
    }

    fun getDynamicStoreRoute(): String {
        val roleId = userProfile?.role?.lowercase() ?: "employee"
        val userStores = userProfile?.stores ?: emptyList()
        val isOwnerOrAdmin = roleId == "owner" || roleId == "admin"

        return if (isOwnerOrAdmin) Screen.Store.route
        else if (userStores.size == 1) Screen.StoreDetail.createRoute(userStores.first().id)
        else Screen.Store.route
    }

    fun getDynamicLoanRoute(): String {
        val roleId = userProfile?.role?.lowercase() ?: "employee"
        val isOwnerOrAdmin = roleId == "owner" || roleId == "admin"

        return if (isOwnerOrAdmin) {
            Screen.Loan.route
        } else {
            Screen.LoanDetail.createRoute(userProfile?.id ?: "me")
        }
    }
}