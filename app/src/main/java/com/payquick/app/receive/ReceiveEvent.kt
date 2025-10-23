package com.payquick.app.receive

sealed class ReceiveEvent {
    data class ShowMessage(val message: String) : ReceiveEvent()
}
