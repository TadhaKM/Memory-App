package com.recall.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.recall.app.core.share.ShareHandler
import com.recall.app.ui.navigation.RecallNavGraph
import com.recall.app.ui.navigation.Screen
import com.recall.app.ui.theme.RecallTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse incoming share intent
        val sharedContent = ShareHandler.parseIntent(intent)

        // Determine start destination based on shared content
        val startDestination = if (sharedContent?.hasContent == true) {
            Screen.Capture.BASE_ROUTE
        } else {
            Screen.Home.route
        }

        Timber.d("Starting with destination: $startDestination, shared content: $sharedContent")

        setContent {
            RecallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecallNavGraph(
                        startDestination = startDestination,
                        sharedText = sharedContent?.text,
                        sharedImageUris = sharedContent?.imageUris ?: emptyList()
                    )
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
