package com.payquick.app.transactions

import androidx.compose.runtime.Immutable

@Immutable
data class TransactionsUiState(
    val isInitialLoading: Boolean = true,
    val isPaginating: Boolean = false,
    val groups: List<TransactionUiGroup> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val errorMessage: String? = null
) {
    val canLoadNext: Boolean get() = currentPage < totalPages && !isPaginating
    val canLoadPrevious: Boolean get() = currentPage > 1 && !isPaginating
}

@Immutable
data class TransactionUiGroup(
    val monthLabel: String,
    val items: List<TransactionUiItem>
)

@Immutable
data class TransactionUiItem(
    val id: String,
    val description: String,
    val amount: String,
    val status: String,
    val timestamp: String
)
