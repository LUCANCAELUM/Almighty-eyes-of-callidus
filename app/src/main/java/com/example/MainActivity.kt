package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.CallidusAiRepository
import com.example.data.firebase.CallidusFirestoreRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.local.CallidusPreferences
import com.example.data.local.CallidusRoomDatabase
import com.example.data.local.LocalTelemetrySqlRepository
import com.example.ui.CallidusViewModel
import com.example.ui.components.CallidusAuthScreen
import com.example.ui.screens.CallidusCommandScreen
import com.example.ui.theme.CallidusTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure App Check BEFORE any ViewModel or Firebase SDK access
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            CallidusTheme {
                CallidusAppRoot()
            }
        }
    }
}

@Composable
private fun CallidusAppRoot() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val appContext = LocalContext.current.applicationContext
    var enteredAsGuest by rememberSaveable { mutableStateOf(false) }

    val showCommandDeck = currentUser != null || enteredAsGuest

    AnimatedContent(
        targetState = showCommandDeck,
        transitionSpec = {
            (scaleIn(
                initialScale = 0.82f,
                animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn()) togetherWith (scaleOut(targetScale = 1.18f) + fadeOut())
        },
        label = "entry_3d_portal_transition"
    ) { isCommandReady ->
        if (!isCommandReady) {
            CallidusAuthScreen(
                onAuthSuccess = { /* AuthStateListener updates currentUser reactively */ },
                onContinueWithoutSignIn = { enteredAsGuest = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val sessionKey = currentUser?.uid ?: "guest"
            key(sessionKey) {
                val firestoreRepo = remember(appContext) { CallidusFirestoreRepository(appContext) }
                val aiRepo = remember { CallidusAiRepository() }
                val sqlRepo = remember(appContext) {
                    LocalTelemetrySqlRepository(CallidusRoomDatabase.getInstance(appContext).telemetrySnapshotDao())
                }
                val preferences = remember(appContext) { CallidusPreferences(appContext) }

                val vm: CallidusViewModel = viewModel(
                    key = sessionKey,
                    factory = viewModelFactory {
                        initializer {
                            CallidusViewModel(
                                currentUserId = currentUser?.uid,
                                userDisplayName = currentUser?.displayName ?: "Guest Operator",
                                userEmail = currentUser?.email,
                                firestoreRepo = firestoreRepo,
                                aiRepo = aiRepo,
                                sqlRepo = sqlRepo,
                                preferences = preferences
                            )
                        }
                    }
                )
                CallidusCommandScreen(
                    viewModel = vm,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
