package com.payquick.app.send

sealed class SendEvent {
    data class ShowMessage(val message: String) : SendEvent()
    data object TransferCompleted : SendEvent()
}
