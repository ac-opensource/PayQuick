package com.payquick.app.send

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.R
import com.payquick.domain.usecase.SubmitMockTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
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
    private val submitMockTransferUseCase: SubmitMockTransferUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SendUiState())
    val state: StateFlow<SendUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SendEvent>()
    val events: SharedFlow<SendEvent> = _events.asSharedFlow()

    init {
        val recipients = listOf(
            Recipient(
                name = context.getString(R.string.send_recipient_name_katarina),
                joinDate = LocalDate.of(2020, 7, 17)
            ),
            Recipient(
                name = context.getString(R.string.send_recipient_name_alex),
                joinDate = LocalDate.of(2022, 3, 1)
            ),
            Recipient(
                name = context.getString(R.string.send_recipient_name_jamie),
                joinDate = LocalDate.of(2021, 11, 20)
            )
        )
        _state.update {
            it.copy(
                availableRecipients = recipients,
                selectedRecipient = recipients.first()
            )
        }
    }

    fun onDigitClick(digit: Char) {
        if (_state.value.amount.length < 10) {
            _state.update {
                it.copy(amount = it.amount + digit, errorMessage = null, errorMessageResId = null)
            }
        }
    }

    fun onBackspaceClick() {
        if (_state.value.amount.isNotEmpty()) {
            _state.update {
                it.copy(amount = it.amount.dropLast(1), errorMessage = null, errorMessageResId = null)
            }
        }
    }

    fun onRecipientChange() {
        _state.update {
            val recipients = it.availableRecipients
            if (recipients.isEmpty()) return@update it
            val currentIndex = recipients.indexOf(it.selectedRecipient).takeIf { idx -> idx >= 0 } ?: 0
            val nextIndex = (currentIndex + 1) % recipients.size
            it.copy(selectedRecipient = recipients[nextIndex])
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
            _state.update {
                it.copy(
                    errorMessage = null,
                    errorMessageResId = it.errorMessageResId ?: R.string.send_error_amount_required
                )
            }
            return
        }

        val amount = current.amount.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _state.update {
                it.copy(
                    errorMessage = null,
                    errorMessageResId = R.string.send_error_amount_invalid
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, errorMessage = null, errorMessageResId = null) }
            val result = submitMockTransferUseCase(
                amount = amount,
                recipient = current.selectedRecipient.name,
                note = null
            )
            result.onSuccess {
                _state.update { it.copy(isProcessing = false, isSuccess = true) }
                _events.emit(SendEvent.TransferCompleted)
            }.onFailure { throwable ->
                val fallbackRes = R.string.send_error_generic
                val message = throwable.message
                _state.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = message,
                        errorMessageResId = message?.let { null } ?: fallbackRes
                    )
                }
                val event = if (message.isNullOrBlank()) {
                    SendEvent.ShowMessage(messageResId = fallbackRes)
                } else {
                    SendEvent.ShowMessage(message = message)
                }
                _events.emit(event)
            }
        }
    }
}
