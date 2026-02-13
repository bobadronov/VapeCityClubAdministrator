package org.bigblackowl.vccadmin.ui.login

sealed interface LoginScreenIntent {
    data class EmailChanged(val value: String) : LoginScreenIntent
    data class PasswordChanged(val value: String) : LoginScreenIntent
    data object TogglePasswordVisibility : LoginScreenIntent
    data class AutoLoginChanged(val enabled: Boolean) : LoginScreenIntent
    data object LoginClicked : LoginScreenIntent
    data object LogoutClicked : LoginScreenIntent
}