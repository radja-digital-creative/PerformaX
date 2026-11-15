package id.biz.yuka.performax.ui.screen.kpi.rule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.KpiPointRuleEntity
import id.biz.yuka.libperformx.repository.KpiPointRuleRepository
import id.biz.yuka.libperformx.request.KpiPointRuleRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KpiRuleViewModel @Inject constructor(
    private val repository: KpiPointRuleRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var kpiRules by mutableStateOf<List<KpiPointRuleEntity>>(emptyList())

    init {
        loadRules()
    }

    fun loadRules() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            when (val result = repository.getAllRules()) {
                is ResultState.Success -> {
                    // Sorting berdasarkan ruleCode agar tampilannya rapi (IN_ dulu, baru OUT_)
                    kpiRules = result.data.sortedBy { it.ruleCode }
                }
                is ResultState.Error -> {
                    errorMessage = result.exception?.message ?: "Gagal memuat aturan KPI"
                }
            }
            isLoading = false
        }
    }

    fun updateRule(ruleCode: String, newPoint: Int, newDescription: String?) {
        viewModelScope.launch {
            isLoading = true
            val request = KpiPointRuleRequest(
                rule_code = ruleCode,
                point_value = newPoint,
                description = newDescription
            )
            val result = repository.updateRule(ruleCode, request)

            if (result is ResultState.Success) {
                loadRules() // Segarkan daftar setelah sukses update
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal memperbarui aturan"
                isLoading = false
            }
        }
    }
}