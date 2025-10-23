package com.payquick.app.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.domain.model.Transaction
import com.payquick.domain.model.TransactionPage
import com.payquick.domain.model.TransactionType
import com.payquick.domain.usecase.FetchTransactionsPageUseCase
import com.payquick.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Currency
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val fetchTransactionsPage: FetchTransactionsPageUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionsUiState())
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TransactionsEvent>()
    val events: SharedFlow<TransactionsEvent> = _events.asSharedFlow()

    private val pageCache = mutableMapOf<Int, TransactionPage>()
    private val numberFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private val zone = TimeZone.currentSystemDefault()

    init {
        loadPage(page = 1, initial = true)
    }

    fun onNextPage() {
        val next = _state.value.currentPage + 1
        if (next <= _state.value.totalPages) {
            loadPage(page = next, initial = false)
        }
    }

    fun onPreviousPage() {
        val previous = _state.value.currentPage - 1
        if (previous >= 1) {
            loadPage(page = previous, initial = false)
        }
    }

    fun onRetry() {
        loadPage(page = _state.value.currentPage, initial = _state.value.groups.isEmpty())
    }

    fun onLogout() {
        viewModelScope.launch {
            runCatching { logoutUseCase() }
                .onSuccess { _events.emit(TransactionsEvent.LoggedOut) }
                .onFailure { error ->
                    _events.emit(
                        TransactionsEvent.ShowMessage(error.message ?: "Unable to log out")
                    )
                }
        }
    }

    private fun loadPage(page: Int, initial: Boolean) {
        pageCache[page]?.let { cached ->
            _state.update { current ->
                current.copy(
                    isInitialLoading = false,
                    isPaginating = false,
                    currentPage = page,
                    totalPages = cached.totalPages,
                    groups = cached.transactions.toUiGroups(),
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                if (initial) it.copy(isInitialLoading = true, errorMessage = null)
                else it.copy(isPaginating = true, errorMessage = null)
            }

            fetchTransactionsPage(page)
                .onSuccess { pageData ->
                    pageCache[page] = pageData
                    _state.update {
                        it.copy(
                            isInitialLoading = false,
                            isPaginating = false,
                            currentPage = page,
                            totalPages = pageData.totalPages,
                            groups = pageData.transactions.toUiGroups(),
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isInitialLoading = false,
                            isPaginating = false,
                            errorMessage = error.message ?: "Unable to load transactions"
                        )
                    }
                }
        }
    }

    private fun List<Transaction>.toUiGroups(): List<TransactionUiGroup> {
        val grouped = groupBy { txn ->
            val localDate = txn.createdAt.toLocalDateTime(zone)
            MonthYear(localDate.year, localDate.monthNumber)
        }
        return grouped.entries
            .sortedByDescending { it.key }
            .map { (monthYear, transactions) ->
                TransactionUiGroup(
                    monthLabel = monthYear.label(),
                    items = transactions.sortedByDescending { it.createdAt }
                        .map { txn -> txn.toUiItem() }
                )
            }
    }

    private fun Transaction.toUiItem(): TransactionUiItem {
        if (numberFormatter.currency?.currencyCode != currency) {
            numberFormatter.currency = Currency.getInstance(currency)
        }
        val localDate = createdAt.toLocalDateTime(zone)
        val timestamp = "%s %d, %d • %02d:%02d".format(
            Locale.getDefault(),
            localDate.month.name.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) },
            localDate.dayOfMonth,
            localDate.year,
            localDate.hour,
            localDate.minute
        )
        val signedAmount = amountInCents / 100.0 * if (type == TransactionType.TRANSFER) -1 else 1
        val amountLabel = numberFormatter.format(kotlin.math.abs(signedAmount))
        val description = when (type) {
            TransactionType.TRANSFER -> "Sent to $destinationId"
            TransactionType.TOPUP -> "Received from $destinationId"
        }
        val statusLabel = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        return TransactionUiItem(
            id = id,
            description = description,
            amount = if (signedAmount < 0) "-$amountLabel" else "+$amountLabel",
            status = statusLabel,
            timestamp = timestamp
        )
    }

    private data class MonthYear(val year: Int, val month: Int) : Comparable<MonthYear> {
        override fun compareTo(other: MonthYear): Int {
            return compareValuesBy(this, other, MonthYear::year, MonthYear::month)
        }

        fun label(): String {
            val monthName = monthNames.getOrElse(month - 1) { "Month" }
            return "$monthName $year"
        }
    }

    companion object {
        private val monthNames = listOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
        )
    }
}
