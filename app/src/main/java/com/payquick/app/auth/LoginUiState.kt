package com.payquick.app.auth

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean get() = email.isNotBlank() && password.isNotBlank()
}
