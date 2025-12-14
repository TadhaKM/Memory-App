package com.recall.app.domain.model

/**
 * Domain model for an Attachment
 */
data class Attachment(
    val id: String,
    val noteId: String,
    val type: AttachmentType,
    val mimeType: String,
    val localUri: String,
    val durationMs: Long? = null,
    val sizeBytes: Long,
    val storagePath: String? = null,
    val syncState: SyncState = SyncState.LOCAL_ONLY
)

enum class AttachmentType {
    AUDIO,
    IMAGE
}
