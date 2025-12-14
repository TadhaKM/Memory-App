package com.recall.app.ai.client

/**
 * Abstraction for embeddings providers (OpenAI ada-002, Cohere, etc.)
 */
interface EmbeddingsClient {
    suspend fun generateEmbedding(text: String): Result<List<Float>>

    fun getEmbeddingDimensions(): Int
}
