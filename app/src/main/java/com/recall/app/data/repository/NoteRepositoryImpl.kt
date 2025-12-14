package com.recall.app.data.repository

import com.recall.app.core.util.Result
import com.recall.app.data.local.dao.*
import com.recall.app.data.local.entity.ResurfaceStateEntity
import com.recall.app.data.mapper.*
import com.recall.app.domain.model.*
import com.recall.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val attachmentDao: AttachmentDao,
    private val aiMetadataDao: AiMetadataDao,
    private val resurfaceStateDao: ResurfaceStateDao,
    private val searchDao: SearchDao
) : NoteRepository {

    override fun getAllNotes(archived: Boolean): Flow<List<Note>> {
        return noteDao.getAllNotes(archived).map { noteEntities ->
            noteEntities.map { noteEntity ->
                val attachments = attachmentDao.getAttachmentsByNoteId(noteEntity.id)
                    .map { it.toDomain() }
                val aiMetadata = aiMetadataDao.getAiMetadataByNoteId(noteEntity.id)?.toDomain()
                noteEntity.toDomain(attachments, aiMetadata)
            }
        }
    }

    override fun getNoteById(noteId: String): Flow<Note?> {
        return combine(
            noteDao.getNoteByIdFlow(noteId),
            attachmentDao.getAttachmentsByNoteIdFlow(noteId),
            aiMetadataDao.getAiMetadataByNoteIdFlow(noteId)
        ) { noteEntity, attachmentEntities, aiMetadataEntity ->
            noteEntity?.let {
                val attachments = attachmentEntities.map { it.toDomain() }
                val aiMetadata = aiMetadataEntity?.toDomain()
                it.toDomain(attachments, aiMetadata)
            }
        }
    }

    override suspend fun createNote(note: Note): Result<Unit> {
        return try {
            noteDao.insertNote(note.toEntity())

            // Create initial resurface state
            resurfaceStateDao.insertResurfaceState(
                ResurfaceStateEntity(
                    noteId = note.id,
                    score = 0.0,
                    lastShownAt = null,
                    neverResurface = false
                )
            )

            Timber.d("Created note: ${note.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error creating note")
            Result.Error(e)
        }
    }

    override suspend fun updateNote(note: Note): Result<Unit> {
        return try {
            noteDao.updateNote(note.toEntity())
            Timber.d("Updated note: ${note.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating note")
            Result.Error(e)
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            noteDao.deleteNoteById(noteId)
            Timber.d("Deleted note: $noteId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting note")
            Result.Error(e)
        }
    }

    override suspend fun archiveNote(noteId: String, archived: Boolean): Result<Unit> {
        return try {
            noteDao.updateArchived(noteId, archived)
            Timber.d("Archive note $noteId: $archived")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error archiving note")
            Result.Error(e)
        }
    }

    override suspend fun pinNote(noteId: String, pinned: Boolean): Result<Unit> {
        return try {
            noteDao.updatePinned(noteId, pinned)
            Timber.d("Pin note $noteId: $pinned")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error pinning note")
            Result.Error(e)
        }
    }

    override suspend fun updateImportance(noteId: String, importance: Int): Result<Unit> {
        return try {
            noteDao.updateImportance(noteId, importance)
            Timber.d("Update importance for note $noteId: $importance")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating importance")
            Result.Error(e)
        }
    }

    override suspend fun addAttachment(attachment: Attachment): Result<Unit> {
        return try {
            attachmentDao.insertAttachment(attachment.toEntity())
            Timber.d("Added attachment: ${attachment.id} to note: ${attachment.noteId}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error adding attachment")
            Result.Error(e)
        }
    }

    override suspend fun getAttachmentsByNoteId(noteId: String): List<Attachment> {
        return try {
            attachmentDao.getAttachmentsByNoteId(noteId).map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting attachments")
            emptyList()
        }
    }

    override suspend fun deleteAttachment(attachmentId: String): Result<Unit> {
        return try {
            attachmentDao.deleteAttachmentById(attachmentId)
            Timber.d("Deleted attachment: $attachmentId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting attachment")
            Result.Error(e)
        }
    }

    override suspend fun getAiMetadata(noteId: String): AiMetadata? {
        return try {
            aiMetadataDao.getAiMetadataByNoteId(noteId)?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error getting AI metadata")
            null
        }
    }

    override suspend fun updateAiMetadata(metadata: AiMetadata): Result<Unit> {
        return try {
            aiMetadataDao.insertAiMetadata(metadata.toEntity())
            Timber.d("Updated AI metadata for note: ${metadata.noteId}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating AI metadata")
            Result.Error(e)
        }
    }

    override fun searchNotes(query: String, archived: Boolean): Flow<List<Note>> {
        return searchDao.searchNotes(query, archived).map { noteEntities ->
            noteEntities.map { noteEntity ->
                val attachments = attachmentDao.getAttachmentsByNoteId(noteEntity.id)
                    .map { it.toDomain() }
                val aiMetadata = aiMetadataDao.getAiMetadataByNoteId(noteEntity.id)?.toDomain()
                noteEntity.toDomain(attachments, aiMetadata)
            }
        }
    }

    override fun searchNotesByType(
        query: String,
        type: NoteType,
        archived: Boolean
    ): Flow<List<Note>> {
        return searchDao.searchNotesByType(query, type.name, archived).map { noteEntities ->
            noteEntities.map { noteEntity ->
                val attachments = attachmentDao.getAttachmentsByNoteId(noteEntity.id)
                    .map { it.toDomain() }
                val aiMetadata = aiMetadataDao.getAiMetadataByNoteId(noteEntity.id)?.toDomain()
                noteEntity.toDomain(attachments, aiMetadata)
            }
        }
    }

    override suspend fun getTopResurfaceNotes(limit: Int): List<Note> {
        return try {
            val resurfaceStates = resurfaceStateDao.getTopResurfaceNotes(limit)
            resurfaceStates.mapNotNull { state ->
                val noteEntity = noteDao.getNoteById(state.noteId)
                noteEntity?.let {
                    val attachments = attachmentDao.getAttachmentsByNoteId(it.id)
                        .map { attachment -> attachment.toDomain() }
                    val aiMetadata = aiMetadataDao.getAiMetadataByNoteId(it.id)?.toDomain()
                    it.toDomain(attachments, aiMetadata)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting top resurface notes")
            emptyList()
        }
    }

    override suspend fun updateResurfaceScore(noteId: String, score: Double): Result<Unit> {
        return try {
            resurfaceStateDao.updateScore(noteId, score)
            Timber.d("Updated resurface score for note $noteId: $score")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating resurface score")
            Result.Error(e)
        }
    }

    override suspend fun updateNeverResurface(noteId: String, never: Boolean): Result<Unit> {
        return try {
            resurfaceStateDao.updateNeverResurface(noteId, never)
            Timber.d("Updated never resurface for note $noteId: $never")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating never resurface")
            Result.Error(e)
        }
    }

    override suspend fun markResurfaceShown(noteId: String): Result<Unit> {
        return try {
            resurfaceStateDao.updateLastShown(noteId, System.currentTimeMillis())
            Timber.d("Marked resurface shown for note: $noteId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking resurface shown")
            Result.Error(e)
        }
    }

    override suspend fun getNotesPendingSync(): List<Note> {
        return try {
            // Get notes with sync state LOCAL_ONLY (0) or DIRTY (2)
            val localOnly = noteDao.getNotesBySyncState(0)
            val dirty = noteDao.getNotesBySyncState(2)

            (localOnly + dirty).map { noteEntity ->
                val attachments = attachmentDao.getAttachmentsByNoteId(noteEntity.id)
                    .map { it.toDomain() }
                val aiMetadata = aiMetadataDao.getAiMetadataByNoteId(noteEntity.id)?.toDomain()
                noteEntity.toDomain(attachments, aiMetadata)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting notes pending sync")
            emptyList()
        }
    }

    override suspend fun getAttachmentsPendingSync(): List<Attachment> {
        return try {
            // Get attachments with sync state LOCAL_ONLY (0)
            attachmentDao.getAttachmentsBySyncState(0).map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting attachments pending sync")
            emptyList()
        }
    }
}
