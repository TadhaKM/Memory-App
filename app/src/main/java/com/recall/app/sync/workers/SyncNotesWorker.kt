package com.recall.app.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.recall.app.core.auth.AuthManager
import com.recall.app.data.local.dao.NoteDao
import com.recall.app.data.remote.NotesDataSource
import com.recall.app.data.remote.model.NoteDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncNotesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteDao: NoteDao,
    private val notesDataSource: NotesDataSource,
    private val authManager: AuthManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!authManager.isAuthenticated()) {
            Timber.w("User not authenticated, skipping note sync")
            return Result.success()
        }

        val userId = authManager.getCurrentUserId() ?: return Result.failure()

        return try {
            // Get notes pending sync (LOCAL_ONLY = 0, DIRTY = 2)
            val localOnlyNotes = noteDao.getNotesBySyncState(0)
            val dirtyNotes = noteDao.getNotesBySyncState(2)
            val pendingNotes = localOnlyNotes + dirtyNotes

            Timber.d("Found ${pendingNotes.size} notes to sync")

            for (noteEntity in pendingNotes) {
                try {
                    val noteDto = NoteDto(
                        id = noteEntity.id,
                        userId = userId,
                        createdAt = noteEntity.createdAt,
                        updatedAt = noteEntity.updatedAt,
                        rawText = noteEntity.rawText,
                        source = noteEntity.source,
                        archived = noteEntity.archived,
                        pinned = noteEntity.pinned,
                        importance = noteEntity.importance,
                        aiStatus = when (noteEntity.aiState) {
                            0 -> "none"
                            1 -> "pending"
                            2 -> "done"
                            3 -> "failed"
                            else -> "pending"
                        },
                        deleted = noteEntity.syncState == 3 // DELETED
                    )

                    val result = notesDataSource.upsertNote(noteDto)

                    if (result.isSuccess) {
                        // Update sync state to SYNCED (1)
                        noteDao.updateSyncState(noteEntity.id, 1)
                        Timber.d("Synced note: ${noteEntity.id}")
                    } else {
                        Timber.e("Failed to sync note: ${noteEntity.id}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing note: ${noteEntity.id}")
                    // Continue with next note
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Sync notes worker failed")
            Result.retry()
        }
    }
}
