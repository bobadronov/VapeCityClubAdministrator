package org.bigblackowl.vccadmin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.ota.OtaUpdateManager
import org.bigblackowl.vccadmin.ota.UpdateState
import org.bigblackowl.vccadmin.theme.locals.LocalAppLocale
import org.bigblackowl.vccadmin.theme.locals.customAppLocale
import org.bigblackowl.vccadmin.ui.login.LoginScreenIntent
import org.bigblackowl.vccadmin.ui.login.LoginScreenViewModel
import org.bigblackowl.vccadmin.utils.PlatformFunctionProvider

class SettingsScreenViewModel(
    private val localRepository: LocalRepository,
    private val otaUpdateManager: OtaUpdateManager,
    private val loginScreenViewModel: LoginScreenViewModel,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UIEvents>(extraBufferCapacity = 1)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                otaUpdateManager.state.collect { state ->
                    val newVersion = state.extractRemoteVersionLabel()

                    _uiState.update {
                        it.copy(
                            updateState = state,
                            newAppVersionLabel = newVersion
                        )
                    }
                }
            }
            launch {
                otaUpdateManager.uiEvent.collect {
                    _uiEvent.emit(it)
                }
            }
        }

        _uiState.update {
            it.copy(
                appBuildLabel = BuildConfig.APP_VERSION,
                isInitialLoading = false
            )
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Init -> viewModelScope.launch { refreshCacheSize() }
            is SettingsIntent.SetTheme -> setTheme(intent.state)
            is SettingsIntent.SetLogoutDialog -> _uiState.update { it.copy(logoutDialogVisible = intent.visible) }
            is SettingsIntent.SetClearCacheDialog -> _uiState.update { it.copy(clearCacheDialogVisible = intent.visible) }
            SettingsIntent.ClearCache -> clearCache()
            SettingsIntent.CheckUpdates -> otaUpdateManager.check()
            SettingsIntent.DownloadUpdate -> otaUpdateManager.download()
            SettingsIntent.InstallUpdate -> otaUpdateManager.install()
            SettingsIntent.Logout -> logout()
            is SettingsIntent.SetLanguage -> onLanguageSelected(intent.iso)
        }
    }

    private fun onLanguageSelected(iso: String?) {
        val normalized = iso?.replace('_', '-') // BCP-47
        if (normalized != null) {
            customAppLocale = normalized
            LocalAppLocale.applyLanguage(normalized)
            localRepository.setLanguage(iso = normalized)
        }
    }

    private fun UpdateState.extractRemoteVersionLabel(): String = when (this) {
        is UpdateState.Available -> info.manifest.version.orEmpty()
        is UpdateState.Verifying -> info.manifest.version.orEmpty()
        is UpdateState.ReadyToInstall -> info.manifest.version.orEmpty()
        is UpdateState.Installing -> info.manifest.version.orEmpty()
        else -> ""
    }

    private fun setTheme(state: Boolean) {
        viewModelScope.launch {
            localRepository.setThemeState(state)
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            runCatching {
                PlatformFunctionProvider.clearCache()
                refreshCacheSize()
            }.onSuccess {
                showMessage("Кеш очищено")
            }.onFailure {
                showMessage("Не вдалося очистити кеш: ${it.message.orEmpty()}")
            }
        }
    }

    private suspend fun refreshCacheSize() {
        val size = PlatformFunctionProvider.getCacheSize()
        _uiState.update { it.copy(cacheSizeBytes = size) }
    }

    private fun logout() {
        viewModelScope.launch {
            loginScreenViewModel.onIntent(LoginScreenIntent.LogoutClicked)
            _uiEvent.emit(UIEvents.NotificationAndNavigate("Вихід виконано"))
        }
    }

    private fun showMessage(message: String) = viewModelScope.launch {
        Napier.d(tag = "ShopAddEditScreenViewModel") { message }
        _uiEvent.emit(UIEvents.ShowMessage(message))
    }
}