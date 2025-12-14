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

    @Query("""
        SELECT * FROM resurface_state
        WHERE never_resurface = 0
        ORDER BY score DESC, last_shown_at ASC
        LIMIT :limit
    """)
    suspend fun getTopResurfaceNotes(limit: Int): List<ResurfaceStateEntity>

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

    @Query("""
        UPDATE resurface_state
        SET last_shown_at = :timestamp
        WHERE note_id = :noteId
    """)
    suspend fun updateLastShown(noteId: String, timestamp: Long)

    @Query("""
        UPDATE resurface_state
        SET never_resurface = :neverResurface
        WHERE note_id = :noteId
    """)
    suspend fun updateNeverResurface(noteId: String, neverResurface: Boolean)

    @Delete
    suspend fun deleteResurfaceState(state: ResurfaceStateEntity)
}
