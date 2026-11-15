package id.biz.yuka.performax.ui.screen.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val storeRepository: StoreRepository
) : ViewModel() {

    var stores by mutableStateOf<List<StoreEntity>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            when (val result = storeRepository.getAllStores()) {
                is ResultState.Success -> stores = result.data
                is ResultState.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }
}