package com.recall.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.auth.AuthManager
import com.recall.app.core.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Observe auth state
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                _uiState.update {
                    it.copy(
                        isAuthenticated = authState is AuthState.Authenticated,
                        isLoading = authState is AuthState.Loading
                    )
                }
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        val result = authManager.signInWithEmail(email, password)
        if (result.isFailure) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Sign in failed"
                )
            }
            Timber.e("Sign in failed: ${result.exceptionOrNull()?.message}")
        } else {
            _uiState.update { it.copy(isLoading = false, error = null) }
        }
    }

    suspend fun signUp(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        val result = authManager.signUpWithEmail(email, password)
        if (result.isFailure) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Sign up failed"
                )
            }
            Timber.e("Sign up failed: ${result.exceptionOrNull()?.message}")
        } else {
            _uiState.update { it.copy(isLoading = false, error = null) }
        }
    }
}

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
