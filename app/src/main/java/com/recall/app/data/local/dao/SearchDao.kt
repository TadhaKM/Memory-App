package com.recall.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.recall.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    /**
     * Full-text search using FTS4
     * The MATCH operator searches the notes_fts table
     */
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.archived = :archived
        AND notes.sync_state != 3
        ORDER BY notes.pinned DESC, notes.created_at DESC
        LIMIT :limit
    """)
    fun searchNotes(
        query: String,
        archived: Boolean = false,
        limit: Int = 100
    ): Flow<List<NoteEntity>>

    /**
     * Search with filters for note type
     */
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.rowid = notes_fts.rowid
        JOIN ai_metadata ON notes.id = ai_metadata.note_id
        WHERE notes_fts MATCH :query
        AND ai_metadata.type = :type
        AND notes.archived = :archived
        AND notes.sync_state != 3
        ORDER BY notes.pinned DESC, notes.created_at DESC
        LIMIT :limit
    """)
    fun searchNotesByType(
        query: String,
        type: String,
        archived: Boolean = false,
        limit: Int = 100
    ): Flow<List<NoteEntity>>

    /**
     * Search notes with attachments of specific type
     */
    @Query("""
        SELECT DISTINCT notes.* FROM notes
        JOIN notes_fts ON notes.rowid = notes_fts.rowid
        JOIN attachments ON notes.id = attachments.note_id
        WHERE notes_fts MATCH :query
        AND attachments.type = :attachmentType
        AND notes.archived = :archived
        AND notes.sync_state != 3
        ORDER BY notes.pinned DESC, notes.created_at DESC
        LIMIT :limit
    """)
    fun searchNotesWithAttachmentType(
        query: String,
        attachmentType: String,
        archived: Boolean = false,
        limit: Int = 100
    ): Flow<List<NoteEntity>>
}
