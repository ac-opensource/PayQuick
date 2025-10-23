package com.payquick.app.auth

import androidx.annotation.StringRes

sealed class LoginEvent {
    data object NavigateToMfaEnroll : LoginEvent()
    data object NavigateToMfaVerify : LoginEvent()
    data class ShowMessage(
        val message: String? = null,
        @StringRes val messageResId: Int? = null
    ) : LoginEvent()
}
