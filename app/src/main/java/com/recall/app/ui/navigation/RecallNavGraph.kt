package com.recall.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recall.app.ui.screens.capture.CaptureScreen
import com.recall.app.ui.screens.dailyrecall.DailyRecallScreen
import com.recall.app.ui.screens.home.HomeScreen
import com.recall.app.ui.screens.notedetail.NoteDetailScreen
import com.recall.app.ui.screens.search.SearchScreen
import com.recall.app.ui.screens.settings.SettingsScreen

@Composable
fun RecallNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    sharedText: String? = null,
    sharedImageUris: List<Uri> = emptyList()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onNavigateToCapture = {
                    navController.navigate(Screen.Capture.BASE_ROUTE)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToDailyRecall = {
                    navController.navigate(Screen.DailyRecall.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Capture.BASE_ROUTE,
        ) {
            CaptureScreen(
                onNavigateBack = { navController.popBackStack() },
                initialText = sharedText,
                initialImageUris = sharedImageUris
            )
        }

        composable(Screen.DailyRecall.route) {
            DailyRecallScreen(
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
