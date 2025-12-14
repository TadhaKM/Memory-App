package com.recall.app.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.recall.app.data.local.dao.NoteDao
import com.recall.app.data.local.dao.ResurfaceStateDao
import com.recall.app.data.local.dao.AttachmentDao
import com.recall.app.data.local.dao.AiMetadataDao
import com.recall.app.data.mapper.toDomain
import com.recall.app.domain.usecase.CalculateResurfaceScoreUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ResurfaceScoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteDao: NoteDao,
    private val resurfaceStateDao: ResurfaceStateDao,
    private val attachmentDao: AttachmentDao,
    private val aiMetadataDao: AiMetadataDao,
    private val calculateResurfaceScore: CalculateResurfaceScoreUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting resurface score calculation...")

            // Get all non-deleted, non-archived notes
            val notes = noteDao.getAllNotes(archived = false)
                .let { flow ->
                    val notesList = mutableListOf<com.recall.app.domain.model.Note>()
                    flow.collect { noteEntities ->
                        notesList.addAll(noteEntities.map { noteEntity ->
                            val attachments = attachmentDao.getAttachmentsByNoteId(noteEntity.id)
                                .map { it.toDomain() }
                            val aiMetadata = aiMetadataDao.getAiMetadataByNoteId(noteEntity.id)?.toDomain()
                            noteEntity.toDomain(attachments, aiMetadata)
                        })
                    }
                    notesList
                }

            Timber.d("Calculating scores for ${notes.size} notes")

            for (note in notes) {
                try {
                    // Get current resurface state
                    val currentState = resurfaceStateDao.getResurfaceStateByNoteId(note.id)

                    // Skip if never resurface is set
                    if (currentState?.neverResurface == true) {
                        Timber.d("Skipping note ${note.id}: never_resurface = true")
                        continue
                    }

                    // Calculate new score
                    val newScore = calculateResurfaceScore(
                        note = note,
                        currentResurfaceScore = currentState?.score ?: 0.0,
                        lastShownAt = currentState?.lastShownAt
                    )

                    // Update score
                    resurfaceStateDao.updateScore(note.id, newScore)
                    Timber.d("Updated score for note ${note.id}: $newScore")

                } catch (e: Exception) {
                    Timber.e(e, "Error calculating score for note ${note.id}")
                    // Continue with other notes
                }
            }

            Timber.d("Resurface score calculation completed")
            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "Resurface score worker failed")
            Result.retry()
        }
    }
}
