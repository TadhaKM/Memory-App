package com.recall.app.di

import android.content.Context
import com.recall.app.core.media.AudioRecorder
import com.recall.app.core.media.ImageManager
import com.recall.app.core.ocr.OcrProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AudioRecorder(context)
    }

    @Provides
    @Singleton
    fun provideImageManager(@ApplicationContext context: Context): ImageManager {
        return ImageManager(context)
    }

    @Provides
    @Singleton
    fun provideOcrProcessor(): OcrProcessor {
        return OcrProcessor()
    }
}
