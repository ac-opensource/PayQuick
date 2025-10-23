package com.payquick.app.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.domain.usecase.SubmitMockTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Currency
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
class SendViewModel @Inject constructor(
    private val submitMockTransferUseCase: SubmitMockTransferUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SendUiState())
    val state: StateFlow<SendUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SendEvent>()
    val events: SharedFlow<SendEvent> = _events.asSharedFlow()

    fun onDigitClick(digit: Char) {
        if (_state.value.amount.length < 10) {
            _state.update {
                it.copy(amount = it.amount + digit, errorMessage = null)
            }
        }
    }

    fun onBackspaceClick() {
        if (_state.value.amount.isNotEmpty()) {
            _state.update {
                it.copy(amount = it.amount.dropLast(1), errorMessage = null)
            }
        }
    }

    fun onRecipientChange() {
        _state.update {
            val currentIndex = it.availableRecipients.indexOf(it.selectedRecipient)
            val nextIndex = (currentIndex + 1) % it.availableRecipients.size
            it.copy(selectedRecipient = it.availableRecipients[nextIndex])
        }
    }

    fun onCurrencyChange(currencyCode: String) {
        _state.update {
            it.copy(selectedCurrency = Currency.getInstance(currencyCode))
        }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isProcessing || !current.isFormValid) {
            _state.update { it.copy(errorMessage = it.errorMessage ?: "Enter an amount") }
            return
        }

        val amount = current.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _state.update { it.copy(errorMessage = "Enter a valid amount") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, errorMessage = null) }
            val result = submitMockTransferUseCase(
                amount = amount,
                recipient = current.selectedRecipient.name,
                note = null
            )
            result.onSuccess {
                _state.update { it.copy(isProcessing = false, isSuccess = true) }
                _events.emit(SendEvent.TransferCompleted)
            }.onFailure { throwable ->
                val message = throwable.message ?: "Unable to send money"
                _state.update { it.copy(isProcessing = false, errorMessage = message) }
                _events.emit(SendEvent.ShowMessage(message))
            }
        }
    }
}
