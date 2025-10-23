package com.payquick.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
        _state.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submit() {
        val current = _state.value
        if (!current.isFormValid || current.isLoading) {
            _state.update { it.copy(errorMessage = it.errorMessage ?: "Enter your credentials") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(current.email.trim(), current.password)
            result.onSuccess {
                _state.update { it.copy(isLoading = false) }
                _events.emit(LoginEvent.LoggedIn)
            }.onFailure { error ->
                val message = error.message ?: "Unable to sign in"
                _state.update { it.copy(isLoading = false, errorMessage = message) }
                _events.emit(LoginEvent.ShowMessage(message))
            }
        }
    }
}
