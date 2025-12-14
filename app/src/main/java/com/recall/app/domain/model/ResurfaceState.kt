package com.recall.app.domain.model

/**
 * Domain model for resurfacing state
 */
data class ResurfaceState(
    val noteId: String,
    val score: Double = 0.0,
    val lastShownAt: Long? = null,
    val neverResurface: Boolean = false
)
