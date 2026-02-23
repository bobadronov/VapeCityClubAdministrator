// LoginViewModel.kt
package org.bigblackowl.vccadmin.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.AuthRepository
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.invalid_credentials
import vccadministrator.composeapp.generated.resources.invalid_email_format
import vccadministrator.composeapp.generated.resources.login_error
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.password_min_length
import kotlin.time.Duration.Companion.seconds

class LoginScreenViewModel(
    private val authRepository: AuthRepository,
    private val localRepository: LocalRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
    private val errorManager: ErrorManager,
) : ViewModel(), KoinComponent {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(autoLoginState = localRepository.getAutoEnterState()) }
        viewModelScope.launch {
            networkMonitorProvider.isConnected.collect { isConnected ->
                _uiState.update { it.copy(networkState = isConnected) }
            }
        }
    }

    fun onIntent(intent: LoginScreenIntent) {
        when (intent) {
            is LoginScreenIntent.EmailChanged -> onEmailChanged(intent.value)
            is LoginScreenIntent.PasswordChanged -> onPasswordChanged(intent.value)
            LoginScreenIntent.TogglePasswordVisibility -> togglePasswordVisibility()
            is LoginScreenIntent.AutoLoginChanged -> changeAutoLoginState(intent.enabled)
            LoginScreenIntent.LoginClicked -> login()
            LoginScreenIntent.LogoutClicked -> logOut()
        }
    }

    private fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, isEmailError = false, errorMessage = null) }
    }

    private fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, isPasswordError = false, errorMessage = null) }
    }

    private fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun changeAutoLoginState(state: Boolean) {
        localRepository.setAutoEnterState(state)
        _uiState.update { it.copy(autoLoginState = state) }
    }

    private fun login() {
        viewModelScope.launch {
            if (networkMonitorProvider.isConnected.value.not()) {
                _uiState.update { it.copy(errorMessage = getString(Res.string.no_internet)) }
                return@launch
            }
            val current = _uiState.value
            val emailValid = isValidEmail(current.email)
            val passwordValid = current.password.length >= 8

            if (!emailValid || !passwordValid) {
                _uiState.update {
                    it.copy(
                        isEmailError = !emailValid, isPasswordError = !passwordValid, errorMessage = when {
                            !emailValid -> getString(Res.string.invalid_email_format)
                            else -> getString(Res.string.password_min_length)
                        }
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.login(current.email.trim(), current.password).onFailure { exception ->
                val message = when {
                    exception.message?.contains("invalid login credentials", ignoreCase = true) == true -> getString(Res.string.invalid_credentials)

                    exception.message?.contains("network", ignoreCase = true) == true -> getString(Res.string.no_internet)

                    else -> getString(Res.string.login_error)
                }
                _uiState.update { it.copy(errorMessage = message) }
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.LoginScreenViewModel.LOGIN)
            }.also {
                delay(2.seconds)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun logOut() {
        viewModelScope.launch {
            if (networkMonitorProvider.isConnected.value.not()) {
                _uiState.update { it.copy(errorMessage = getString(Res.string.no_internet)) }
                return@launch
            }
            authRepository.signOut()
            localRepository.clearLocalStorage()
            _uiState.update { it.copy(password = "", autoLoginState = false) }
        }
    }

    /**
     * Функція для валідації email без використання регулярних виразів.
     * Це обхідний шлях для потенційних проблем з Regex у Kotlin/Wasm.
     * Перевіряє базову структуру email: локальна частина @ домен.
     */
    private fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false

        val parts = email.split("@")
        if (parts.size != 2) return false

        val local = parts[0]
        val domain = parts[1]

        if (local.isEmpty() || domain.isEmpty()) return false

        // Локальна частина: букви, цифри, _, -, .
        if (!local.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }) return false

        val domainParts = domain.split(".")
        if (domainParts.size < 2) return false

        // Кожен сегмент домену: букви, цифри, _, -
        if (domainParts.any { it.isEmpty() || !it.all { c -> c.isLetterOrDigit() || c == '_' || c == '-' } }) return false

        // TLD (останній сегмент): довжина 2-4 символи (приблизна перевірка)
        val tld = domainParts.last()
        return tld.length in 2..4
    }

}
