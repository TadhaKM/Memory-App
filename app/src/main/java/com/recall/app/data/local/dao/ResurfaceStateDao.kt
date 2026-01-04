package com.recall.app.data.local.dao

import androidx.room.*
import com.recall.app.data.local.entity.ResurfaceStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResurfaceStateDao {
    @Query("SELECT * FROM resurface_state WHERE note_id = :noteId")
    suspend fun getResurfaceStateByNoteId(noteId: String): ResurfaceStateEntity?

    @Query("SELECT * FROM resurface_state WHERE note_id = :noteId")
    fun getResurfaceStateByNoteIdFlow(noteId: String): Flow<ResurfaceStateEntity?>

    @Query("SELECT * FROM resurface_state")
    suspend fun getAllResurfaceStates(): List<ResurfaceStateEntity>

    /**
     * Get top notes for Daily Recall
     * EXCLUDES: never_resurface=true AND memory_state=DORMANT
     * Dormant notes have faded from active memory and should only be found via search
     */
    @Query("""
        SELECT * FROM resurface_state
        WHERE never_resurface = 0
          AND memory_state != 'DORMANT'
        ORDER BY score DESC, last_shown_at ASC
        LIMIT :limit
    """)
    suspend fun getTopResurfaceNotes(limit: Int): List<ResurfaceStateEntity>

    /**
     * Get notes by memory state
     */
    @Query("SELECT * FROM resurface_state WHERE memory_state = :state")
    suspend fun getNotesByMemoryState(state: String): List<ResurfaceStateEntity>

    /**
     * Count notes by memory state (for analytics)
     */
    @Query("SELECT COUNT(*) FROM resurface_state WHERE memory_state = :state")
    suspend fun countNotesByMemoryState(state: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResurfaceState(state: ResurfaceStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResurfaceStates(states: List<ResurfaceStateEntity>)

    @Update
    suspend fun updateResurfaceState(state: ResurfaceStateEntity)

    @Query("""
        UPDATE resurface_state
        SET score = :score
        WHERE note_id = :noteId
    """)
    suspend fun updateScore(noteId: String, score: Double)

    /**
     * Update memory strength and derived state
     * This is the core of Adaptive Memory Decay
     */
    @Query("""
        UPDATE resurface_state
        SET score = :score,
            memory_strength = :memoryStrength,
            memory_state = :memoryState
        WHERE note_id = :noteId
    """)
    suspend fun updateMemoryState(
        noteId: String,
        score: Double,
        memoryStrength: Double,
        memoryState: String
    )

    @Query("""
        UPDATE resurface_state
        SET last_shown_at = :timestamp
        WHERE note_id = :noteId
    """)
    suspend fun updateLastShown(noteId: String, timestamp: Long)

    /**
     * Update last interaction time (when user opens/edits note)
     * This fights decay - notes that are interacted with stay fresh
     */
    @Query("""
        UPDATE resurface_state
        SET last_interaction_at = :timestamp
        WHERE note_id = :noteId
    """)
    suspend fun updateLastInteraction(noteId: String, timestamp: Long)

    @Query("""
        UPDATE resurface_state
        SET never_resurface = :neverResurface
        WHERE note_id = :noteId
    """)
    suspend fun updateNeverResurface(noteId: String, neverResurface: Boolean)

    @Delete
    suspend fun deleteResurfaceState(state: ResurfaceStateEntity)
}
