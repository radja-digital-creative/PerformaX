package id.biz.yuka.performax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import id.biz.yuka.performax.ui.components.GlobalErrorPopup
import id.biz.yuka.performax.ui.navigation.AppNav
import id.biz.yuka.performax.ui.theme.PerformaXTheme
import id.biz.yuka.performax.utils.UiEvent
import id.biz.yuka.performax.utils.UiEventManager
import javax.inject.Inject
import android.content.Intent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var uiEventManager: UiEventManager

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerformaXTheme {
                var globalErrorMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    uiEventManager.event.collect { event ->
                        when (event) {
                            is UiEvent.ShowError -> {
                                globalErrorMessage = event.message
                            }
                        }
                    }
                }

                Box {
                    id.biz.yuka.performax.ui.session.SessionAccessGate { signedIn ->
                        if (signedIn) {
                            val navController = rememberNavController()
                            AppNav(navController = navController)
                        } else {
                            // SessionAccessGate observes saveSession and verifies before mounting app routes.
                            id.biz.yuka.performax.ui.screen.login.LoginScreen(onLoginSuccess = {})
                        }
                    }
                    
                    GlobalErrorPopup(
                        errorMessage = globalErrorMessage,
                        onDismiss = { globalErrorMessage = null }
                    )
                }
            }
        }
    }
}
