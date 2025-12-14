package com.recall.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recall.app.ui.screens.home.HomeScreen
import com.recall.app.ui.screens.notedetail.NoteDetailScreen

@Composable
fun RecallNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onNavigateToCapture = {
                    navController.navigate(Screen.Capture.route)
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

        composable(Screen.Capture.route) {
            // CaptureScreen will be implemented
        }

        composable(Screen.DailyRecall.route) {
            // DailyRecallScreen will be implemented
        }

        composable(Screen.Search.route) {
            // SearchScreen will be implemented
        }

        composable(Screen.Settings.route) {
            // SettingsScreen will be implemented
        }
    }
}
