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
import com.recall.app.domain.model.MemoryState
import com.recall.app.domain.usecase.CalculateMemoryStrengthUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Nightly worker that calculates memory strength and state for all notes
 *
 * This is the heart of Adaptive Memory Decay:
 * - Calculates resurfacing_score (for Daily Recall ranking)
 * - Calculates memory_strength (resurfacing_score - decay_pressure)
 * - Derives memory_state (Fresh, Condensed, Faded, Dormant)
 *
 * Notes that become DORMANT are excluded from Daily Recall but can still be searched.
 */
@HiltWorker
class ResurfaceScoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteDao: NoteDao,
    private val resurfaceStateDao: ResurfaceStateDao,
    private val attachmentDao: AttachmentDao,
    private val aiMetadataDao: AiMetadataDao,
    private val calculateMemoryStrength: CalculateMemoryStrengthUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting Adaptive Memory Decay calculation...")

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

            Timber.d("Calculating memory strength for ${notes.size} notes")

            // Track state transitions for logging
            var freshCount = 0
            var condensedCount = 0
            var fadedCount = 0
            var dormantCount = 0

            for (note in notes) {
                try {
                    // Get current resurface state
                    val currentState = resurfaceStateDao.getResurfaceStateByNoteId(note.id)

                    // Skip if never resurface is set
                    if (currentState?.neverResurface == true) {
                        Timber.d("Skipping note ${note.id}: never_resurface = true")
                        continue
                    }

                    // Calculate memory strength (includes resurfacing score and decay)
                    val result = calculateMemoryStrength(
                        note = note,
                        currentResurfaceScore = currentState?.score ?: 0.0,
                        lastShownAt = currentState?.lastShownAt,
                        lastInteractionAt = currentState?.lastInteractionAt
                    )

                    // Update both score and memory state
                    resurfaceStateDao.updateMemoryState(
                        noteId = note.id,
                        score = result.resurfaceScore,
                        memoryStrength = result.memoryStrength,
                        memoryState = result.memoryState.name
                    )

                    // Track for summary
                    when (result.memoryState) {
                        MemoryState.FRESH -> freshCount++
                        MemoryState.CONDENSED -> condensedCount++
                        MemoryState.FADED -> fadedCount++
                        MemoryState.DORMANT -> dormantCount++
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Error calculating memory strength for note ${note.id}")
                    // Continue with other notes
                }
            }

            Timber.d(
                "Adaptive Memory Decay completed: " +
                "Fresh=$freshCount, Condensed=$condensedCount, " +
                "Faded=$fadedCount, Dormant=$dormantCount"
            )

            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "Resurface score worker failed")
            Result.retry()
        }
    }
}
