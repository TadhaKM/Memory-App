package com.recall.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.recall.app.core.share.ShareHandler
import com.recall.app.data.preferences.UserPreferencesRepository
import com.recall.app.ui.navigation.RecallNavGraph
import com.recall.app.ui.navigation.Screen
import com.recall.app.ui.theme.RecallTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse incoming share intent
        val sharedContent = ShareHandler.parseIntent(intent)

        setContent {
            var startDestination by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()

                startDestination = when {
                    // If shared content, go directly to capture
                    sharedContent?.hasContent == true -> Screen.Capture.BASE_ROUTE
                    // If first time, show onboarding
                    !prefs.hasSeenOnboarding -> Screen.Onboarding.route
                    // Otherwise go to home
                    else -> Screen.Home.route
                }

                // Mark onboarding as seen when navigating away from it
                if (!prefs.hasSeenOnboarding && startDestination != Screen.Onboarding.route) {
                    userPreferencesRepository.updateHasSeenOnboarding(true)
                }

                isLoading = false

                Timber.d("Starting with destination: $startDestination, shared content: $sharedContent")
            }

            RecallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isLoading && startDestination != null) {
                        RecallNavGraph(
                            startDestination = startDestination!!,
                            sharedText = sharedContent?.text,
                            sharedImageUris = sharedContent?.imageUris ?: emptyList(),
                            onOnboardingComplete = {
                                lifecycleScope.launch {
                                    userPreferencesRepository.updateHasSeenOnboarding(true)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle new share intents when activity is already running
        val sharedContent = ShareHandler.parseIntent(intent)
        if (sharedContent?.hasContent == true) {
            Timber.d("Received new shared content: $sharedContent")
            // Recreate the activity to handle the new shared content
            recreate()
        }
    }
}
