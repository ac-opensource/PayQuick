package com.payquick.app.send

import java.time.LocalDate
import java.util.Currency

data class SendUiState(
    val amount: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,

    val selectedRecipient: Recipient = Recipient.MOCK_DATA.first(),
    val availableRecipients: List<Recipient> = Recipient.MOCK_DATA,

    val selectedCurrency: Currency = Currency.getInstance("USD"),
    val availableCurrencies: List<Currency> = listOf(
        Currency.getInstance("AUD"),
        Currency.getInstance("NZD"),
        Currency.getInstance("EUR"),
        Currency.getInstance("USD"),
        Currency.getInstance("GBP"),
    )
) {
    val isFormValid: Boolean get() = amount.toDoubleOrNull()?.let { it > 0.0 } == true
}

data class Recipient(
    val name: String,
    val joinDate: LocalDate,
    val avatarUrl: String? = null
) {
    companion object {
        val MOCK_DATA = listOf(
            Recipient("Katarina", LocalDate.of(2020, 7, 17)),
            Recipient("Alex Morgan", LocalDate.of(2022, 3, 1)),
            Recipient("Jamie Rivera", LocalDate.of(2021, 11, 20)),
        )
    }
}
