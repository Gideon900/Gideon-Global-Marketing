package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.credentials.CredentialManager
import com.example.ui.AppNavigation
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.theme.GideonGlobalTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GideonGlobalTheme {
                LaunchedEffect(Unit) {
                    // Wait until the first frame has drawn and committed its SurfaceSyncGroup
                    // before invoking Credential Manager IPC so WindowManager sync never stalls.
                    withFrameNanos { }
                    delay(350L)
                    val credentialManager = CredentialManager.create(this@MainActivity)
                    attemptAutoSignIn(
                        context = this@MainActivity,
                        credentialManager = credentialManager,
                        onAuthSuccess = {},
                        onUnauthenticated = {},
                        scope = this
                    )
                }
                AppNavigation()
            }
        }
    }
}
