package com.recall.app.ai.client

/**
 * Abstraction for LLM providers (OpenAI, Anthropic, etc.)
 * Allows easy swapping of providers
 */
interface LlmClient {
    suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 1000
    ): Result<String>
}

data class AiProcessingResult(
    val summary: String,
    val type: String,
    val topics: List<String>,
    val entities: List<String>,
    val actionItems: List<AiActionItem>
)

data class AiActionItem(
    val text: String,
    val dueHint: String?
)
