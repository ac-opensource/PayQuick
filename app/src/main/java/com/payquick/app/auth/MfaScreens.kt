package com.payquick.app.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payquick.R
import com.payquick.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

data class MfaEnrollUiState(
    val secret: String,
    val code: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorMessageResId: Int? = null,
    val setupComplete: Boolean = false
)

@HiltViewModel
class MfaEnrollViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        MfaEnrollUiState(secret = generateSecret())
    )
    val state: StateFlow<MfaEnrollUiState> = _state.asStateFlow()

    fun onCodeChanged(value: String) {
        val filtered = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(code = filtered, errorMessageResId = null) }
    }

    fun confirm() {
        if (_state.value.code.length < 6) {
            _state.update { it.copy(errorMessageResId = R.string.mfa_error_code_required) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessageResId = null) }
            runCatching { authRepository.setMfaEnrollment(true) }
                .onSuccess {
                    _state.update { it.copy(isLoading = false, setupComplete = true) }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessageResId = R.string.mfa_error_generic
                        )
                    }
                }
        }
    }

    private fun generateSecret(length: Int = 16): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        return buildString(length) {
            repeat(length) {
                append(alphabet.random())
            }
        }
    }
}

@Composable
private fun OtpCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    cellCount: Int = 6,
    onDone: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (enabled) {
            focusRequester.requestFocus()
        }
    }

    BasicTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(cellCount)
            onValueChange(digits)
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(cellCount) { index ->
                        val digit = value.getOrNull(index)?.toString().orEmpty()
                        val isActive = isFocused && (value.length == index || (value.length == cellCount && index == cellCount - 1))
                        val borderColor = when {
                            digit.isNotEmpty() -> MaterialTheme.colorScheme.primary
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun MfaEnrollScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MfaEnrollViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MfaEnrollContent(
        state = state,
        onCodeChanged = viewModel::onCodeChanged,
        onConfirm = viewModel::confirm,
        onContinue = onSetupComplete,
        modifier = modifier
    )
}

@Composable
private fun MfaEnrollContent(
    state: MfaEnrollUiState,
    onCodeChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.mfa_enroll_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.mfa_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Image(
                painter = painterResource(id = R.drawable.mock_qr),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )
            OutlinedCard(
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    Text(
                        text = stringResource(R.string.mfa_secret_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.secret.chunked(4).joinToString(" "),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.mfa_enroll_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OtpCodeInput(
                    value = state.code,
                    enabled = !state.setupComplete,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { input ->
                        if (!state.setupComplete) {
                            onCodeChanged(input)
                        }
                    },
                    onDone = {
                        if (!state.setupComplete) {
                            onConfirm()
                        }
                    }
                )
                state.errorMessageResId?.let { resId ->
                    Text(
                        text = stringResource(id = resId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (state.setupComplete) {
                Text(
                    text = stringResource(R.string.mfa_enroll_complete_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = {
                    if (state.setupComplete) {
                        onContinue()
                    } else {
                        onConfirm()
                    }
                },
                enabled = if (state.setupComplete) {
                    true
                } else {
                    state.code.length == 6 && !state.isLoading
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (state.setupComplete) {
                            stringResource(R.string.mfa_action_continue)
                        } else {
                            stringResource(R.string.mfa_enroll_confirm)
                        }
                    )
                }
            }
        }
    }
}

data class MfaVerifyUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorMessageResId: Int? = null,
    val verified: Boolean = false
)

@HiltViewModel
class MfaVerifyViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(MfaVerifyUiState())
    val state: StateFlow<MfaVerifyUiState> = _state.asStateFlow()

    fun onCodeChanged(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(code = sanitized, errorMessageResId = null) }
    }

    fun confirm() {
        if (_state.value.code.length < 6) {
            _state.update { it.copy(errorMessageResId = R.string.mfa_error_code_required) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessageResId = null) }
            _state.update { it.copy(isLoading = false, verified = true) }
        }
    }
}

@Composable
fun MfaVerifyScreen(
    onVerificationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MfaVerifyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MfaVerifyContent(
        state = state,
        onCodeChanged = viewModel::onCodeChanged,
        onConfirm = viewModel::confirm,
        onContinue = onVerificationComplete,
        modifier = modifier
    )
}

@Composable
private fun MfaVerifyContent(
    state: MfaVerifyUiState,
    onCodeChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.mfa_verify_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.mfa_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Image(
                painter = painterResource(id = R.drawable.mfa_shield),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.mfa_verify_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OtpCodeInput(
                    value = state.code,
                    enabled = !state.verified,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { input ->
                        if (!state.verified) {
                            onCodeChanged(input)
                        }
                    },
                    onDone = {
                        if (!state.verified) {
                            onConfirm()
                        }
                    }
                )
                state.errorMessageResId?.let { resId ->
                    Text(
                        text = stringResource(id = resId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (state.verified) {
                Text(
                    text = stringResource(R.string.mfa_verify_complete_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = {
                    if (state.verified) {
                        onContinue()
                    } else {
                        onConfirm()
                    }
                },
                enabled = if (state.verified) {
                    true
                } else {
                    state.code.length == 6 && !state.isLoading
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (state.verified) {
                            stringResource(R.string.mfa_action_continue)
                        } else {
                            stringResource(R.string.mfa_verify_confirm)
                        }
                    )
                }
            }
        }
    }
}
