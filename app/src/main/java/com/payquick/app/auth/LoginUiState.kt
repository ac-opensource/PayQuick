package com.payquick.app.auth

import androidx.annotation.StringRes

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @StringRes val emailErrorResId: Int? = null,
    @StringRes val formMessageResId: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val rememberMe: Boolean = false
) {
    val isFormValid: Boolean get() = email.isNotBlank() && password.isNotBlank()
}
