package com.payquick.app.send

import androidx.annotation.StringRes

sealed class SendEvent {
    data class ShowMessage(
        val message: String? = null,
        @StringRes val messageResId: Int? = null
    ) : SendEvent()
    data object TransferCompleted : SendEvent()
}
