package com.recall.app.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.recall.app.core.auth.AuthManager
import com.recall.app.data.local.dao.AttachmentDao
import com.recall.app.data.remote.NotesDataSource
import com.recall.app.data.remote.StorageDataSource
import com.recall.app.data.remote.model.AttachmentDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.io.File

@HiltWorker
class UploadAttachmentsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val attachmentDao: AttachmentDao,
    private val storageDataSource: StorageDataSource,
    private val notesDataSource: NotesDataSource,
    private val authManager: AuthManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!authManager.isAuthenticated()) {
            Timber.w("User not authenticated, skipping attachment upload")
            return Result.success()
        }

        val userId = authManager.getCurrentUserId() ?: return Result.failure()

        return try {
            // Get all local-only attachments
            val pendingAttachments = attachmentDao.getAttachmentsBySyncState(0) // LOCAL_ONLY

            Timber.d("Found ${pendingAttachments.size} attachments to upload")

            for (attachment in pendingAttachments) {
                try {
                    // Upload to storage
                    val file = File(attachment.localUri)
                    if (!file.exists()) {
                        Timber.w("Attachment file not found: ${attachment.localUri}")
                        continue
                    }

                    val uploadResult = storageDataSource.uploadAttachment(
                        userId = userId,
                        noteId = attachment.noteId,
                        attachmentId = attachment.id,
                        file = file,
                        mimeType = attachment.mimeType
                    )

                    if (uploadResult.isSuccess) {
                        val storagePath = uploadResult.getOrNull()!!

                        // Update storage path and sync state in local DB
                        attachmentDao.updateStoragePath(
                            attachmentId = attachment.id,
                            storagePath = storagePath,
                            syncState = 1 // SYNCED
                        )

                        // Upsert to Supabase DB
                        val attachmentDto = AttachmentDto(
                            id = attachment.id,
                            noteId = attachment.noteId,
                            userId = userId,
                            type = attachment.type,
                            mimeType = attachment.mimeType,
                            storagePath = storagePath,
                            durationMs = attachment.durationMs,
                            sizeBytes = attachment.sizeBytes
                        )

                        notesDataSource.upsertAttachment(attachmentDto)
                        Timber.d("Uploaded attachment: ${attachment.id}")
                    } else {
                        Timber.e("Failed to upload attachment: ${attachment.id}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error uploading attachment: ${attachment.id}")
                    // Continue with next attachment
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Upload attachments worker failed")
            Result.retry()
        }
    }
}
