package com.recall.app.domain.model

/**
 * Domain model for AI-generated metadata
 */
data class AiMetadata(
    val noteId: String,
    val summary: String? = null,
    val type: NoteType? = null,
    val topics: List<String> = emptyList(),
    val entities: List<String> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val transcript: String? = null,
    val ocrText: String? = null,
    val embedding: List<Float>? = null,
    val processedAt: Long? = null
)

enum class NoteType {
    TASK,
    IDEA,
    REFERENCE,
    JOURNAL,
    QUESTION,
    QUOTE,
    OTHER
}

data class ActionItem(
    val text: String,
    val done: Boolean = false,
    val dueHint: String? = null
)
