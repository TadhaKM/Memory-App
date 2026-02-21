package com.recall.app.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: String) = "note_detail/$noteId"
    }
    object Capture : Screen("capture?sharedText={sharedText}") {
        const val BASE_ROUTE = "capture"

        fun createRoute(sharedText: String? = null): String {
            return if (sharedText != null) {
                val encoded = URLEncoder.encode(sharedText, StandardCharsets.UTF_8.toString())
                "capture?sharedText=$encoded"
            } else {
                BASE_ROUTE
            }
        }
    }
    object DailyRecall : Screen("daily_recall")
    object Search : Screen("search")
    object Settings : Screen("settings")
}
