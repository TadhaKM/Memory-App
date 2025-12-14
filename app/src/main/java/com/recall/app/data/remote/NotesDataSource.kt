package com.recall.app.data.remote

import com.recall.app.core.config.SupabaseConfig
import com.recall.app.data.remote.model.AttachmentDto
import com.recall.app.data.remote.model.NoteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val postgrest = supabaseClient.postgrest

    suspend fun upsertNote(note: NoteDto): Result<Unit> {
        return try {
            postgrest.from(SupabaseConfig.NOTES_TABLE)
                .upsert(note)

            Timber.d("Upserted note: ${note.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert note")
            Result.failure(e)
        }
    }

    suspend fun getNoteById(noteId: String): Result<NoteDto?> {
        return try {
            val note = postgrest.from(SupabaseConfig.NOTES_TABLE)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", noteId)
                    }
                }
                .decodeSingleOrNull<NoteDto>()

            Timber.d("Fetched note: $noteId")
            Result.success(note)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch note")
            Result.failure(e)
        }
    }

    suspend fun getUserNotes(userId: String): Result<List<NoteDto>> {
        return try {
            val notes = postgrest.from(SupabaseConfig.NOTES_TABLE)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                        eq("deleted", false)
                    }
                }
                .decodeList<NoteDto>()

            Timber.d("Fetched ${notes.size} notes for user: $userId")
            Result.success(notes)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user notes")
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            postgrest.from(SupabaseConfig.NOTES_TABLE)
                .update({
                    set("deleted", true)
                }) {
                    filter {
                        eq("id", noteId)
                    }
                }

            Timber.d("Deleted note: $noteId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete note")
            Result.failure(e)
        }
    }

    suspend fun upsertAttachment(attachment: AttachmentDto): Result<Unit> {
        return try {
            postgrest.from(SupabaseConfig.ATTACHMENTS_TABLE)
                .upsert(attachment)

            Timber.d("Upserted attachment: ${attachment.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert attachment")
            Result.failure(e)
        }
    }

    suspend fun getNoteAttachments(noteId: String): Result<List<AttachmentDto>> {
        return try {
            val attachments = postgrest.from(SupabaseConfig.ATTACHMENTS_TABLE)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("note_id", noteId)
                    }
                }
                .decodeList<AttachmentDto>()

            Timber.d("Fetched ${attachments.size} attachments for note: $noteId")
            Result.success(attachments)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch attachments")
            Result.failure(e)
        }
    }
}
