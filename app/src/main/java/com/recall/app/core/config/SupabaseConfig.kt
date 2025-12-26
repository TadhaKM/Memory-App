package com.recall.app.core.config

object SupabaseConfig {
    // These should be loaded from BuildConfig or local.properties in production
    const val SUPABASE_URL = "https://kwejbghommhlhncpiebs.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt3ZWpiZ2hvbW1obGhuY3BpZWJzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU3NDE4OTIsImV4cCI6MjA4MTMxNzg5Mn0.4baUCEVO9durojouG0U1Z4T2MvWoVND6LYqLVSkb644"

    // Storage bucket name
    const val ATTACHMENTS_BUCKET = "recall-attachments"

    // Table names
    const val NOTES_TABLE = "notes"
    const val ATTACHMENTS_TABLE = "attachments"
    const val AI_METADATA_TABLE = "ai_metadata"
    const val RESURFACE_TABLE = "resurface"
}
