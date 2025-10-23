package com.payquick.data.transactions

import com.payquick.data.network.PayQuickApi
import com.payquick.data.network.model.TransactionDto
import com.payquick.domain.model.Transaction
import com.payquick.domain.model.TransactionPage
import com.payquick.domain.model.TransactionType
import com.payquick.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant

@Singleton
class DefaultTransactionRepository @Inject constructor(
    private val api: PayQuickApi
) : TransactionRepository {

    override suspend fun fetchTransactions(page: Int): Result<TransactionPage> {
        return try {
            val response = api.getTransactions(page)
            Result.success(
                TransactionPage(
                    transactions = response.data.map { it.toDomain() },
                    currentPage = response.pagination.currentPage,
                    totalPages = response.pagination.totalPages,
                    totalItems = response.pagination.totalItems,
                    itemsPerPage = response.pagination.itemsPerPage
                )
            )
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    private fun TransactionDto.toDomain(): Transaction = Transaction(
        id = id,
        amountInCents = amountInCents,
        currency = currency,
        type = TransactionType.valueOf(type),
        status = status,
        createdAt = Instant.parse(createdAt),
        destinationId = destinationId
    )
}
