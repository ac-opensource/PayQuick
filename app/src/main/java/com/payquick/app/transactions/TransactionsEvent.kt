package com.payquick.app.transactions

sealed class TransactionsEvent {
    data class ShowMessage(val message: String) : TransactionsEvent()
    data object LoggedOut : TransactionsEvent()
}
