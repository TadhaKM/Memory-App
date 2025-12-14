package com.recall.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["created_at"]),
        Index(value = ["sync_state"]),
        Index(value = ["ai_state"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "raw_text")
    val rawText: String?,

    @ColumnInfo(name = "source")
    val source: String, // MANUAL, SHARE, VOICE, CAMERA, IMPORT

    @ColumnInfo(name = "archived")
    val archived: Boolean = false,

    @ColumnInfo(name = "pinned")
    val pinned: Boolean = false,

    @ColumnInfo(name = "importance")
    val importance: Int = 0,

    @ColumnInfo(name = "sync_state")
    val syncState: Int = 0, // 0=LOCAL_ONLY, 1=SYNCED, 2=DIRTY, 3=DELETED

    @ColumnInfo(name = "ai_state")
    val aiState: Int = 0, // 0=NONE, 1=PENDING, 2=DONE, 3=FAILED

    @ColumnInfo(name = "last_error")
    val lastError: String? = null
)
