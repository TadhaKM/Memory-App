package com.recall.app.core.config

import com.recall.app.core.security.SecurityConfig

/**
 * SECURITY: Supabase configuration with secure key loading
 *
 * API keys are loaded from BuildConfig (set via local.properties).
 * Never hardcode secrets - they will be exposed in version control
 * and can be extracted from the APK.
 *
 * Setup:
 * 1. Copy local.properties.example to local.properties
 * 2. Add your Supabase credentials
 * 3. Rebuild the app
 */
object SupabaseConfig {
    // ==========================================================================
    // SECURITY: Credentials loaded from BuildConfig (local.properties)
    // ==========================================================================
    val SUPABASE_URL: String
        get() = SecurityConfig.supabaseUrl

    val SUPABASE_ANON_KEY: String
        get() = SecurityConfig.supabaseAnonKey

    // ==========================================================================
    // Static configuration (safe to hardcode)
    // ==========================================================================

    // Storage bucket name
    const val ATTACHMENTS_BUCKET = "recall-attachments"

    // Table names
    const val NOTES_TABLE = "notes"
    const val ATTACHMENTS_TABLE = "attachments"
    const val AI_METADATA_TABLE = "ai_metadata"
    const val RESURFACE_TABLE = "resurface"

    /**
     * Check if Supabase is properly configured
     */
    fun isConfigured(): Boolean = SecurityConfig.isSupabaseConfigured()
}
