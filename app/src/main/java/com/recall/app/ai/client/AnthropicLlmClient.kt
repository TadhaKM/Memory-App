package com.recall.app.ai.client

import com.recall.app.ai.config.AnthropicConfig
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

@Singleton
class AnthropicLlmClient @Inject constructor() : LlmClient {

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
            Timber.d("Anthropic request: $requestJson")

            val request = Request.Builder()
                .url(AnthropicConfig.BASE_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", AnthropicConfig.API_KEY)
                .addHeader("anthropic-version", AnthropicConfig.API_VERSION)
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                Timber.e("Anthropic API error: ${response.code} - $responseBody")
                return@withContext Result.failure(
                    Exception("Anthropic API error: ${response.code} - ${responseBody?.take(200)}")
                )
            }

            if (responseBody == null) {
                return@withContext Result.failure(Exception("Empty response from Anthropic API"))
            }

            Timber.d("Anthropic response: ${responseBody.take(500)}")

            val anthropicResponse = json.decodeFromString<AnthropicResponse>(responseBody)
            val textContent = anthropicResponse.content.firstOrNull { it.type == "text" }?.text
                ?: return@withContext Result.failure(Exception("No text content in response"))

            Result.success(textContent)
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
