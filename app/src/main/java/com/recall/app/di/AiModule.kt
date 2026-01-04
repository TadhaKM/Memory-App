package com.recall.app.di

import com.recall.app.ai.client.AnthropicLlmClient
import com.recall.app.ai.client.EmbeddingsClient
import com.recall.app.ai.client.LlmClient
import com.recall.app.ai.client.TranscriptionClient
import com.recall.app.ai.config.AnthropicConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideLlmClient(): LlmClient? {
        return if (AnthropicConfig.isConfigured()) {
            Timber.d("Anthropic API configured, enabling AI features")
            AnthropicLlmClient()
        } else {
            Timber.d("Anthropic API not configured, AI features disabled")
            null // Graceful degradation - app works without AI
        }
    }

    @Provides
    @Singleton
    fun provideEmbeddingsClient(): EmbeddingsClient? {
        // Anthropic doesn't provide embeddings API
        // Could add OpenAI or Voyage AI embeddings later
        return null
    }

    @Provides
    @Singleton
    fun provideTranscriptionClient(): TranscriptionClient? {
        // Could add Whisper API integration later
        return null
    }
}
