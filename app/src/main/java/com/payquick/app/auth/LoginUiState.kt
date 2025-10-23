package com.payquick.app.auth

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val rememberMe: Boolean = false
) {
    val isFormValid: Boolean get() = email.isNotBlank() && password.isNotBlank()
}
