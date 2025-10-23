package com.payquick.app.transactions

import androidx.annotation.StringRes

sealed class TransactionsEvent {
    data class ShowMessage(
        val message: String? = null,
        @StringRes val messageResId: Int? = null
    ) : TransactionsEvent()
    data object LoggedOut : TransactionsEvent()
}
