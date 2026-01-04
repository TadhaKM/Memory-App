package com.recall.app.di

import com.recall.app.core.security.InputValidator
import com.recall.app.core.security.RateLimiter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * SECURITY: Hilt module providing security-related dependencies
 *
 * Provides:
 * - InputValidator for sanitizing and validating user input
 * - RateLimiter for protecting API endpoints from abuse
 *
 * These are singletons to ensure consistent rate limiting across the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideInputValidator(): InputValidator {
        return InputValidator()
    }

    @Provides
    @Singleton
    fun provideRateLimiter(): RateLimiter {
        return RateLimiter()
    }
}
