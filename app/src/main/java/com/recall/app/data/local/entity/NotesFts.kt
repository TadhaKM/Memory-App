package com.recall.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search table for notes
 * This table mirrors the notes table for fast text searching
 */
@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class NotesFts(
    @ColumnInfo(name = "raw_text")
    val rawText: String?
)
