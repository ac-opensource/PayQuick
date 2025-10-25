package com.payquick.domain.model

import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class TransactionType { TRANSFER, TOPUP }

data class Transaction @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val status: String,
    val createdAt: Instant,
    val destinationId: String
)
