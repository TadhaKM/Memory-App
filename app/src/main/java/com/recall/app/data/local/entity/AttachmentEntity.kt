package com.recall.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_id"]),
        Index(value = ["sync_state"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "note_id")
    val noteId: String,

    @ColumnInfo(name = "type")
    val type: String, // AUDIO, IMAGE

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "local_uri")
    val localUri: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long? = null,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "storage_path")
    val storagePath: String? = null,

    @ColumnInfo(name = "sync_state")
    val syncState: Int = 0 // 0=LOCAL_ONLY, 1=SYNCED, 2=FAILED
)
