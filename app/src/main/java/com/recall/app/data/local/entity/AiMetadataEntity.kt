package com.recall.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_metadata",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_id"])
    ]
)
data class AiMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "note_id")
    val noteId: String,

    @ColumnInfo(name = "summary")
    val summary: String? = null,

    @ColumnInfo(name = "type")
    val type: String? = null, // TASK, IDEA, REFERENCE, JOURNAL, QUESTION, QUOTE, OTHER

    @ColumnInfo(name = "topics_json")
    val topicsJson: String? = null, // JSON array of strings

    @ColumnInfo(name = "entities_json")
    val entitiesJson: String? = null, // JSON array of strings

    @ColumnInfo(name = "action_items_json")
    val actionItemsJson: String? = null, // JSON array of {text, done, dueHint}

    @ColumnInfo(name = "transcript")
    val transcript: String? = null,

    @ColumnInfo(name = "ocr_text")
    val ocrText: String? = null,

    @ColumnInfo(name = "embedding_json")
    val embeddingJson: String? = null, // JSON array of floats

    @ColumnInfo(name = "processed_at")
    val processedAt: Long? = null
)
