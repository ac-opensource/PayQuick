package com.payquick.app.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.domain.model.Transaction
import com.payquick.domain.model.TransactionType
import com.payquick.domain.usecase.FetchTransactionsPageUseCase
import com.payquick.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val fetchTransactionsPage: FetchTransactionsPageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionsUiState())
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TransactionsEvent>()
    val events: SharedFlow<TransactionsEvent> = _events.asSharedFlow()

    private val loadedTransactions = mutableListOf<Transaction>()
    private val numberFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private val timestampFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    private val zone = TimeZone.currentSystemDefault()

    private var currentPage = 0
    private var totalPages = 1
    private var lastPageSize = 0
    private var autoLoading = false

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, endReached = false, groups = emptyList()) }
            currentPage = 0
            totalPages = 1
            loadedTransactions.clear()
            lastPageSize = 0
            loadPage(page = 1, append = false)
        }
    }

    fun loadMore() {
        if (_state.value.isLoading || _state.value.isFetchingMore || _state.value.endReached || autoLoading) return
        if (currentPage >= totalPages) {
            _state.update { it.copy(endReached = true) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isFetchingMore = true, errorMessage = null) }
            loadPage(page = currentPage + 1, append = true)
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        updateGroups()
    }

    fun onFilterChange(filter: TransactionListFilter) {
        if (_state.value.filter == filter) return
        _state.update { it.copy(filter = filter) }
        updateGroups()
    }

    fun onRetry() {
        if (loadedTransactions.isEmpty()) {
            refresh()
        } else {
            loadMore()
        }
    }

    private suspend fun loadPage(page: Int, append: Boolean) {
        fetchTransactionsPage(page)
            .onSuccess { pageData ->
                totalPages = pageData.totalPages.coerceAtLeast(1)
                currentPage = page
                lastPageSize = pageData.itemsPerPage.coerceAtLeast(pageData.transactions.size)

                if (!append) loadedTransactions.clear()
                loadedTransactions.addAll(pageData.transactions)

                _state.update {
                    it.copy(
                        isLoading = false,
                        isFetchingMore = false,
                        endReached = currentPage >= totalPages,
                        errorMessage = null
                    )
                }

                updateGroups()
            }
            .onFailure { error ->
                autoLoading = false
                _state.update {
                    it.copy(
                        isLoading = false,
                        isFetchingMore = false,
                        errorMessage = error.message ?: "Unable to load transactions"
                    )
                }
            }
    }

    private fun updateGroups() {
        val groups = buildFilteredGroups()
        _state.update { it.copy(groups = groups) }

        val itemsCount = groups.sumOf { it.items.size }
        val targetCount = max(lastPageSize, AUTO_LOAD_TARGET)
        val shouldAutoLoad = !autoLoading && !_state.value.isLoading && !_state.value.isFetchingMore && !_state.value.endReached && currentPage < totalPages && targetCount > 0 && itemsCount < targetCount
        if (shouldAutoLoad) {
            autoLoading = true
            _state.update { it.copy(isFetchingMore = true) }
            viewModelScope.launch {
                loadPage(page = currentPage + 1, append = true)
                autoLoading = false
                updateGroups()
            }
        }
    }

    private fun buildFilteredGroups(): List<TransactionUiGroup> {
        if (loadedTransactions.isEmpty()) return emptyList()

        val query = _state.value.searchQuery.trim().lowercase(Locale.getDefault())
        val filter = _state.value.filter

        val uiItems = loadedTransactions.asSequence()
            .filter { transaction ->
                when (filter) {
                    TransactionListFilter.ALL -> true
                    TransactionListFilter.SENT -> transaction.type == TransactionType.TRANSFER
                    TransactionListFilter.RECEIVED -> transaction.type != TransactionType.TRANSFER
                }
            }
            .map { it.toUiItem() }
            .filter { item ->
                if (query.isBlank()) return@filter true
                val haystack = listOf(
                    item.title,
                    item.counterpartyLabel,
                    item.statusLabel,
                    item.directionLabel,
                    item.amountLabel,
                    item.currencyCode
                ).joinToString(" ").lowercase(Locale.getDefault())
                haystack.contains(query)
            }
            .sortedByDescending { it.dateTime }
            .toList()

        val grouped = uiItems.groupBy { item -> MonthYear(item.dateTime.year, item.dateTime.monthValue) }

        return grouped.entries
            .sortedByDescending { it.key }
            .map { (monthYear, items) ->
                TransactionUiGroup(
                    monthLabel = monthYear.label(),
                    items = items.sortedByDescending { it.dateTime }
                )
            }
    }

    private fun Transaction.toUiItem(): TransactionUiItem {
        val isCredit = type != TransactionType.TRANSFER
        val signedAmount = if (isCredit) this.amount else this.amount.negate()
        val formattedAmount = formatCurrency(signedAmount.abs(), currency)
        val amountLabel = if (signedAmount.signum() >= 0) "+$formattedAmount" else "-$formattedAmount"

        val localDateTime = createdAt.toLocalDateTime(zone).toJavaLocalDateTime()
        val subtitle = timestampFormatter.format(localDateTime)

        val counterparty = destinationId
        val directionLabel = if (isCredit) {
            "Received from $counterparty"
        } else {
            "Sent to $counterparty"
        }

        val statusLabel = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        return TransactionUiItem(
            id = id,
            title = counterparty,
            subtitle = subtitle,
            amountLabel = amountLabel,
            isCredit = isCredit,
            statusLabel = statusLabel,
            dateTime = localDateTime,
            currencyCode = currency,
            counterpartyLabel = counterparty,
            directionLabel = directionLabel
        )
    }

    private fun formatCurrency(amount: BigDecimal, currencyCode: String): String {
        val fallback = runCatching { Currency.getInstance(Locale.getDefault()) }
            .getOrNull() ?: numberFormatter.currency ?: Currency.getInstance("USD")
        val targetCurrency = runCatching { Currency.getInstance(currencyCode) }
            .getOrNull() ?: fallback
        if (numberFormatter.currency != targetCurrency) {
            numberFormatter.currency = targetCurrency
        }
        return numberFormatter.format(amount)
    }

    private data class MonthYear(val year: Int, val month: Int) : Comparable<MonthYear> {
        override fun compareTo(other: MonthYear): Int {
            return compareValuesBy(this, other, MonthYear::year, MonthYear::month)
        }

        fun label(): String {
            val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
            return "$monthName $year"
        }
    }

    companion object {
        private const val AUTO_LOAD_TARGET = 12
    }

    private fun resolveCurrency(currencyCode: String): Currency {
        val fallback = runCatching { Currency.getInstance(Locale.getDefault()) }
            .getOrNull() ?: numberFormatter.currency ?: Currency.getInstance("USD")
        return runCatching { Currency.getInstance(currencyCode) }
            .getOrElse { fallback }
    }
}
