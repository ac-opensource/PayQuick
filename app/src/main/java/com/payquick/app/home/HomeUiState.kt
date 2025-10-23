package com.payquick.app.home

import java.time.LocalDateTime

data class HomeUiState(
    val isLoading: Boolean = true,
    val isFetchingMore: Boolean = false,
    val endReached: Boolean = false,
    val headline: String = "",
    val subHeadline: String = "",
    val balance: String = "$0.00",
    val lastRefreshedLabel: String? = null,
    val transactionGroups: List<HomeTransactionGroup> = emptyList(),
    val errorMessage: String? = null
)

data class HomeTransactionGroup(
    val monthLabel: String,
    val items: List<HomeTransactionUi>
)

data class HomeTransactionUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val isCredit: Boolean,
    val status: String,
    val dateTime: LocalDateTime
)
