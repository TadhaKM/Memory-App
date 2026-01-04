package com.recall.app.domain.model

/**
 * The 4 Memory States - the heart of Adaptive Memory Decay
 *
 * Every note moves through these cognitive states over time.
 * Notes EARN their way upward through interaction and relevance.
 * Notes FADE downward through neglect and time.
 *
 * This is NOT deletion, hiding, or archiving.
 * This is progressive loss of PROMINENCE, not existence.
 *
 * Think human memory:
 * - You remember that something mattered
 * - You forget the exact wording
 * - Unless you revisit it
 */
enum class MemoryState {
    /**
     * Full note with all details visible
     * Memory Strength > 3.0
     */
    FRESH,

    /**
     * Summary + key entities shown
     * Memory Strength 2.0 - 3.0
     */
    CONDENSED,

    /**
     * Title + 1-line essence only
     * Memory Strength 1.0 - 2.0
     */
    FADED,

    /**
     * Invisible unless searched
     * Memory Strength < 1.0
     * Excluded from Daily Recall
     */
    DORMANT;

    companion object {
        /**
         * Determine memory state from memory strength score
         */
        fun fromStrength(strength: Double): MemoryState {
            return when {
                strength > 3.0 -> FRESH
                strength > 2.0 -> CONDENSED
                strength > 1.0 -> FADED
                else -> DORMANT
            }
        }

        /**
         * Get minimum strength for a state
         */
        fun minStrength(state: MemoryState): Double {
            return when (state) {
                FRESH -> 3.0
                CONDENSED -> 2.0
                FADED -> 1.0
                DORMANT -> 0.0
            }
        }
    }
}
