package id.biz.yuka.performax.ui.screen.bonus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.BonusEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.BonusRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BonusViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bonusRepository: BonusRepository // Menggunakan Repo Asli
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Role & Auth
    var isOwner by mutableStateOf(false)
    var currentUserId by mutableStateOf<String?>(null)

    // Navigasi Bulan
    var selectedDate by mutableStateOf(LocalDate.now())
    var currentMonthStr by mutableStateOf("")

    // Data Bonus (Telah menggunakan BonusEntity asli)
    var bonuses by mutableStateOf<List<BonusEntity>>(emptyList())

    // Filter Chips ("Semua", "Pending", "Cair")
    var selectedFilter by mutableStateOf("Semua")

    // Computed property: Memfilter data yang akan ditampilkan di UI
    val displayedBonuses: List<BonusEntity>
        get() = when (selectedFilter) {
            "Pending" -> bonuses.filter { !it.isProcessed }
            "Cair" -> bonuses.filter { it.isProcessed }
            else -> bonuses
        }

    // Computed property: Menghitung total dari bonus yang sedang ditampilkan (terfilter)
    val totalBonusAmount: Double
        get() = displayedBonuses.sumOf { it.amount }

    init {
        // Eksekusi berurutan: Ambil data User -> Set Tanggal -> Tarik Data DB
        viewModelScope.launch {
            isLoading = true

            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                currentUserId = userId
                val roleResult = authRepository.getUserRole(userId)
                if (roleResult is ResultState.Success) {
                    isOwner = roleResult.data == "owner"
                }
            }

            updateMonthString()
            loadBonuses() // Tarik data setelah Role dipastikan
        }
    }

    private fun updateMonthString() {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))
        currentMonthStr = selectedDate.format(formatter)
    }

    fun changeMonth(monthsToAdd: Long) {
        selectedDate = selectedDate.plusMonths(monthsToAdd)
        updateMonthString()
        loadBonuses()
    }

    fun loadBonuses() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // Kosongkan list sejenak agar UI merespons loading transisi
            bonuses = emptyList()

            // Format tanggal awal dan akhir bulan yang dipilih
            val startOfMonth = selectedDate.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endOfMonth = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth()).format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Eksekusi penarikan data berdasarkan Role
            val result = if (isOwner) {
                bonusRepository.getAllBonuses(startDate = startOfMonth, endDate = endOfMonth)
            } else {
                if (currentUserId != null) {
                    bonusRepository.getBonusesByUserId(
                        userId = currentUserId!!,
                        startDate = startOfMonth,
                        endDate = endOfMonth
                    )
                } else {
                    null
                }
            }

            if (result is ResultState.Success) {
                // Urutkan data berdasarkan tanggal kejadian (Terbaru di atas)
                bonuses = result.data.sortedByDescending { it.bonusDate }
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Terjadi kesalahan saat memuat data"
            }

            isLoading = false
        }
    }

    // Fungsi untuk Owner: Memproses bonus ke Payroll
    fun processBonus(bonusId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = bonusRepository.processBonus(bonusId)

            if (result is ResultState.Success) {
                // Tarik data ulang jika update status sukses
                loadBonuses()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal memproses bonus"
            }

            isLoading = false
        }
    }
}