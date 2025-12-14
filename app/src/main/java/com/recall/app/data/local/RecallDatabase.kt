package com.recall.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.recall.app.data.local.dao.*
import com.recall.app.data.local.entity.*

@Database(
    entities = [
        NoteEntity::class,
        AttachmentEntity::class,
        AiMetadataEntity::class,
        ResurfaceStateEntity::class,
        NotesFts::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RecallDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun aiMetadataDao(): AiMetadataDao
    abstract fun resurfaceStateDao(): ResurfaceStateDao
    abstract fun searchDao(): SearchDao
}
