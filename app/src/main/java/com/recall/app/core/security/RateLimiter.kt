package com.recall.app.core.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SECURITY: Client-side rate limiter using Token Bucket algorithm
 *
 * Protects against:
 * - Accidental API abuse (infinite loops, runaway requests)
 * - Cost overruns from excessive API calls
 * - Server-side rate limit violations (429 errors)
 *
 * OWASP Reference: A04:2021 - Insecure Design
 * - Implement rate limiting to prevent resource exhaustion
 * - Use sensible defaults with graceful degradation
 */
@Singleton
class RateLimiter @Inject constructor() {

    // Bucket storage: key -> TokenBucket
    private val buckets = ConcurrentHashMap<String, TokenBucket>()
    private val mutex = Mutex()

    /**
     * Rate limit configurations for different API endpoints
     */
    object Limits {
        // Anthropic API: 60 requests/minute, burst of 10
        val ANTHROPIC_API = RateLimitConfig(
            key = "anthropic_api",
            maxTokens = 10,
            refillRate = 1.0, // 1 token per second = 60/minute
            refillIntervalMs = 1000L
        )

        // Supabase Auth: 10 requests/minute (auth is expensive)
        val SUPABASE_AUTH = RateLimitConfig(
            key = "supabase_auth",
            maxTokens = 5,
            refillRate = 0.167, // ~10/minute
            refillIntervalMs = 1000L
        )

        // Supabase Database: 100 requests/minute
        val SUPABASE_DB = RateLimitConfig(
            key = "supabase_db",
            maxTokens = 20,
            refillRate = 1.67, // ~100/minute
            refillIntervalMs = 1000L
        )

        // Supabase Storage: 30 requests/minute (uploads are heavy)
        val SUPABASE_STORAGE = RateLimitConfig(
            key = "supabase_storage",
            maxTokens = 10,
            refillRate = 0.5, // 30/minute
            refillIntervalMs = 1000L
        )

        // General API calls: 120 requests/minute
        val GENERAL = RateLimitConfig(
            key = "general",
            maxTokens = 30,
            refillRate = 2.0, // 120/minute
            refillIntervalMs = 1000L
        )
    }

    /**
     * Attempt to acquire a token for the given rate limit config
     *
     * @return RateLimitResult indicating success or failure with retry info
     */
    suspend fun tryAcquire(config: RateLimitConfig): RateLimitResult {
        return mutex.withLock {
            val bucket = buckets.getOrPut(config.key) {
                TokenBucket(
                    maxTokens = config.maxTokens,
                    refillRate = config.refillRate,
                    refillIntervalMs = config.refillIntervalMs
                )
            }

            bucket.refill()

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0
                Timber.d("RateLimiter[${config.key}]: Acquired token, ${bucket.tokens.toInt()} remaining")
                RateLimitResult.Allowed
            } else {
                val retryAfterMs = ((1.0 - bucket.tokens) / config.refillRate * 1000).toLong()
                Timber.w("RateLimiter[${config.key}]: Rate limited, retry after ${retryAfterMs}ms")
                RateLimitResult.Limited(
                    retryAfterMs = retryAfterMs,
                    message = "Rate limit exceeded for ${config.key}. Please wait ${retryAfterMs / 1000}s."
                )
            }
        }
    }

    /**
     * Check if a request would be allowed without consuming a token
     */
    suspend fun wouldAllow(config: RateLimitConfig): Boolean {
        return mutex.withLock {
            val bucket = buckets[config.key] ?: return@withLock true
            bucket.refill()
            bucket.tokens >= 1.0
        }
    }

    /**
     * Reset rate limit for a specific key (useful for testing)
     */
    suspend fun reset(key: String) {
        mutex.withLock {
            buckets.remove(key)
        }
    }

    /**
     * Reset all rate limits
     */
    suspend fun resetAll() {
        mutex.withLock {
            buckets.clear()
        }
    }

    /**
     * Token bucket implementation
     */
    private class TokenBucket(
        val maxTokens: Int,
        val refillRate: Double,
        val refillIntervalMs: Long
    ) {
        var tokens: Double = maxTokens.toDouble()
        var lastRefillTime: Long = System.currentTimeMillis()

        fun refill() {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRefillTime

            if (elapsed >= refillIntervalMs) {
                val intervals = elapsed / refillIntervalMs
                tokens = minOf(maxTokens.toDouble(), tokens + (intervals * refillRate))
                lastRefillTime = now
            }
        }
    }
}

/**
 * Configuration for a rate limit bucket
 */
data class RateLimitConfig(
    val key: String,
    val maxTokens: Int,
    val refillRate: Double,
    val refillIntervalMs: Long
)

/**
 * Result of a rate limit check
 */
sealed class RateLimitResult {
    object Allowed : RateLimitResult()

    data class Limited(
        val retryAfterMs: Long,
        val message: String
    ) : RateLimitResult()

    fun isAllowed(): Boolean = this is Allowed
}

/**
 * Exception thrown when rate limit is exceeded
 * HTTP 429 equivalent for client-side use
 */
class RateLimitExceededException(
    val retryAfterMs: Long,
    override val message: String
) : Exception(message)
