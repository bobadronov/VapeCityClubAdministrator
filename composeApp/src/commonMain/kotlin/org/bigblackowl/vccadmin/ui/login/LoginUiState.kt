package org.bigblackowl.vccadmin.ui.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEmailError: Boolean = false,
    val isPasswordError: Boolean = false,
    val autoLoginState: Boolean = true,
    val networkState: Boolean = false,
) {
    val canLogin: Boolean
        get() = email.isNotBlank() && password.length >= 8 && !isLoading
}