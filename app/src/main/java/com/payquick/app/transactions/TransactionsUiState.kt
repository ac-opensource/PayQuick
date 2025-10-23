package com.payquick.app.transactions

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

enum class TransactionListFilter { ALL, SENT, RECEIVED }

@Immutable
data class TransactionsUiState(
    val isLoading: Boolean = true,
    val isFetchingMore: Boolean = false,
    val endReached: Boolean = false,
    val groups: List<TransactionUiGroup> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val filter: TransactionListFilter = TransactionListFilter.ALL
)

@Immutable
data class TransactionUiGroup(
    val monthLabel: String,
    val items: List<TransactionUiItem>
)

@Immutable
data class TransactionUiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val isCredit: Boolean,
    val statusLabel: String,
    val dateTime: LocalDateTime,
    val currencyCode: String,
    val counterpartyLabel: String,
    val directionLabel: String
)
