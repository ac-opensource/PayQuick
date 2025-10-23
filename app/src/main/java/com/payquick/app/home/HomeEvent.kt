package com.payquick.app.home

sealed class HomeEvent {
    data class ShowMessage(val message: String) : HomeEvent()
}
