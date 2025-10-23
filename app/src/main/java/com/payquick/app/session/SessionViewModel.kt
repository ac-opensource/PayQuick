package com.payquick.app.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.domain.model.Session
import com.payquick.domain.usecase.ObserveSessionUseCase
import com.payquick.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val isLoading: Boolean = true,
    val session: Session? = null,
    val isLoggingOut: Boolean = false
)

sealed class SessionEvent {
    data class Error(val message: String) : SessionEvent()
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>()
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    init {
        observeSessionUseCase()
            .onEach { session ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        isLoggingOut = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun logout() {
        if (_state.value.isLoggingOut) return
        viewModelScope.launch {
            _state.update { it.copy(isLoggingOut = true) }
            runCatching { logoutUseCase() }
                .onFailure { error ->
                    _state.update { it.copy(isLoggingOut = false) }
                    _events.emit(SessionEvent.Error(error.message ?: "Unable to log out"))
                }
        }
    }
}
