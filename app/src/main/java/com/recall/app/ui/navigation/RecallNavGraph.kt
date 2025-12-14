package com.recall.app.ui.navigation

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
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToDailyRecall = {
                    navController.navigate(Screen.DailyRecall.route)
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
            CaptureScreen(
                onNavigateBack = { navController.popBackStack() }
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
            // SettingsScreen will be implemented
        }
    }
}
