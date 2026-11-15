package id.biz.yuka.performax.ui.screen.employee.rule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.EmployeeRuleEntity
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.EmployeeRuleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.CreateEmployeeRuleRequest
import id.biz.yuka.libperformx.request.CreateStoreRuleRequest
import id.biz.yuka.libperformx.request.UpdateEmployeeRuleRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeRuleViewModel @Inject constructor(
    private val ruleRepository: EmployeeRuleRepository,
    private val storeRepository: StoreRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    var rules by mutableStateOf<List<EmployeeRuleEntity>>(emptyList())

    // Store List & Selection
    var stores by mutableStateOf<List<StoreEntity>>(emptyList())
    var selectedStoreForView by mutableStateOf<StoreEntity?>(null)

    // Aturan yang sudah difilter untuk toko yang sedang dipilih
    var rulesForSelectedStore by mutableStateOf<List<EmployeeRuleEntity>>(emptyList())

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val rulesDeferred = async { ruleRepository.getAllRules() }
            val storesDeferred = async { storeRepository.getAllStores() }

            val rulesResult = rulesDeferred.await()
            val storesResult = storesDeferred.await()

            if (rulesResult is ResultState.Success) {
                rules = rulesResult.data
            } else if (rulesResult is ResultState.Error) {
                errorMessage = rulesResult.exception?.message
            }

            if (storesResult is ResultState.Success) {
                stores = storesResult.data
                if (selectedStoreForView == null && stores.isNotEmpty()) {
                    selectStore(stores.first())
                } else if (selectedStoreForView != null) {
                    selectStore(selectedStoreForView!!)
                }
            } else if (storesResult is ResultState.Error) {
                errorMessage = storesResult.exception?.message
            }

            isLoading = false
        }
    }

    fun selectStore(store: StoreEntity) {
        selectedStoreForView = store
        viewModelScope.launch {
            isLoading = true
            val result = ruleRepository.getRulesByStoreId(store.id)
            if (result is ResultState.Success) {
                val activeStoreRuleIds = result.data.filter { it.isActive }.map { it.ruleId }
                rulesForSelectedStore = rules.filter { activeStoreRuleIds.contains(it.id) }
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message
            }
            isLoading = false
        }
    }

    fun addRule(request: CreateEmployeeRuleRequest) {
        viewModelScope.launch {
            isLoading = true
            val result = ruleRepository.createRule(request)
            if (result is ResultState.Success) {
                loadData()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal membuat aturan"
                isLoading = false
            }
        }
    }

    // --- FUNGSI BARU UNTUK EDIT & HAPUS (DIPERBAIKI) ---
    fun updateRule(ruleId: String, request: UpdateEmployeeRuleRequest) {
        viewModelScope.launch {
            isLoading = true
            val result = ruleRepository.updateRule(ruleId, request)
            if (result is ResultState.Success) {
                loadData()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal mengubah aturan"
                isLoading = false
            }
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            isLoading = true
            val result = ruleRepository.deleteRule(ruleId)
            if (result is ResultState.Success) {
                loadData()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal menghapus aturan"
                isLoading = false
            }
        }
    }

    fun assignRuleToStore(storeId: String, ruleId: String) {
        viewModelScope.launch {
            isLoading = true
            val request = CreateStoreRuleRequest(store_id = storeId, rule_id = ruleId)
            val result = ruleRepository.addRuleToStore(request)
            if (result is ResultState.Success) {
                loadData()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal assign aturan"
                isLoading = false
            }
        }
    }

    // --- FUNGSI BARU UNTUK UNASSIGN DARI TOKO (DIPERBAIKI) ---
    fun removeRuleFromStore(storeId: String, ruleId: String) {
        viewModelScope.launch {
            isLoading = true
            val result = ruleRepository.removeRuleFromStore(storeId, ruleId)
            if (result is ResultState.Success) {
                loadData()
            } else if (result is ResultState.Error) {
                errorMessage = result.exception?.message ?: "Gagal melepaskan aturan"
                isLoading = false
            }
        }
    }
}