package com.payquick.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface PayQuickRoute

@Serializable
object Home : PayQuickRoute

@Serializable
object Splash : PayQuickRoute

@Serializable
object Send : PayQuickRoute

@Serializable
object Receive : PayQuickRoute

@Serializable
object Transactions : PayQuickRoute

@Serializable
object Login : PayQuickRoute

@Serializable
data class TransactionDetails(
    val id: String,
    val amountLabel: String,
    val isCredit: Boolean,
    val counterpartyLabel: String,
    val statusLabel: String,
    val timestampLabel: String,
    val currencyCode: String,
    val directionLabel: String
) : PayQuickRoute
