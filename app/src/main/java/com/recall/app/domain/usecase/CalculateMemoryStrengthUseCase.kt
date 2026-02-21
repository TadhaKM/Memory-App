package com.recall.app.domain.usecase

import com.recall.app.domain.model.MemoryState
import com.recall.app.domain.model.Note
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Calculates memory_strength for a note - the core of Adaptive Memory Decay
 *
 * memory_strength = resurfacing_score - decay_pressure(time)
 *
 * This is SEPARATE from resurfacing_score because:
 * - resurfacing_score determines WHAT to show today
 * - memory_strength determines IF a note has earned persistence
 *
 * Without this separation, decay cannot exist.
 * With this separation, ignoring becomes meaningful.
 */
class CalculateMemoryStrengthUseCase @Inject constructor(
    private val calculateResurfaceScore: CalculateResurfaceScoreUseCase
) {

    /**
     * Calculate memory strength for a note
     *
     * @param note The note to calculate for
     * @param currentResurfaceScore Current resurfacing score (or 0 if new)
     * @param lastShownAt Last time note was shown in Daily Recall
     * @param lastInteractionAt Last time user opened/edited the note (null = never)
     * @return MemoryStrengthResult with strength score and derived state
     */
    operator fun invoke(
        note: Note,
        currentResurfaceScore: Double,
        lastShownAt: Long?,
        lastInteractionAt: Long?
    ): MemoryStrengthResult {
        // Step 1: Get the resurfacing score (what we already have)
        val resurfaceScore = calculateResurfaceScore(note, currentResurfaceScore, lastShownAt)

        // Step 2: Calculate decay pressure based on time since last interaction
        val decayPressure = calculateDecayPressure(
            note = note,
            lastInteractionAt = lastInteractionAt,
            lastShownAt = lastShownAt
        )

        // Step 3: memory_strength = resurfacing_score - decay_pressure
        val memoryStrength = (resurfaceScore - decayPressure).coerceAtLeast(0.0)

        // Step 4: Derive memory state from strength
        val memoryState = MemoryState.fromStrength(memoryStrength)

        Timber.d(
            "Note ${note.id}: resurfaceScore=${"%.2f".format(resurfaceScore)}, " +
            "decayPressure=${"%.2f".format(decayPressure)}, " +
            "memoryStrength=${"%.2f".format(memoryStrength)}, " +
            "state=$memoryState"
        )

        return MemoryStrengthResult(
            resurfaceScore = resurfaceScore,
            decayPressure = decayPressure,
            memoryStrength = memoryStrength,
            memoryState = memoryState
        )
    }

    /**
     * Calculate decay pressure based on time since last interaction
     *
     * Simple, linear decay: days_since_last_interaction * 0.03
     *
     * This means:
     * - 30 days without interaction = 0.9 decay pressure
     * - 60 days without interaction = 1.8 decay pressure
     * - 100 days without interaction = 3.0 decay pressure (enough to push Fresh → Dormant)
     *
     * The philosophical heart: without decay, nothing earns memory.
     * With decay, ignoring becomes meaningful.
     */
    private fun calculateDecayPressure(
        note: Note,
        lastInteractionAt: Long?,
        lastShownAt: Long?
    ): Double {
        val now = System.currentTimeMillis()

        // Use the most recent of: last interaction, last shown, or creation
        val lastActivity = maxOf(
            lastInteractionAt ?: 0L,
            lastShownAt ?: 0L,
            note.createdAt
        )

        val daysSinceActivity = TimeUnit.MILLISECONDS.toDays(now - lastActivity)

        // Linear decay: 0.03 per day
        // This is tunable - start conservative
        val decayRate = DECAY_RATE_PER_DAY

        val basePressure = daysSinceActivity * decayRate

        // Pinned notes resist decay (halved pressure)
        val finalPressure = if (note.pinned) {
            basePressure * PINNED_DECAY_RESISTANCE
        } else {
            basePressure
        }

        return finalPressure.coerceAtLeast(0.0)
    }

    companion object {
        /**
         * Decay rate per day without interaction
         * 0.03 means ~33 days to lose 1.0 strength
         * ~100 days to go from Fresh (3.0) to Dormant (0.0)
         */
        const val DECAY_RATE_PER_DAY = 0.03

        /**
         * Pinned notes decay at half rate
         */
        const val PINNED_DECAY_RESISTANCE = 0.5
    }
}

/**
 * Result of memory strength calculation
 */
data class MemoryStrengthResult(
    /** The resurfacing score (for Daily Recall ranking) */
    val resurfaceScore: Double,

    /** The decay pressure applied */
    val decayPressure: Double,

    /** Final memory strength = resurfaceScore - decayPressure */
    val memoryStrength: Double,

    /** Derived memory state */
    val memoryState: MemoryState
)
