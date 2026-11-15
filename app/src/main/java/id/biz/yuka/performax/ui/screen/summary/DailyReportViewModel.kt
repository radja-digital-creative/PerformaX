package id.biz.yuka.performax.ui.screen.summary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.summary.DailyReportEntity
import id.biz.yuka.libperformx.summary.DailyReportRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DailyReportViewModel @Inject constructor(
    private val dailyReportRepository: DailyReportRepository
) : ViewModel() {

    val todayStr: String = LocalDate.now().toString()

    var isLoading by mutableStateOf(false)
    var reportEntity by mutableStateOf<DailyReportEntity?>(null)
    var markdownContent by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    init { loadReport() }

    /**
     * Cek ketersediaan report hari ini, lalu download konten markdown-nya.
     */
    fun loadReport() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val checkRes = dailyReportRepository.checkTodayReport(todayStr)
            if (checkRes is ResultState.Success && checkRes.data != null) {
                reportEntity = checkRes.data
                // Download konten .md dari storage
                val mdRes = dailyReportRepository.downloadMarkdownContent(todayStr)
                when (mdRes) {
                    is ResultState.Success -> markdownContent = mdRes.data
                    is ResultState.Error -> {
                        // Jika error "tidak ditemukan", anggap belum ada report (Empty State)
                        if (mdRes.message.contains("tidak ditemukan", ignoreCase = true)) {
                            markdownContent = null
                        } else {
                            errorMessage = mdRes.message
                        }
                    }
                }
            } else {
                // Report belum ada — set null agar UI tampilkan empty state
                reportEntity = null
                markdownContent = null
            }

            isLoading = false
        }
    }

    /**
     * Trigger Edge Function untuk generate/regenerate report hari ini,
     * lalu reload konten setelah selesai.
     */
    fun generateReport() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val genRes = dailyReportRepository.generateReport(todayStr)
            when (genRes) {
                is ResultState.Success -> {
                    reportEntity = genRes.data
                    // Download konten yang baru di-generate
                    val mdRes = dailyReportRepository.downloadMarkdownContent(todayStr)
                    when (mdRes) {
                        is ResultState.Success -> markdownContent = mdRes.data
                        is ResultState.Error -> {
                            // Jika error "tidak ditemukan", mungkin storage belum sinkron
                            errorMessage = "Konten laporan sedang diproses, silakan refresh beberapa saat lagi."
                        }
                    }
                }
                is ResultState.Error -> errorMessage = genRes.message
            }

            isLoading = false
        }
    }
}
