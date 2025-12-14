package com.recall.app.data.local.dao

import androidx.room.*
import com.recall.app.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE note_id = :noteId")
    suspend fun getAttachmentsByNoteId(noteId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE note_id = :noteId")
    fun getAttachmentsByNoteIdFlow(noteId: String): Flow<List<AttachmentEntity>>

    @Query("""
        SELECT * FROM attachments
        WHERE sync_state = :syncState
        ORDER BY note_id
    """)
    suspend fun getAttachmentsBySyncState(syncState: Int): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Update
    suspend fun updateAttachment(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: String)

    @Query("""
        UPDATE attachments
        SET sync_state = :syncState
        WHERE id = :attachmentId
    """)
    suspend fun updateSyncState(attachmentId: String, syncState: Int)

    @Query("""
        UPDATE attachments
        SET storage_path = :storagePath, sync_state = :syncState
        WHERE id = :attachmentId
    """)
    suspend fun updateStoragePath(attachmentId: String, storagePath: String, syncState: Int)
}
