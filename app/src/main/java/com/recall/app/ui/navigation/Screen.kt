package com.recall.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: String) = "note_detail/$noteId"
    }
    object Capture : Screen("capture")
    object DailyRecall : Screen("daily_recall")
    object Search : Screen("search")
    object Settings : Screen("settings")
}
