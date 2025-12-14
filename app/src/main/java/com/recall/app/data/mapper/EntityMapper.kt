package com.recall.app.data.mapper

import com.recall.app.data.local.entity.*
import com.recall.app.domain.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Converts Room entities to domain models and vice versa
 */

// Note Entity Mappers
fun NoteEntity.toDomain(
    attachments: List<Attachment> = emptyList(),
    aiMetadata: AiMetadata? = null
): Note {
    return Note(
        id = id,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        rawText = rawText,
        source = NoteSource.valueOf(source),
        archived = archived,
        pinned = pinned,
        importance = importance,
        syncState = SyncState.values()[syncState],
        aiState = AiState.values()[aiState],
        lastError = lastError,
        attachments = attachments,
        aiMetadata = aiMetadata
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        rawText = rawText,
        source = source.name,
        archived = archived,
        pinned = pinned,
        importance = importance,
        syncState = syncState.ordinal,
        aiState = aiState.ordinal,
        lastError = lastError
    )
}

// Attachment Entity Mappers
fun AttachmentEntity.toDomain(): Attachment {
    return Attachment(
        id = id,
        noteId = noteId,
        type = AttachmentType.valueOf(type),
        mimeType = mimeType,
        localUri = localUri,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        storagePath = storagePath,
        syncState = SyncState.values()[syncState]
    )
}

fun Attachment.toEntity(): AttachmentEntity {
    return AttachmentEntity(
        id = id,
        noteId = noteId,
        type = type.name,
        mimeType = mimeType,
        localUri = localUri,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        storagePath = storagePath,
        syncState = syncState.ordinal
    )
}

// AI Metadata Entity Mappers
fun AiMetadataEntity.toDomain(): AiMetadata {
    val json = Json { ignoreUnknownKeys = true }

    return AiMetadata(
        noteId = noteId,
        summary = summary,
        type = type?.let { NoteType.valueOf(it) },
        topics = topicsJson?.let { json.decodeFromString<List<String>>(it) } ?: emptyList(),
        entities = entitiesJson?.let { json.decodeFromString<List<String>>(it) } ?: emptyList(),
        actionItems = actionItemsJson?.let { json.decodeFromString<List<ActionItem>>(it) } ?: emptyList(),
        transcript = transcript,
        ocrText = ocrText,
        embedding = embeddingJson?.let { json.decodeFromString<List<Float>>(it) },
        processedAt = processedAt
    )
}

fun AiMetadata.toEntity(): AiMetadataEntity {
    val json = Json { ignoreUnknownKeys = true }

    return AiMetadataEntity(
        noteId = noteId,
        summary = summary,
        type = type?.name,
        topicsJson = if (topics.isNotEmpty()) json.encodeToString(topics) else null,
        entitiesJson = if (entities.isNotEmpty()) json.encodeToString(entities) else null,
        actionItemsJson = if (actionItems.isNotEmpty()) json.encodeToString(actionItems) else null,
        transcript = transcript,
        ocrText = ocrText,
        embeddingJson = embedding?.let { json.encodeToString(it) },
        processedAt = processedAt
    )
}

// Resurface State Entity Mappers
fun ResurfaceStateEntity.toDomain(): ResurfaceState {
    return ResurfaceState(
        noteId = noteId,
        score = score,
        lastShownAt = lastShownAt,
        neverResurface = neverResurface
    )
}

fun ResurfaceState.toEntity(): ResurfaceStateEntity {
    return ResurfaceStateEntity(
        noteId = noteId,
        score = score,
        lastShownAt = lastShownAt,
        neverResurface = neverResurface
    )
}
