package com.payquick.domain.repository

import com.payquick.domain.model.TransactionPage

interface TransactionRepository {
    suspend fun fetchTransactions(page: Int): Result<TransactionPage>
}
