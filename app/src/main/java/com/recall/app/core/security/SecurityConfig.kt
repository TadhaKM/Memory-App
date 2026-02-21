package com.recall.app.core.security

import com.recall.app.BuildConfig
import timber.log.Timber

/**
 * SECURITY: Centralized security configuration
 *
 * All API keys and sensitive configuration are loaded from BuildConfig,
 * which reads from local.properties (gitignored) at build time.
 *
 * OWASP Reference: A02:2021 - Cryptographic Failures
 * - Never hardcode secrets in source code
 * - Use environment variables or secure vaults
 * - Rotate keys regularly
 */
object SecurityConfig {

    // ==========================================================================
    // Supabase Configuration
    // ==========================================================================
    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.takeIf { it.isNotBlank() }
            ?: run {
                Timber.w("SECURITY: SUPABASE_URL not configured")
                ""
            }

    val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.takeIf { it.isNotBlank() }
            ?: run {
                Timber.w("SECURITY: SUPABASE_ANON_KEY not configured")
                ""
            }

    // ==========================================================================
    // Anthropic Configuration
    // ==========================================================================
    val anthropicApiKey: String
        get() = BuildConfig.ANTHROPIC_API_KEY.takeIf { it.isNotBlank() }
            ?: run {
                Timber.d("Anthropic API key not configured - AI features disabled")
                ""
            }

    // ==========================================================================
    // Analytics Configuration
    // ==========================================================================
    val posthogApiKey: String
        get() = BuildConfig.POSTHOG_API_KEY

    val sentryDsn: String
        get() = BuildConfig.SENTRY_DSN

    // ==========================================================================
    // Configuration Validation
    // ==========================================================================

    fun isSupabaseConfigured(): Boolean =
        supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    fun isAnthropicConfigured(): Boolean =
        anthropicApiKey.isNotBlank()

    fun isAnalyticsConfigured(): Boolean =
        posthogApiKey.isNotBlank()

    fun isSentryConfigured(): Boolean =
        sentryDsn.isNotBlank()

    /**
     * Log configuration status (without exposing actual keys)
     * Call this during app initialization for debugging
     */
    fun logConfigurationStatus() {
        Timber.d("Security Configuration Status:")
        Timber.d("  - Supabase: ${if (isSupabaseConfigured()) "CONFIGURED" else "NOT CONFIGURED"}")
        Timber.d("  - Anthropic AI: ${if (isAnthropicConfigured()) "CONFIGURED" else "NOT CONFIGURED"}")
        Timber.d("  - PostHog Analytics: ${if (isAnalyticsConfigured()) "CONFIGURED" else "NOT CONFIGURED"}")
        Timber.d("  - Sentry Crash Reporting: ${if (isSentryConfigured()) "CONFIGURED" else "NOT CONFIGURED"}")
    }

    /**
     * Mask sensitive strings for logging (show first 4 and last 4 chars only)
     */
    fun maskSecret(secret: String): String {
        if (secret.length <= 12) return "****"
        return "${secret.take(4)}...${secret.takeLast(4)}"
    }
}
