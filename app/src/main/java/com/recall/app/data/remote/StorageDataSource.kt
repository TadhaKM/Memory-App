package com.recall.app.data.remote

import com.recall.app.core.config.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val storage = supabaseClient.storage

    suspend fun uploadAttachment(
        userId: String,
        noteId: String,
        attachmentId: String,
        file: File,
        mimeType: String
    ): Result<String> {
        return try {
            val extension = when {
                mimeType.contains("audio") -> "m4a"
                mimeType.contains("image") -> "jpg"
                else -> "bin"
            }

            val path = "$userId/$noteId/$attachmentId.$extension"
            val bucket = storage.from(SupabaseConfig.ATTACHMENTS_BUCKET)

            // Upload file
            bucket.upload(path, file.readBytes(), upsert = true)

            Timber.d("Uploaded attachment: $path (${file.length()} bytes)")
            Result.success(path)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload attachment")
            Result.failure(e)
        }
    }

    suspend fun downloadAttachment(storagePath: String): Result<ByteArray> {
        return try {
            val bucket = storage.from(SupabaseConfig.ATTACHMENTS_BUCKET)
            val data = bucket.downloadAuthenticated(storagePath)

            Timber.d("Downloaded attachment: $storagePath (${data.size} bytes)")
            Result.success(data)
        } catch (e: Exception) {
            Timber.e(e, "Failed to download attachment")
            Result.failure(e)
        }
    }

    suspend fun deleteAttachment(storagePath: String): Result<Unit> {
        return try {
            val bucket = storage.from(SupabaseConfig.ATTACHMENTS_BUCKET)
            bucket.delete(storagePath)

            Timber.d("Deleted attachment: $storagePath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete attachment")
            Result.failure(e)
        }
    }

    fun getPublicUrl(storagePath: String): String {
        val bucket = storage.from(SupabaseConfig.ATTACHMENTS_BUCKET)
        return bucket.publicUrl(storagePath)
    }
}
