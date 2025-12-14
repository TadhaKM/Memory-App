package com.recall.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("raw_text") val rawText: String? = null,
    @SerialName("source") val source: String,
    @SerialName("archived") val archived: Boolean = false,
    @SerialName("pinned") val pinned: Boolean = false,
    @SerialName("importance") val importance: Int = 0,
    @SerialName("ai_status") val aiStatus: String = "pending",
    @SerialName("deleted") val deleted: Boolean = false
)

@Serializable
data class AttachmentDto(
    @SerialName("id") val id: String,
    @SerialName("note_id") val noteId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("type") val type: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("size_bytes") val sizeBytes: Long
)

@Serializable
data class AiMetadataDto(
    @SerialName("note_id") val noteId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("summary") val summary: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("topics") val topics: List<String>? = null,
    @SerialName("entities") val entities: List<String>? = null,
    @SerialName("action_items") val actionItems: List<ActionItemDto>? = null,
    @SerialName("transcript") val transcript: String? = null,
    @SerialName("ocr_text") val ocrText: String? = null,
    @SerialName("embedding") val embedding: List<Float>? = null,
    @SerialName("processed_at") val processedAt: Long? = null
)

@Serializable
data class ActionItemDto(
    @SerialName("text") val text: String,
    @SerialName("done") val done: Boolean = false,
    @SerialName("due_hint") val dueHint: String? = null
)

@Serializable
data class ResurfaceDto(
    @SerialName("note_id") val noteId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("score") val score: Double = 0.0,
    @SerialName("last_shown_at") val lastShownAt: Long? = null,
    @SerialName("never_resurface") val neverResurface: Boolean = false
)
