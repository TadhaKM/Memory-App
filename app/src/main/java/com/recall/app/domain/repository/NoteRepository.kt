package com.recall.app.domain.repository

import com.recall.app.core.util.Result
import com.recall.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    // Note CRUD
    fun getAllNotes(archived: Boolean = false): Flow<List<Note>>
    fun getNoteById(noteId: String): Flow<Note?>
    suspend fun createNote(note: Note): Result<Unit>
    suspend fun updateNote(note: Note): Result<Unit>
    suspend fun deleteNote(noteId: String): Result<Unit>

    // Note actions
    suspend fun archiveNote(noteId: String, archived: Boolean): Result<Unit>
    suspend fun pinNote(noteId: String, pinned: Boolean): Result<Unit>
    suspend fun updateImportance(noteId: String, importance: Int): Result<Unit>

    // Attachments
    suspend fun addAttachment(attachment: Attachment): Result<Unit>
    suspend fun getAttachmentsByNoteId(noteId: String): List<Attachment>
    suspend fun deleteAttachment(attachmentId: String): Result<Unit>

    // AI Metadata
    suspend fun getAiMetadata(noteId: String): AiMetadata?
    suspend fun updateAiMetadata(metadata: AiMetadata): Result<Unit>

    // Search
    fun searchNotes(query: String, archived: Boolean = false): Flow<List<Note>>
    fun searchNotesByType(query: String, type: NoteType, archived: Boolean = false): Flow<List<Note>>

    // Resurfacing & Memory Decay
    suspend fun getTopResurfaceNotes(limit: Int): List<Note>
    suspend fun updateResurfaceScore(noteId: String, score: Double): Result<Unit>
    suspend fun updateNeverResurface(noteId: String, never: Boolean): Result<Unit>
    suspend fun markResurfaceShown(noteId: String): Result<Unit>

    /**
     * Track user interaction with a note (open/edit)
     * This fights memory decay - interacted notes stay fresh
     */
    suspend fun trackNoteInteraction(noteId: String): Result<Unit>

    // Sync
    suspend fun getNotesPendingSync(): List<Note>
    suspend fun getAttachmentsPendingSync(): List<Attachment>
}
