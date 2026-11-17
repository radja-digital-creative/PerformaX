package id.biz.yuka.performax.ui.session

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.performax.data.SessionManager
import id.biz.yuka.performax.service.LiveLocationTracking
import id.biz.yuka.performax.service.LocationWorker
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

sealed interface AccountAccessState {
    data object Checking : AccountAccessState
    data object SignedOut : AccountAccessState
    data object Allowed : AccountAccessState
    data object NetworkError : AccountAccessState
}

/** Holds all authenticated navigation behind the server's current account decision. */
@HiltViewModel
class SessionAccessViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableState = MutableStateFlow<AccountAccessState>(AccountAccessState.Checking)
    val state = mutableState.asStateFlow()
    private val foreground = MutableStateFlow(false)
    private val retryVersion = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(SupabaseConfig.client.auth.sessionStatus, sessionManager.isLoggedIn,
                foreground, retryVersion) { auth, loggedIn, visible, _ -> Triple(auth, loggedIn, visible) }
                .collectLatest { (auth, loggedIn, visible) ->
                    if (auth is SessionStatus.LoadingFromStorage) {
                        mutableState.value = AccountAccessState.Checking
                        return@collectLatest
                    }
                    val userId = authRepository.getCurrentUserId()
                    if (userId == null || !loggedIn) {
                        mutableState.value = AccountAccessState.SignedOut
                        stopLocation()
                        if (userId == null && loggedIn) sessionManager.clearSession()
                        return@collectLatest
                    }
                    mutableState.value = AccountAccessState.Checking
                    if (!visible) return@collectLatest
                    while (true) {
                        try {
                            val active = withTimeout(15_000) { authRepository.isCurrentAccountActive() }
                            // A response for an old identity can never authorize a new session.
                            if (authRepository.getCurrentUserId() != userId) return@collectLatest
                            if (!active) {
                                mutableState.value = AccountAccessState.Checking
                                stopLocation()
                                authRepository.signOutLocally()
                                sessionManager.clearSession()
                                mutableState.value = AccountAccessState.SignedOut
                                return@collectLatest
                            }
                            mutableState.value = AccountAccessState.Allowed
                            LiveLocationTracking.resumeIfEnabled(context)
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            mutableState.value = AccountAccessState.NetworkError
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Connectivity failure does not mean the account was revoked.
                            mutableState.value = AccountAccessState.NetworkError
                        }
                        delay(30_000)
                    }
                }
        }
    }

    fun setForeground(value: Boolean) {
        if (!value && mutableState.value != AccountAccessState.SignedOut) {
            mutableState.value = AccountAccessState.Checking
        }
        foreground.value = value
    }

    fun retry() { retryVersion.value += 1 }

    private fun stopLocation() {
        LiveLocationTracking.stopForLogout(context)
        WorkManager.getInstance(context).cancelUniqueWork(LocationWorker.PERIODIC_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork("location_on_demand")
    }
}

@Composable
fun SessionAccessGate(
    viewModel: SessionAccessViewModel = hiltViewModel(),
    content: @Composable (signedIn: Boolean) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setForeground(true)
                Lifecycle.Event.ON_STOP -> viewModel.setForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.setForeground(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state by viewModel.state.collectAsState()
    val navigationState = rememberSaveableStateHolder()
    LaunchedEffect(state) {
        if (state == AccountAccessState.SignedOut) navigationState.removeState("authenticated")
        if (state == AccountAccessState.Allowed) navigationState.removeState("login")
    }
    when (state) {
        AccountAccessState.Allowed -> navigationState.SaveableStateProvider("authenticated") { content(true) }
        AccountAccessState.SignedOut -> navigationState.SaveableStateProvider("login") { content(false) }
        else -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            if (state == AccountAccessState.NetworkError) {
                Text("Tidak dapat memverifikasi akses akun. Periksa koneksi lalu coba lagi.")
                Button(onClick = viewModel::retry) { Text("Coba lagi") }
            } else {
                CircularProgressIndicator()
                Text("Memverifikasi akses akun…")
            }
        }
    }
}
