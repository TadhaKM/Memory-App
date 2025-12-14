package com.recall.app.di

import com.recall.app.ai.client.EmbeddingsClient
import com.recall.app.ai.client.LlmClient
import com.recall.app.ai.client.TranscriptionClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideLlmClient(): LlmClient? {
        // TODO: Return actual implementation when AI provider is configured
        // Example: return OpenAiLlmClient(apiKey)
        return null // Graceful degradation - app works without AI
    }

    @Provides
    @Singleton
    fun provideEmbeddingsClient(): EmbeddingsClient? {
        // TODO: Return actual implementation when embeddings provider is configured
        // Example: return OpenAiEmbeddingsClient(apiKey)
        return null // Graceful degradation
    }

    @Provides
    @Singleton
    fun provideTranscriptionClient(): TranscriptionClient? {
        // TODO: Return actual implementation when transcription provider is configured
        // Example: return WhisperTranscriptionClient(apiKey)
        return null // Graceful degradation
    }
}
