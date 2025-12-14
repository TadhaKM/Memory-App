package com.recall.app.domain.model

/**
 * Domain model for a Note
 */
data class Note(
    val id: String,
    val userId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rawText: String?,
    val source: NoteSource,
    val archived: Boolean = false,
    val pinned: Boolean = false,
    val importance: Int = 0,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val aiState: AiState = AiState.NONE,
    val lastError: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val aiMetadata: AiMetadata? = null
)

enum class NoteSource {
    MANUAL,
    SHARE,
    VOICE,
    CAMERA,
    IMPORT
}

enum class SyncState {
    LOCAL_ONLY,
    SYNCED,
    DIRTY,
    DELETED
}

enum class AiState {
    NONE,
    PENDING,
    DONE,
    FAILED
}
