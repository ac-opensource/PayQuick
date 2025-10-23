package com.payquick.app.auth

sealed class LoginEvent {
    data object LoggedIn : LoginEvent()
    data class ShowMessage(val message: String) : LoginEvent()
}
