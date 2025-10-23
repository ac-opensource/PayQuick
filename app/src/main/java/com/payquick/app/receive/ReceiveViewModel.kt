package com.payquick.app.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.domain.usecase.GenerateMockReceiveCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val generateMockReceiveCodeUseCase: GenerateMockReceiveCodeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiveUiState())
    val state: StateFlow<ReceiveUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ReceiveEvent>()
    val events: SharedFlow<ReceiveEvent> = _events.asSharedFlow()

    private val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val timeZone = TimeZone.currentSystemDefault()

    init {
        refreshCode()
    }

    fun refreshCode() {
        val code = generateMockReceiveCodeUseCase()
        val link = "https://payquick.app/pay/${code.lowercase(Locale.getDefault())}"
        val timestamp = formatter.format(Clock.System.now().toLocalDateTime(timeZone).toJavaLocalDateTime())
        _state.update {
            it.copy(
                code = code,
                link = link,
                refreshedLabel = "Generated at $timestamp"
            )
        }
    }

    fun copyLink() {
        viewModelScope.launch {
            _events.emit(ReceiveEvent.ShowMessage("Link copied to clipboard"))
        }
    }

    fun shareLink() {
        viewModelScope.launch {
            _events.emit(ReceiveEvent.ShowMessage("Share sheet coming soon"))
        }
    }
}
