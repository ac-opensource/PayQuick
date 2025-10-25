package com.payquick.data.transactions

import com.payquick.data.network.PayQuickApi
import com.payquick.data.network.model.Pagination
import com.payquick.data.network.model.TransactionDto
import com.payquick.data.network.model.TransactionEnvelope
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DefaultTransactionRepositoryTest {

    @MockK
    private lateinit var api: PayQuickApi

    private lateinit var repository: DefaultTransactionRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = DefaultTransactionRepository(api)
    }

    @Test
    fun fetchTransactionsMapsNetworkResponse() = runTest {
        val dto = TransactionDto(
            id = "1",
            amountInCents = 1234,
            currency = "USD",
            type = "TRANSFER",
            status = "completed",
            createdAt = "2024-01-01T00:00:00Z",
            destinationId = "alex@example.com"
        )
        val envelope = TransactionEnvelope(
            status = "success",
            message = "ok",
            pagination = Pagination(
                currentPage = 1,
                totalPages = 1,
                totalItems = 1,
                itemsPerPage = 1
            ),
            data = listOf(dto)
        )
        coEvery { api.getTransactions(1) } returns envelope

        val result = repository.fetchTransactions(1)

        assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        assertEquals(1, page.transactions.size)
        val transaction = page.transactions.first()
        assertEquals(BigDecimal.valueOf(12.34), transaction.amount)
        assertEquals("alex@example.com", transaction.destinationId)
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), transaction.createdAt)
    }

    @Test
    fun fetchTransactionsPropagatesFailure() = runTest {
        coEvery { api.getTransactions(1) } throws IllegalStateException()

        val result = repository.fetchTransactions(1)

        assertTrue(result.isFailure)
    }
}
