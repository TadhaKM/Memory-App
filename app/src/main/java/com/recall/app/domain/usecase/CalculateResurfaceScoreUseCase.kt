package com.recall.app.domain.usecase

import com.recall.app.core.util.Constants
import com.recall.app.domain.model.AiState
import com.recall.app.domain.model.Note
import com.recall.app.domain.model.NoteType
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

class CalculateResurfaceScoreUseCase @Inject constructor() {

    operator fun invoke(note: Note, currentResurfaceScore: Double, lastShownAt: Long?): Double {
        var score = Random.nextDouble(0.0, 1.0) // Base randomness

        // +2.0 if pinned
        if (note.pinned) {
            score += 2.0
            Timber.d("Note ${note.id}: +2.0 (pinned)")
        }

        // +1.5 if task with unfinished action items
        if (note.aiMetadata?.type == NoteType.TASK) {
            val hasUnfinishedItems = note.aiMetadata.actionItems.any { !it.done }
            if (hasUnfinishedItems) {
                score += 1.5
                Timber.d("Note ${note.id}: +1.5 (task with unfinished items)")
            }
        }

        // +1.0 if idea older than 21 days and not shown recently
        if (note.aiMetadata?.type == NoteType.IDEA) {
            val ageInDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - note.createdAt)
            if (ageInDays >= Constants.IDEA_RESURFACE_THRESHOLD_DAYS) {
                score += 1.0
                Timber.d("Note ${note.id}: +1.0 (idea older than 21 days)")
            }
        }

        // +1.0 if importance >= 2
        if (note.importance >= 2) {
            score += 1.0
            Timber.d("Note ${note.id}: +1.0 (importance ${note.importance})")
        }

        // +0.5 if has AI metadata (processed)
        if (note.aiState == AiState.DONE) {
            score += 0.5
            Timber.d("Note ${note.id}: +0.5 (AI processed)")
        }

        // -1.0 if shown in last 3 days
        lastShownAt?.let {
            val daysSinceShown = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it)
            if (daysSinceShown < Constants.MIN_RESURFACE_INTERVAL_DAYS) {
                score -= 1.0
                Timber.d("Note ${note.id}: -1.0 (shown ${daysSinceShown} days ago)")
            }
        }

        // -0.5 if archived
        if (note.archived) {
            score -= 0.5
            Timber.d("Note ${note.id}: -0.5 (archived)")
        }

        Timber.d("Note ${note.id}: Final score = $score")
        return score.coerceAtLeast(0.0)
    }
}
