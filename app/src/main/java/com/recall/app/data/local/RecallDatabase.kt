package com.recall.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
abstract class RecallDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun aiMetadataDao(): AiMetadataDao
    abstract fun resurfaceStateDao(): ResurfaceStateDao
    abstract fun searchDao(): SearchDao

    companion object {
        /**
         * Migration 1 → 2: Add Adaptive Memory Decay columns
         *
         * Adds:
         * - memory_strength: The core decay metric
         * - memory_state: Derived state (FRESH, CONDENSED, FADED, DORMANT)
         * - last_interaction_at: Tracks user engagement to fight decay
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns with defaults
                db.execSQL(
                    "ALTER TABLE resurface_state ADD COLUMN memory_strength REAL NOT NULL DEFAULT 3.0"
                )
                db.execSQL(
                    "ALTER TABLE resurface_state ADD COLUMN memory_state TEXT NOT NULL DEFAULT 'FRESH'"
                )
                db.execSQL(
                    "ALTER TABLE resurface_state ADD COLUMN last_interaction_at INTEGER"
                )

                // Create indices for new columns
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_resurface_state_memory_strength ON resurface_state(memory_strength)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_resurface_state_memory_state ON resurface_state(memory_state)"
                )
            }
        }
    }
}
