package com.payquick.app.send

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payquick.R
import com.payquick.app.common.SwipeToSend
import com.payquick.app.common.rememberBackNavigationAction
import com.payquick.app.common.TopBar
import java.time.format.DateTimeFormatter

@Composable
fun SendScreen(
    onNavigateHome: () -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SendViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backAction = rememberBackNavigationAction(onNavigateHome)

    BackHandler(enabled = backAction.isEnabled) {
        backAction.onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SendEvent.ShowMessage -> onShowSnackbar(event.message)
                SendEvent.TransferCompleted -> {
                    onShowSnackbar("Transfer scheduled")
                    backAction.onBack()
                }
            }
        }
    }

    SendContent(
        state = state,
        onDigitClick = viewModel::onDigitClick,
        onBackspaceClick = viewModel::onBackspaceClick,
        onDoneClick = viewModel::onSubmit,
        onNavigateBack = backAction.onBack,
        isBackEnabled = backAction.isEnabled,
        onRecipientChange = viewModel::onRecipientChange,
        onCurrencyChange = viewModel::onCurrencyChange,
        modifier = modifier
    )
}

@Composable
private fun SendContent(
    state: SendUiState,
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onDoneClick: () -> Unit,
    onNavigateBack: () -> Unit,
    isBackEnabled: Boolean,
    onRecipientChange: () -> Unit,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .padding(PaddingValues(horizontal = 20.dp)),
    ) {
        TopBar(
            "Send Money",
            leftIcon = Icons.Rounded.ArrowBack,
            onLeftIconClick = onNavigateBack,
            leftIconEnabled = isBackEnabled
        )
        Spacer(modifier = Modifier.height(24.dp))
        RecipientHeader(
            recipient = state.selectedRecipient,
            onClick = onRecipientChange
        )
        Spacer(modifier = Modifier.height(24.dp))
        AmountDisplay(
            state = state,
            onCurrencyChange = onCurrencyChange
        )
        Spacer(modifier = Modifier.weight(1f))
        Keypad(
            onDigitClick = onDigitClick,
            onBackspaceClick = onBackspaceClick,
            onDoneClick = onDoneClick
        )
        Spacer(modifier = Modifier.height(12.dp))
        SwipeToSend(onSwiped = onDoneClick)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RecipientHeader(
    recipient: Recipient,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_default_user),
            contentDescription = "Recipient avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Column {
            Text(
                text = recipient.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Joined ${recipient.joinDate.format(DateTimeFormatter.ofPattern("MM.dd.yyyy"))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "Change",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AmountDisplay(
    state: SendUiState,
    onCurrencyChange: (String) -> Unit
) {
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.selectedCurrency.symbol,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Box {
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Change currency", modifier = Modifier.clickable { currencyMenuExpanded = true }.padding(end = 8.dp))

                DropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false }
                ) {
                    state.availableCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text("${currency.currencyCode} (${currency.symbol})") },
                            onClick = {
                                onCurrencyChange(currency.currencyCode)
                                currencyMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Text(
                text = state.amount.ifEmpty { "0" },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "1 EUR = 1.22 USD",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.isProcessing) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
        }
        state.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val buttons = "123456789"
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        text = digit.toString(),
                        onClick = { onDigitClick(digit) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(
                icon = Icons.Rounded.Close,
                onClick = onBackspaceClick,
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "0",
                onClick = { onDigitClick('0') },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                icon = Icons.Rounded.Check,
                onClick = onDoneClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KeypadButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        if (text != null) {
            Text(text, fontSize = 24.sp)
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
        }
    }
}
