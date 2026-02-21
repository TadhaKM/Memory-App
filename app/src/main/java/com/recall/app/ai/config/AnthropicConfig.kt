package com.recall.app.ai.config

import com.recall.app.core.security.SecurityConfig

/**
 * SECURITY: Anthropic Claude API configuration with secure key loading
 *
 * API keys are loaded from BuildConfig (set via local.properties).
 * Never hardcode API keys in source code.
 *
 * Setup:
 * 1. Get an API key from https://console.anthropic.com/
 * 2. Add to local.properties: ANTHROPIC_API_KEY=sk-ant-your-key-here
 * 3. Rebuild the app
 *
 * OWASP Reference: A02:2021 - Cryptographic Failures
 * - API keys must never be committed to version control
 * - Keys should be rotated regularly
 * - Consider using a backend proxy for production apps
 */
object AnthropicConfig {
    // ==========================================================================
    // SECURITY: API key loaded from BuildConfig (local.properties)
    // ==========================================================================
    val API_KEY: String
        get() = SecurityConfig.anthropicApiKey

    // ==========================================================================
    // Static configuration (safe to hardcode)
    // ==========================================================================
    const val BASE_URL = "https://api.anthropic.com/v1/messages"
    const val MODEL = "claude-3-haiku-20240307" // Fast and cost-effective for note processing
    const val API_VERSION = "2023-06-01"

    /**
     * Check if Anthropic API is properly configured
     */
    fun isConfigured(): Boolean = SecurityConfig.isAnthropicConfigured()
}
