package id.biz.yuka.performax.ui.screen.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.data.SessionManager
import id.biz.yuka.performax.utils.UiEventManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val uiEventManager: UiEventManager
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val _loginSuccess = MutableSharedFlow<Unit>()
    val loginSuccess = _loginSuccess.asSharedFlow()

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email dan password tidak boleh kosong"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val result = authRepository.login(LoginRequest(email, password))
            
            when (result) {
                is ResultState.Success -> {
                    // This login VM can outlive the screen; never retain credentials after success.
                    password = ""
                    email = ""
                    sessionManager.saveSession(result.data.email)
                    _loginSuccess.emit(Unit)
                }
                is ResultState.Error -> {
                    uiEventManager.showError(result.message)
                }
            }
            isLoading = false
        }
    }
}
