package com.payquick.app.send

import androidx.annotation.StringRes
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

data class SendUiState(
    val amount: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    @StringRes val errorMessageResId: Int? = null,
    val isSuccess: Boolean = false,

    val selectedRecipient: Recipient = Recipient.EMPTY,
    val availableRecipients: List<Recipient> = emptyList(),

    val selectedCurrency: Currency = Currency.getInstance("USD"),
    val availableCurrencies: List<Currency> = listOf(
        Currency.getInstance("AUD"),
        Currency.getInstance("NZD"),
        Currency.getInstance("EUR"),
        Currency.getInstance("USD"),
        Currency.getInstance("GBP"),
    )
) {
    val isFormValid: Boolean get() = amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
}

data class Recipient(
    val name: String,
    val joinDate: LocalDate,
    val avatarUrl: String? = null
) {
    companion object {
        val EMPTY = Recipient("", LocalDate.now())
    }
}
