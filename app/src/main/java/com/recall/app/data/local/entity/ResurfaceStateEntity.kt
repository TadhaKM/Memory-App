package com.recall.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks both resurfacing score and memory strength for each note
 *
 * Key distinction:
 * - score (resurfacing_score): determines WHAT to show in Daily Recall today
 * - memory_strength: determines IF a note has earned persistence over time
 *
 * This separation is the heart of Adaptive Memory Decay.
 */
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
        Index(value = ["score"]),
        Index(value = ["memory_strength"]),
        Index(value = ["memory_state"])
    ]
)
data class ResurfaceStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "note_id")
    val noteId: String,

    /** Resurfacing score - used for Daily Recall ranking */
    @ColumnInfo(name = "score")
    val score: Double = 0.0,

    /**
     * Memory strength - determines note's persistence state
     * memory_strength = resurfacing_score - decay_pressure
     */
    @ColumnInfo(name = "memory_strength", defaultValue = "3.0")
    val memoryStrength: Double = 3.0,

    /**
     * Memory state derived from strength:
     * FRESH (>3.0), CONDENSED (2-3), FADED (1-2), DORMANT (<1)
     */
    @ColumnInfo(name = "memory_state", defaultValue = "FRESH")
    val memoryState: String = "FRESH",

    /** Last time note was shown in Daily Recall */
    @ColumnInfo(name = "last_shown_at")
    val lastShownAt: Long? = null,

    /** Last time user interacted with note (opened/edited) */
    @ColumnInfo(name = "last_interaction_at")
    val lastInteractionAt: Long? = null,

    /** If true, never show in Daily Recall */
    @ColumnInfo(name = "never_resurface")
    val neverResurface: Boolean = false
)
