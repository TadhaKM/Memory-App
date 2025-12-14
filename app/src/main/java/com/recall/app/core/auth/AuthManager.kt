package com.recall.app.core.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val auth: Auth = supabaseClient.auth

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
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            checkAuthStatus()
            Timber.d("Sign up successful: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sign up failed")
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            checkAuthStatus()
            Timber.d("Sign in successful: $email")
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
