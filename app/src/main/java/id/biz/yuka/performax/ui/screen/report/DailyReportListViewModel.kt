package id.biz.yuka.performax.ui.screen.report

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
import javax.inject.Inject

@HiltViewModel
class DailyReportListViewModel @Inject constructor(
    private val dailyReportRepository: DailyReportRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var reports by mutableStateOf<List<DailyReportEntity>>(emptyList())
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = dailyReportRepository.getAllReports()) {
                is ResultState.Success -> reports = result.data
                is ResultState.Error  -> errorMessage = result.message
            }
            isLoading = false
        }
    }
}
