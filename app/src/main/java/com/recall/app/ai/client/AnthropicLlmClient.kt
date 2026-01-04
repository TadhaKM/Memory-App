package com.recall.app.ai.client

import com.recall.app.ai.config.AnthropicConfig
import com.recall.app.core.security.RateLimitExceededException
import com.recall.app.core.security.RateLimitResult
import com.recall.app.core.security.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SECURITY: Anthropic Claude API client with rate limiting and input validation
 *
 * Security measures implemented:
 * - Rate limiting to prevent API abuse and cost overruns
 * - Input length validation to prevent token exhaustion attacks
 * - Secure API key loading from BuildConfig
 * - No logging of sensitive data (API keys, full responses)
 *
 * OWASP References:
 * - A04:2021 - Insecure Design (rate limiting)
 * - A03:2021 - Injection (input validation)
 */
@Singleton
class AnthropicLlmClient @Inject constructor(
    private val rateLimiter: RateLimiter
) : LlmClient {

    // ==========================================================================
    // SECURITY: Input limits to prevent token exhaustion attacks
    // ==========================================================================
    companion object {
        const val MAX_SYSTEM_PROMPT_LENGTH = 10_000
        const val MAX_USER_PROMPT_LENGTH = 50_000
        const val MAX_TOTAL_INPUT_LENGTH = 60_000
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float,
        maxTokens: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // ==========================================================================
            // SECURITY: Rate limiting check
            // ==========================================================================
            when (val rateLimitResult = rateLimiter.tryAcquire(RateLimiter.Limits.ANTHROPIC_API)) {
                is RateLimitResult.Limited -> {
                    Timber.w("Rate limited: ${rateLimitResult.message}")
                    return@withContext Result.failure(
                        RateLimitExceededException(
                            rateLimitResult.retryAfterMs,
                            rateLimitResult.message
                        )
                    )
                }
                is RateLimitResult.Allowed -> {
                    // Continue with request
                }
            }

            // ==========================================================================
            // SECURITY: Input validation
            // ==========================================================================
            if (systemPrompt.length > MAX_SYSTEM_PROMPT_LENGTH) {
                return@withContext Result.failure(
                    IllegalArgumentException("System prompt exceeds maximum length of $MAX_SYSTEM_PROMPT_LENGTH")
                )
            }

            if (userPrompt.length > MAX_USER_PROMPT_LENGTH) {
                return@withContext Result.failure(
                    IllegalArgumentException("User prompt exceeds maximum length of $MAX_USER_PROMPT_LENGTH")
                )
            }

            val totalLength = systemPrompt.length + userPrompt.length
            if (totalLength > MAX_TOTAL_INPUT_LENGTH) {
                return@withContext Result.failure(
                    IllegalArgumentException("Total input exceeds maximum length of $MAX_TOTAL_INPUT_LENGTH")
                )
            }

            // ==========================================================================
            // Build and send request
            // ==========================================================================
            val requestBody = AnthropicRequest(
                model = AnthropicConfig.MODEL,
                maxTokens = maxTokens,
                system = systemPrompt,
                messages = listOf(
                    Message(role = "user", content = userPrompt)
                ),
                temperature = temperature
            )

            val requestJson = json.encodeToString(requestBody)
            // SECURITY: Don't log full request body (may contain sensitive user data)
            Timber.d("Anthropic request: ${requestJson.length} bytes")

            val request = Request.Builder()
                .url(AnthropicConfig.BASE_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", AnthropicConfig.API_KEY)
                .addHeader("anthropic-version", AnthropicConfig.API_VERSION)
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            // ==========================================================================
            // Handle rate limit responses from server (429)
            // ==========================================================================
            if (response.code == 429) {
                val retryAfter = response.header("retry-after")?.toLongOrNull()?.times(1000) ?: 60_000L
                Timber.w("Server rate limit hit, retry after ${retryAfter}ms")
                return@withContext Result.failure(
                    RateLimitExceededException(retryAfter, "Anthropic API rate limit exceeded")
                )
            }

            if (!response.isSuccessful) {
                // SECURITY: Don't log full error body (may contain sensitive info)
                Timber.e("Anthropic API error: ${response.code}")
                return@withContext Result.failure(
                    Exception("Anthropic API error: ${response.code}")
                )
            }

            if (responseBody == null) {
                return@withContext Result.failure(Exception("Empty response from Anthropic API"))
            }

            // SECURITY: Only log response size, not content
            Timber.d("Anthropic response: ${responseBody.length} bytes")

            val anthropicResponse = json.decodeFromString<AnthropicResponse>(responseBody)
            val textContent = anthropicResponse.content.firstOrNull { it.type == "text" }?.text
                ?: return@withContext Result.failure(Exception("No text content in response"))

            Result.success(textContent)
        } catch (e: RateLimitExceededException) {
            // Re-throw rate limit exceptions as-is
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate completion with Anthropic")
            Result.failure(e)
        }
    }
}

@Serializable
private data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String? = null,
    val messages: List<Message>,
    val temperature: Float = 0.7f
)

@Serializable
private data class Message(
    val role: String,
    val content: String
)

@Serializable
private data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_sequence") val stopSequence: String? = null,
    val usage: Usage? = null
)

@Serializable
private data class ContentBlock(
    val type: String,
    val text: String? = null
)

@Serializable
private data class Usage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)
