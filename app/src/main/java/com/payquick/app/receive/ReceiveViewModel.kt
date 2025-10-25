package com.payquick.app.receive

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.domain.usecase.GenerateMockReceiveCodeUseCase
import com.payquick.domain.time.TimeZone
import com.payquick.domain.time.toJavaLocalDateTime
import com.payquick.domain.time.toLocalDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import com.payquick.R
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val generateMockReceiveCodeUseCase: GenerateMockReceiveCodeUseCase,
    @ApplicationContext private val context: Context
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
                refreshedLabel = context.getString(R.string.receive_refreshed_label, timestamp)
            )
        }
    }

    fun copyLink() {
        viewModelScope.launch {
            _events.emit(ReceiveEvent.ShowMessage(context.getString(R.string.receive_message_copied)))
        }
    }

    fun shareLink() {
        viewModelScope.launch {
            _events.emit(ReceiveEvent.ShowMessage(context.getString(R.string.receive_message_share_coming)))
        }
    }
}
