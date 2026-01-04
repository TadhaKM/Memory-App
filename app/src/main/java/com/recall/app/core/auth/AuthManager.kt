package com.recall.app.core.auth

import com.recall.app.core.security.InputValidator
import com.recall.app.core.security.RateLimitExceededException
import com.recall.app.core.security.RateLimitResult
import com.recall.app.core.security.RateLimiter
import com.recall.app.core.security.ValidationResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SECURITY: Authentication manager with input validation and rate limiting
 *
 * Security measures implemented:
 * - Input validation for email and password
 * - Rate limiting to prevent brute force attacks
 * - Secure logging (no passwords logged)
 *
 * OWASP References:
 * - A07:2021 - Identification and Authentication Failures
 * - A04:2021 - Insecure Design (rate limiting)
 */
@Singleton
class AuthManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val inputValidator: InputValidator,
    private val rateLimiter: RateLimiter
) {
    private val auth get() = supabaseClient.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        val session = auth.currentSessionOrNull()
        _authState.value = if (session != null) {
            AuthState.Authenticated(
                userId = session.user?.id ?: "",
                email = session.user?.email ?: ""
            )
        } else {
            AuthState.Unauthenticated
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        // ==========================================================================
        // SECURITY: Rate limiting for auth endpoints
        // ==========================================================================
        when (val rateLimitResult = rateLimiter.tryAcquire(RateLimiter.Limits.SUPABASE_AUTH)) {
            is RateLimitResult.Limited -> {
                Timber.w("Auth rate limited: ${rateLimitResult.message}")
                return Result.failure(
                    RateLimitExceededException(
                        rateLimitResult.retryAfterMs,
                        "Too many sign up attempts. Please wait ${rateLimitResult.retryAfterMs / 1000} seconds."
                    )
                )
            }
            is RateLimitResult.Allowed -> { /* Continue */ }
        }

        // ==========================================================================
        // SECURITY: Input validation
        // ==========================================================================
        val emailResult = inputValidator.validateEmail(email)
        if (emailResult is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(emailResult.errorMessage))
        }

        val passwordResult = inputValidator.validatePassword(password)
        if (passwordResult is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(passwordResult.errorMessage))
        }

        val validatedEmail = (emailResult as ValidationResult.Valid).sanitizedValue

        return try {
            auth.signUpWith(Email) {
                this.email = validatedEmail
                this.password = password
            }
            checkAuthStatus()
            // SECURITY: Don't log email in production (PII)
            Timber.d("Sign up successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sign up failed")
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        // ==========================================================================
        // SECURITY: Rate limiting for auth endpoints
        // ==========================================================================
        when (val rateLimitResult = rateLimiter.tryAcquire(RateLimiter.Limits.SUPABASE_AUTH)) {
            is RateLimitResult.Limited -> {
                Timber.w("Auth rate limited: ${rateLimitResult.message}")
                return Result.failure(
                    RateLimitExceededException(
                        rateLimitResult.retryAfterMs,
                        "Too many sign in attempts. Please wait ${rateLimitResult.retryAfterMs / 1000} seconds."
                    )
                )
            }
            is RateLimitResult.Allowed -> { /* Continue */ }
        }

        // ==========================================================================
        // SECURITY: Input validation
        // ==========================================================================
        val emailResult = inputValidator.validateEmail(email)
        if (emailResult is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(emailResult.errorMessage))
        }

        // For sign in, only validate length (not strength - password already exists)
        if (password.isBlank() || password.length > InputValidator.Limits.PASSWORD_MAX_LENGTH) {
            return Result.failure(IllegalArgumentException("Invalid password"))
        }

        val validatedEmail = (emailResult as ValidationResult.Valid).sanitizedValue

        return try {
            auth.signInWith(Email) {
                this.email = validatedEmail
                this.password = password
            }
            checkAuthStatus()
            // SECURITY: Don't log email in production (PII)
            Timber.d("Sign in successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sign in failed")
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            _authState.value = AuthState.Unauthenticated
            Timber.d("Sign out successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sign out failed")
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    fun isAuthenticated(): Boolean {
        return auth.currentSessionOrNull() != null
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String, val email: String) : AuthState()
}
