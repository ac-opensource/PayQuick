package com.payquick.domain.model

import java.math.BigDecimal
import kotlinx.datetime.Instant

enum class TransactionType { TRANSFER, TOPUP }

data class Transaction(
    val id: String,
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val status: String,
    val createdAt: Instant,
    val destinationId: String
)
