package com.payquick.app.transactions

import android.content.Context
import com.payquick.R
import com.payquick.app.testing.MainDispatcherRule
import com.payquick.domain.model.Transaction
import com.payquick.domain.model.TransactionPage
import com.payquick.domain.model.TransactionType
import com.payquick.domain.repository.TransactionRepository
import com.payquick.domain.usecase.FetchTransactionsPageUseCase
import androidx.lifecycle.viewModelScope
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Context = mockContext()

    @Test
    fun refreshPopulatesGroups() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeTransactionRepository(success = true)
        val viewModel = TransactionsViewModel(FetchTransactionsPageUseCase(repository), context)

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.groups.isNotEmpty())
        assertEquals(null, viewModel.state.value.errorMessage)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun refreshFailureEmitsFallbackMessage() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeTransactionRepository(success = false)
        val viewModel = TransactionsViewModel(FetchTransactionsPageUseCase(repository), context)

        val eventDeferred = async { viewModel.events.filterIsInstance<TransactionsEvent.ShowMessage>().first() }
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(R.string.transactions_error_load, viewModel.state.value.errorMessageResId)
        val event = eventDeferred.await()
        assertTrue(event.messageResId == R.string.transactions_error_load)
        viewModel.viewModelScope.cancel()
    }

    private class FakeTransactionRepository(private val success: Boolean) : TransactionRepository {
        override suspend fun fetchTransactions(page: Int): Result<TransactionPage> {
            return if (success) {
                Result.success(
                    TransactionPage(
                        transactions = listOf(
                            Transaction(
                                id = "1",
                                amount = BigDecimal.TEN,
                                currency = "USD",
                                type = TransactionType.TRANSFER,
                                status = "completed",
                                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                                destinationId = "alice@example.com"
                            )
                        ),
                        currentPage = 1,
                        totalPages = 1,
                        totalItems = 1,
                        itemsPerPage = 1
                    )
                )
            } else {
                Result.failure(RuntimeException())
            }
        }
    }

    private fun mockContext(): Context = mockk(relaxed = true) {
        every { getString(R.string.transactions_direction_received, any()) } answers {
            val counterparty = args[1].toString()
            "Received from $counterparty"
        }
        every { getString(R.string.transactions_direction_sent, any()) } answers {
            val counterparty = args[1].toString()
            "Sent to $counterparty"
        }
        every { getString(R.string.transactions_month_label, any(), any()) } answers {
            val month = args.getOrNull(1)?.toString() ?: ""
            val year = args.getOrNull(2)?.toString() ?: ""
            "$month $year"
        }
        every { getString(R.string.transactions_error_load) } returns "Unable to load transactions"
    }
}
