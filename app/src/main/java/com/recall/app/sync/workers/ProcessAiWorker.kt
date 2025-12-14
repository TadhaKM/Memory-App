package com.recall.app.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.recall.app.ai.client.TranscriptionClient
import com.recall.app.ai.processor.NoteProcessor
import com.recall.app.core.auth.AuthManager
import com.recall.app.data.local.dao.AiMetadataDao
import com.recall.app.data.local.dao.AttachmentDao
import com.recall.app.data.local.dao.NoteDao
import com.recall.app.data.local.entity.AiMetadataEntity
import com.recall.app.domain.model.ActionItem
import com.recall.app.domain.model.NoteType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

@HiltWorker
class ProcessAiWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteDao: NoteDao,
    private val attachmentDao: AttachmentDao,
    private val aiMetadataDao: AiMetadataDao,
    private val noteProcessor: NoteProcessor,
    private val transcriptionClient: TranscriptionClient?,
    private val authManager: AuthManager
) : CoroutineWorker(context, params) {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        if (!authManager.isAuthenticated()) {
            Timber.w("User not authenticated, skipping AI processing")
            return Result.success()
        }

        return try {
            // Get notes with AI state PENDING (1)
            val pendingNotes = noteDao.getNotesByAiState(1)

            Timber.d("Found ${pendingNotes.size} notes pending AI processing")

            for (note in pendingNotes) {
                try {
                    // Get attachments
                    val attachments = attachmentDao.getAttachmentsByNoteId(note.id)

                    // Transcribe audio attachments
                    var transcript: String? = null
                    val audioAttachments = attachments.filter { it.type == "AUDIO" }
                    if (audioAttachments.isNotEmpty() && transcriptionClient != null) {
                        transcript = transcribeAudioAttachments(audioAttachments)
                    }

                    // Get OCR text from existing metadata if available
                    val existingMetadata = aiMetadataDao.getAiMetadataByNoteId(note.id)
                    val ocrText = existingMetadata?.ocrText

                    // Process with AI
                    val processedData = noteProcessor.processNote(
                        rawText = note.rawText,
                        transcript = transcript,
                        ocrText = ocrText
                    )

                    if (processedData.isSuccess) {
                        val data = processedData.getOrThrow()

                        // Save AI metadata
                        val metadata = AiMetadataEntity(
                            noteId = note.id,
                            summary = data.summary,
                            type = data.type,
                            topicsJson = if (data.topics.isNotEmpty()) {
                                json.encodeToString(data.topics)
                            } else null,
                            entitiesJson = if (data.entities.isNotEmpty()) {
                                json.encodeToString(data.entities)
                            } else null,
                            actionItemsJson = if (data.actionItems.isNotEmpty()) {
                                json.encodeToString(data.actionItems.map {
                                    ActionItem(text = it.text, dueHint = it.dueHint)
                                })
                            } else null,
                            transcript = transcript,
                            ocrText = ocrText,
                            embeddingJson = data.embedding?.let { json.encodeToString(it) },
                            processedAt = System.currentTimeMillis()
                        )

                        aiMetadataDao.insertAiMetadata(metadata)

                        // Update AI state to DONE (2)
                        noteDao.updateAiState(note.id, 2)

                        Timber.d("Processed note with AI: ${note.id}")
                    } else {
                        // Update AI state to FAILED (3)
                        noteDao.updateAiState(note.id, 3)
                        Timber.e("Failed to process note: ${note.id}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing note: ${note.id}")
                    noteDao.updateAiState(note.id, 3)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Process AI worker failed")
            Result.retry()
        }
    }

    private suspend fun transcribeAudioAttachments(
        audioAttachments: List<com.recall.app.data.local.entity.AttachmentEntity>
    ): String? {
        val transcripts = mutableListOf<String>()

        for (attachment in audioAttachments) {
            try {
                val file = File(attachment.localUri)
                if (file.exists()) {
                    val result = transcriptionClient!!.transcribeAudio(file)
                    result.getOrNull()?.let { transcripts.add(it.text) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to transcribe: ${attachment.id}")
            }
        }

        return if (transcripts.isNotEmpty()) {
            transcripts.joinToString("\n\n")
        } else null
    }
}
