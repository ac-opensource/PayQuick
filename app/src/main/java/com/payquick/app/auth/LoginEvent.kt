package com.payquick.app.auth

sealed class LoginEvent {
    data object NavigateToMfaEnroll : LoginEvent()
    data object NavigateToMfaVerify : LoginEvent()
    data class ShowMessage(val message: String) : LoginEvent()
}
