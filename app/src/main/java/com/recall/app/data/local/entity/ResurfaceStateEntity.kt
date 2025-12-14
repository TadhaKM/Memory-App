package com.recall.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "resurface_state",
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
        Index(value = ["score"])
    ]
)
data class ResurfaceStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "note_id")
    val noteId: String,

    @ColumnInfo(name = "score")
    val score: Double = 0.0,

    @ColumnInfo(name = "last_shown_at")
    val lastShownAt: Long? = null,

    @ColumnInfo(name = "never_resurface")
    val neverResurface: Boolean = false
)
