package com.recall.app.core.util

object Constants {
    // Database
    const val DATABASE_NAME = "recall_database"
    const val DATABASE_VERSION = 1

    // DataStore
    const val PREFERENCES_NAME = "recall_preferences"

    // WorkManager
    const val UPLOAD_ATTACHMENTS_WORK = "upload_attachments"
    const val SYNC_NOTES_WORK = "sync_notes"
    const val PROCESS_AI_WORK = "process_ai"
    const val NIGHTLY_RESURFACE_WORK = "nightly_resurface"

    // AI
    const val MAX_TOPICS = 5
    const val MAX_ENTITIES = 8
    const val SUMMARY_MAX_LENGTH = 240

    // Resurfacing
    const val MIN_RESURFACE_INTERVAL_DAYS = 3
    const val IDEA_RESURFACE_THRESHOLD_DAYS = 21
    const val DEFAULT_DAILY_RECALL_COUNT = 5

    // Sync
    const val SYNC_RETRY_MAX_ATTEMPTS = 3
    const val SYNC_BACKOFF_DELAY_MS = 2000L
}
