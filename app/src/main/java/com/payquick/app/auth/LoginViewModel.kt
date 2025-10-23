package com.payquick.app.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.R
import com.payquick.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun onEmailChanged(value: String) {
        _state.update { it.copy(email = value, errorMessage = null, emailErrorResId = null, formMessageResId = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRememberMeChanged(value: Boolean) {
        _state.update { it.copy(rememberMe = value) }
    }

    fun submit() {
        login()
    }

    private fun login() {
        val current = _state.value
        if (current.isLoading) return

        val trimmedEmail = current.email.trim()
        when {
            trimmedEmail.isEmpty() -> {
                _state.update {
                    it.copy(
                        emailErrorResId = R.string.login_error_email_required,
                        formMessageResId = R.string.login_error_credentials,
                        errorMessage = null
                    )
                }
                return
            }
            current.password.isBlank() -> {
                _state.update {
                    it.copy(
                        formMessageResId = R.string.login_error_credentials,
                        errorMessage = null
                    )
                }
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                _state.update {
                    it.copy(
                        emailErrorResId = R.string.login_error_invalid_email,
                        formMessageResId = R.string.login_error_invalid_email,
                        errorMessage = null
                    )
                }
                return
            }
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    emailErrorResId = null,
                    formMessageResId = null,
                    email = trimmedEmail
                )
            }
            val result = authRepository.login(trimmedEmail, current.password)
            result.onSuccess {
                val enrolled = authRepository.isMfaEnrolled.first()
                _state.update { it.copy(isLoading = false) }
                val event = if (enrolled) LoginEvent.NavigateToMfaVerify else LoginEvent.NavigateToMfaEnroll
                _events.emit(event)
            }.onFailure { error ->
                val message = error.message
                val fallbackRes = R.string.login_error_generic
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message,
                        formMessageResId = message?.let { null } ?: fallbackRes
                    )
                }
                val event = if (message.isNullOrBlank()) {
                    LoginEvent.ShowMessage(messageResId = fallbackRes)
                } else {
                    LoginEvent.ShowMessage(message = message)
                }
                _events.emit(event)
            }
        }
    }
}
