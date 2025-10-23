package com.payquick.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payquick.domain.model.Transaction
import com.payquick.domain.model.TransactionType
import com.payquick.domain.usecase.FetchTransactionsPageUseCase
import com.payquick.domain.usecase.ObserveSessionUseCase
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    private val fetchTransactionsPageUseCase: FetchTransactionsPageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private val timestampFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    private val zone = TimeZone.currentSystemDefault()

    private var currentPage = 1

    init {
        observeSessionUseCase()
            .onEach { session ->
                _state.update { current ->
                    val firstName = session?.user?.fullName?.substringBefore(" ")?.ifBlank { null }
                    val headline = firstName?.let { "Hi, $it" } ?: "Welcome"
                    val subHeadline = session?.user?.email ?: "Let's get you moving money"
                    current.copy(
                        headline = headline,
                        subHeadline = subHeadline
                    )
                }
            }
            .launchIn(viewModelScope)

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, endReached = false, transactionGroups = emptyList()) }
            currentPage = 1
            val result = fetchTransactionsPageUseCase(page = currentPage)
            result.onSuccess { page ->
                val transactionUi = page.transactions.map { it.toUiModel() }
                val balanceAmount = page.transactions.fold(BigDecimal.ZERO) { acc, txn ->
                    val signedAmount = if (txn.type == TransactionType.TRANSFER) txn.amount.negate() else txn.amount
                    acc + signedAmount
                }
                val balanceCurrency = page.transactions.firstOrNull()?.currency ?: currencyFormatter.currency?.currencyCode ?: Currency.getInstance(Locale.getDefault()).currencyCode
                val balanceLabel = formatCurrency(balanceAmount, balanceCurrency)
                val refreshedLabel = timestampFormatter.format(Clock.System.now().toLocalDateTime(zone).toJavaLocalDateTime())

                _state.update {
                    it.copy(
                        isLoading = false,
                        balance = balanceLabel,
                        transactionGroups = transactionUi.toUiGroups(),
                        errorMessage = null,
                        lastRefreshedLabel = "Updated $refreshedLabel"
                    )
                }
            }.onFailure { throwable ->
                val message = throwable.message ?: "Unable to load activity"
                _state.update { it.copy(isLoading = false, errorMessage = message) }
                _events.emit(HomeEvent.ShowMessage(message))
            }
        }
    }

    fun loadMoreTransactions() {
        if (state.value.isLoading || state.value.isFetchingMore || state.value.endReached) return

        viewModelScope.launch {
            _state.update { it.copy(isFetchingMore = true) }
            currentPage++
            val result = fetchTransactionsPageUseCase(page = currentPage)
            result.onSuccess { page ->
                if (page.transactions.isEmpty()) {
                    _state.update { it.copy(endReached = true, isFetchingMore = false) }
                } else {
                    val newTransactions = page.transactions.map { it.toUiModel() }
                    _state.update {
                    val allTransactions = it.transactionGroups.flatMap { it.items } + newTransactions
                        it.copy(
                            transactionGroups = allTransactions.toUiGroups(),
                            isFetchingMore = false
                        )
                    }
                }
            }.onFailure { throwable ->
                val message = throwable.message ?: "Unable to load more activity"
                _state.update { it.copy(isFetchingMore = false, errorMessage = message) }
                _events.emit(HomeEvent.ShowMessage(message))
            }
        }
    }

    private fun List<HomeTransactionUi>.toUiGroups(): List<HomeTransactionGroup> {
        val grouped = groupBy { txn ->
            val localDate = txn.dateTime.toLocalDate()
            MonthYear(localDate.year, localDate.monthValue)
        }
        return grouped.entries
            .sortedByDescending { it.key }
            .map { (monthYear, transactions) ->
                HomeTransactionGroup(
                    monthLabel = monthYear.label(),
                    items = transactions.sortedByDescending { it.dateTime }
                )
            }
    }

    private fun Transaction.toUiModel(): HomeTransactionUi {
        val isCredit = type != TransactionType.TRANSFER
        val signedAmount = if (isCredit) amount else amount.negate()
        val formattedAmount = formatCurrency(signedAmount.abs(), currency)
        val amountLabel = if (signedAmount.signum() >= 0) "+$formattedAmount" else "-$formattedAmount"

        val localDateTime = createdAt.toLocalDateTime(zone).toJavaLocalDateTime()
        val subtitle = timestampFormatter.format(localDateTime)

        val title = when (type) {
            TransactionType.TRANSFER -> "$destinationId"
            TransactionType.TOPUP -> "$destinationId"
        }

        val statusLabel = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        return HomeTransactionUi(
            id = id,
            title = title,
            subtitle = subtitle,
            amountLabel = amountLabel,
            isCredit = isCredit,
            status = statusLabel,
            dateTime = localDateTime,
            currencyCode = currency
        )
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

    private fun formatCurrency(amount: BigDecimal, currencyCode: String): String {
        val fallbackCurrency = runCatching { Currency.getInstance(Locale.getDefault()) }
            .getOrNull() ?: currencyFormatter.currency ?: Currency.getInstance("USD")
        val targetCurrency = runCatching { Currency.getInstance(currencyCode) }
            .getOrNull() ?: fallbackCurrency
        if (currencyFormatter.currency != targetCurrency) {
            currencyFormatter.currency = targetCurrency
        }
        return currencyFormatter.format(amount)
    }
}
