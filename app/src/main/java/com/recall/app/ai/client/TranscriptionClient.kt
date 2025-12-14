package com.recall.app.ai.client

import java.io.File

/**
 * Abstraction for transcription providers (Whisper, AssemblyAI, etc.)
 */
interface TranscriptionClient {
    suspend fun transcribeAudio(audioFile: File): Result<TranscriptionResult>
}

data class TranscriptionResult(
    val text: String,
    val confidence: Float? = null,
    val durationMs: Long? = null
)
