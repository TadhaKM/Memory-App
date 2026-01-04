package com.recall.app.ai.config

/**
 * Configuration for Anthropic Claude API
 *
 * To get an API key:
 * 1. Go to https://console.anthropic.com/
 * 2. Create an account or sign in
 * 3. Navigate to API Keys section
 * 4. Create a new API key
 */
object AnthropicConfig {
    // TODO: Move to secure storage (encrypted SharedPreferences or BuildConfig)
    const val API_KEY = "" // Add your Anthropic API key here

    const val BASE_URL = "https://api.anthropic.com/v1/messages"
    const val MODEL = "claude-3-haiku-20240307" // Fast and cost-effective for note processing
    const val API_VERSION = "2023-06-01"

    fun isConfigured(): Boolean = API_KEY.isNotBlank()
}
