package com.recall.app.ai.processor

import com.recall.app.ai.client.*
import com.recall.app.ai.prompts.AiPrompts
import com.recall.app.core.util.Constants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteProcessor @Inject constructor(
    private val llmClient: LlmClient?,
    private val embeddingsClient: EmbeddingsClient?,
    private val transcriptionClient: TranscriptionClient?
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun processNote(
        rawText: String?,
        transcript: String?,
        ocrText: String?
    ): Result<ProcessedNoteData> {
        try {
            // Combine all text sources
            val combinedText = combineText(rawText, transcript, ocrText)

            if (combinedText.isBlank()) {
                return Result.failure(Exception("No text content to process"))
            }

            // Generate LLM classification
            val aiResult = if (llmClient != null) {
                generateAiMetadata(combinedText)
            } else {
                // Fallback if no LLM client configured
                AiProcessingResult(
                    summary = combinedText.take(Constants.SUMMARY_MAX_LENGTH),
                    type = "other",
                    topics = emptyList(),
                    entities = emptyList(),
                    actionItems = emptyList()
                )
            }

            // Generate embeddings
            val embedding = if (embeddingsClient != null && aiResult != null) {
                val embeddingText = AiPrompts.createEmbeddingText(
                    rawText, transcript, ocrText, aiResult.summary
                )
                embeddingsClient.generateEmbedding(embeddingText).getOrNull()
            } else {
                null
            }

            return Result.success(
                ProcessedNoteData(
                    summary = aiResult.summary,
                    type = aiResult.type,
                    topics = aiResult.topics.take(Constants.MAX_TOPICS),
                    entities = aiResult.entities.take(Constants.MAX_ENTITIES),
                    actionItems = aiResult.actionItems,
                    embedding = embedding
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to process note")
            return Result.failure(e)
        }
    }

    private suspend fun generateAiMetadata(content: String): AiProcessingResult {
        val userPrompt = AiPrompts.createUserPrompt(content)

        val response = llmClient!!.generateCompletion(
            systemPrompt = AiPrompts.SYSTEM_PROMPT,
            userPrompt = userPrompt,
            temperature = 0.3f,
            maxTokens = 500
        ).getOrThrow()

        // Parse JSON response
        val parsed = json.decodeFromString<AiResponseDto>(response)

        return AiProcessingResult(
            summary = parsed.summary.take(Constants.SUMMARY_MAX_LENGTH),
            type = parsed.type,
            topics = parsed.topics.distinct().take(Constants.MAX_TOPICS),
            entities = parsed.entities.distinct().take(Constants.MAX_ENTITIES),
            actionItems = parsed.actionItems.map {
                AiActionItem(text = it.text, dueHint = it.dueHint)
            }
        )
    }

    private fun combineText(rawText: String?, transcript: String?, ocrText: String?): String {
        return listOfNotNull(rawText, transcript, ocrText)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }
}

data class ProcessedNoteData(
    val summary: String,
    val type: String,
    val topics: List<String>,
    val entities: List<String>,
    val actionItems: List<AiActionItem>,
    val embedding: List<Float>?
)

@Serializable
private data class AiResponseDto(
    @SerialName("summary") val summary: String,
    @SerialName("type") val type: String,
    @SerialName("topics") val topics: List<String> = emptyList(),
    @SerialName("entities") val entities: List<String> = emptyList(),
    @SerialName("action_items") val actionItems: List<ActionItemDto> = emptyList()
)

@Serializable
private data class ActionItemDto(
    @SerialName("text") val text: String,
    @SerialName("due_hint") val dueHint: String? = null
)
