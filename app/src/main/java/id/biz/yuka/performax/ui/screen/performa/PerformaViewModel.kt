package id.biz.yuka.performax.ui.screen.performa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.DetailPerformaEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.SummaryPerformaRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// State penampung UI
sealed class PerformaUiState {
    object Loading : PerformaUiState()
    data class Success(val data: DetailPerformaEntity) : PerformaUiState()
    data class Error(val message: String) : PerformaUiState()
}

@HiltViewModel
class PerformaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val performaRepository: SummaryPerformaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerformaUiState>(PerformaUiState.Loading)
    val uiState: StateFlow<PerformaUiState> = _uiState.asStateFlow()

    init {
        // Ambil data bulan dan tahun saat ini secara otomatis
        val today = LocalDate.now()
        fetchPerformanceDetail(today.monthValue, today.year)
    }

    fun fetchPerformanceDetail(month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.value = PerformaUiState.Loading

            // Ambil ID Karyawan yang sedang login
            val userId = authRepository.getCurrentUserId()

            if (userId != null) {
                when (val result = performaRepository.getPerformanceDetail(userId, month, year)) {
                    is ResultState.Success -> {
                        _uiState.value = PerformaUiState.Success(result.data)
                    }
                    is ResultState.Error -> {
                        _uiState.value = PerformaUiState.Error(result.message)
                    }
                }
            } else {
                _uiState.value = PerformaUiState.Error("Sesi pengguna tidak valid.")
            }
        }
    }
}