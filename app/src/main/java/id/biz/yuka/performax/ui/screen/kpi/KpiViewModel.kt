package id.biz.yuka.performax.ui.screen.kpi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.KpiEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.KpiRepository
import id.biz.yuka.libperformx.repository.UserRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class KpiViewModel @Inject constructor(
    private val kpiRepository: KpiRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var leaderboard by mutableStateOf<List<KpiEntity>>(emptyList())
    var myKpi by mutableStateOf<KpiEntity?>(null)

    var isLoading by mutableStateOf(false)
    var isOwnerOrAdmin by mutableStateOf(false)

    // Filter Default: Bulan ini
    var currentMonth = LocalDate.now().monthValue
    var currentYear = LocalDate.now().year

    init { loadKpiData() }

    fun loadKpiData() {
        viewModelScope.launch {
            isLoading = true

            // ✅ Menggunakan Repository, ViewModel bersih dari dependensi Supabase!
            val userId = authRepository.getCurrentUserId()

            if (userId != null) {
                // 1. Dapatkan Profil dan Cek Role
                val profileRes = userRepository.getUserProfile(userId)
                if (profileRes is ResultState.Success) {
                    val role = profileRes.data.role.lowercase()
                    isOwnerOrAdmin = role == "owner" || role == "admin"
                }

                // 2. Ambil KPI Diri Sendiri secara spesifik menggunakan getEmployeeKpiDetail
                val myKpiRes = kpiRepository.getEmployeeKpiDetail(userId, currentMonth, currentYear)
                if (myKpiRes is ResultState.Success) {
                    myKpi = myKpiRes.data
                }

                // 3. Ambil Leaderboard secara terpisah
                val boardRes = kpiRepository.getKpiLeaderboard(currentMonth, currentYear)
                if (boardRes is ResultState.Success) {
                    leaderboard = boardRes.data
                }
            }
            isLoading = false
        }
    }
}