package com.payquick.app.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.app.navigation.Home
import com.payquick.app.navigation.Login
import com.payquick.app.navigation.PayQuickRoute
import com.payquick.domain.usecase.EnsureSessionUseCase
import com.payquick.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val ensureSessionUseCase: EnsureSessionUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    init {
        verifyExistingSession()
    }

    private fun verifyExistingSession() {
        viewModelScope.launch {
            val result = ensureSessionUseCase()
            result.onSuccess {
                _state.update { it.copy(isLoading = false, nextRoute = Home) }
            }.onFailure {
                runCatching { logoutUseCase() }
                _state.update { it.copy(isLoading = false, nextRoute = Login) }
            }
        }
    }

    fun onNavigationConsumed() {
        _state.update { it.copy(nextRoute = null) }
    }
}

data class SplashUiState(
    val isLoading: Boolean = true,
    val nextRoute: PayQuickRoute? = null
)
