package com.recall.app.core.config

object SupabaseConfig {
    // These should be loaded from BuildConfig or local.properties in production
    const val SUPABASE_URL = "https://placeholder.supabase.co"
    const val SUPABASE_ANON_KEY = "placeholder_anon_key"

    // Storage bucket name
    const val ATTACHMENTS_BUCKET = "recall-attachments"

    // Table names
    const val NOTES_TABLE = "notes"
    const val ATTACHMENTS_TABLE = "attachments"
    const val AI_METADATA_TABLE = "ai_metadata"
    const val RESURFACE_TABLE = "resurface"
}
