package com.recall.app.data.local.dao

import androidx.room.*
import com.recall.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteByIdFlow(noteId: String): Flow<NoteEntity?>

    @Query("""
        SELECT * FROM notes
        WHERE archived = :archived
        AND sync_state != 3
        ORDER BY pinned DESC, created_at DESC
    """)
    fun getAllNotes(archived: Boolean = false): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE sync_state = :syncState
        ORDER BY created_at DESC
    """)
    suspend fun getNotesBySyncState(syncState: Int): List<NoteEntity>

    @Query("""
        SELECT * FROM notes
        WHERE ai_state = :aiState
        ORDER BY created_at DESC
    """)
    suspend fun getNotesByAiState(aiState: Int): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)

    @Query("""
        UPDATE notes
        SET sync_state = :syncState
        WHERE id = :noteId
    """)
    suspend fun updateSyncState(noteId: String, syncState: Int)

    @Query("""
        UPDATE notes
        SET ai_state = :aiState
        WHERE id = :noteId
    """)
    suspend fun updateAiState(noteId: String, aiState: Int)

    @Query("""
        UPDATE notes
        SET archived = :archived
        WHERE id = :noteId
    """)
    suspend fun updateArchived(noteId: String, archived: Boolean)

    @Query("""
        UPDATE notes
        SET pinned = :pinned
        WHERE id = :noteId
    """)
    suspend fun updatePinned(noteId: String, pinned: Boolean)

    @Query("""
        UPDATE notes
        SET importance = :importance
        WHERE id = :noteId
    """)
    suspend fun updateImportance(noteId: String, importance: Int)
}
