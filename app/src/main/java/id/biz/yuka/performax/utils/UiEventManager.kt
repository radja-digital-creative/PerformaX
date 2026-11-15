package id.biz.yuka.performax.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
}

@Singleton
class UiEventManager @Inject constructor() {
    private val _event = MutableSharedFlow<UiEvent>()
    val event = _event.asSharedFlow()

    suspend fun emitEvent(event: UiEvent) {
        _event.emit(event)
    }

    suspend fun showError(message: String) {
        _event.emit(UiEvent.ShowError(message))
    }
}
