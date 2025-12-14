package com.recall.app.data.local.dao

import androidx.room.*
import com.recall.app.data.local.entity.AiMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMetadataDao {
    @Query("SELECT * FROM ai_metadata WHERE note_id = :noteId")
    suspend fun getAiMetadataByNoteId(noteId: String): AiMetadataEntity?

    @Query("SELECT * FROM ai_metadata WHERE note_id = :noteId")
    fun getAiMetadataByNoteIdFlow(noteId: String): Flow<AiMetadataEntity?>

    @Query("""
        SELECT * FROM ai_metadata
        WHERE type = :type
    """)
    suspend fun getAiMetadataByType(type: String): List<AiMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiMetadata(metadata: AiMetadataEntity)

    @Update
    suspend fun updateAiMetadata(metadata: AiMetadataEntity)

    @Delete
    suspend fun deleteAiMetadata(metadata: AiMetadataEntity)

    @Query("DELETE FROM ai_metadata WHERE note_id = :noteId")
    suspend fun deleteAiMetadataByNoteId(noteId: String)
}
