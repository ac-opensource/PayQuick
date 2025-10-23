package com.payquick.domain.usecase

import com.payquick.domain.model.TransactionPage
import com.payquick.domain.repository.TransactionRepository
import javax.inject.Inject

class FetchTransactionsPageUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(page: Int): Result<TransactionPage> =
        transactionRepository.fetchTransactions(page)
}
