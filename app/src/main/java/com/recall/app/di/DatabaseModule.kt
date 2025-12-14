package com.recall.app.di

import android.content.Context
import androidx.room.Room
import com.recall.app.core.util.Constants
import com.recall.app.data.local.RecallDatabase
import com.recall.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRecallDatabase(
        @ApplicationContext context: Context
    ): RecallDatabase {
        return Room.databaseBuilder(
            context,
            RecallDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For MVP - in production, use proper migrations
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: RecallDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun provideAttachmentDao(database: RecallDatabase): AttachmentDao {
        return database.attachmentDao()
    }

    @Provides
    @Singleton
    fun provideAiMetadataDao(database: RecallDatabase): AiMetadataDao {
        return database.aiMetadataDao()
    }

    @Provides
    @Singleton
    fun provideResurfaceStateDao(database: RecallDatabase): ResurfaceStateDao {
        return database.resurfaceStateDao()
    }

    @Provides
    @Singleton
    fun provideSearchDao(database: RecallDatabase): SearchDao {
        return database.searchDao()
    }
}
