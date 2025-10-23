package com.payquick.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface PayQuickRoute

@Serializable
object Home : PayQuickRoute

@Serializable
object Send : PayQuickRoute

@Serializable
object Receive : PayQuickRoute

@Serializable
object Transactions : PayQuickRoute

@Serializable
object Login : PayQuickRoute
