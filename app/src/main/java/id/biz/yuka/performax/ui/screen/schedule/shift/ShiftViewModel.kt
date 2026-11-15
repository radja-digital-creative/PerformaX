package id.biz.yuka.performax.ui.screen.schedule.shift

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.ShiftEntity
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    var stores by mutableStateOf<List<StoreEntity>>(emptyList())
    var shifts by mutableStateOf<List<ShiftEntity>>(emptyList())
    var selectedStoreIndex by mutableIntStateOf(0)

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            isLoading = true
            when (val result = storeRepository.getAllStores()) {
                is ResultState.Success -> {
                    stores = result.data
                    if (stores.isNotEmpty()) {
                        loadShiftsForStore(stores[selectedStoreIndex].id)
                    }
                }
                is ResultState.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun onStoreChanged(index: Int) {
        selectedStoreIndex = index
        if (stores.isNotEmpty()) {
            loadShiftsForStore(stores[index].id)
        }
    }

    fun loadShiftsForStore(storeId: String) {
        viewModelScope.launch {
            isLoading = true
            when (val result = scheduleRepository.getShifts(storeId)) {
                is ResultState.Success -> shifts = result.data
                is ResultState.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }
}