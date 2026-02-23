package org.bigblackowl.vccadmin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.bigblackowl.vccadmin.data.repository.LocalRepository
import org.bigblackowl.vccadmin.ota.OtaUpdateManager
import org.bigblackowl.vccadmin.ota.UpdateState
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

        viewModelScope.launch { refreshCacheSize() }

        _uiState.update {
            it.copy(
                appBuildLabel = BuildConfig.APP_VERSION,
                isInitialLoading = false
            )
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> setTheme(intent.state)
            is SettingsIntent.SetLogoutDialog -> _uiState.update { it.copy(logoutDialogVisible = intent.visible) }
            is SettingsIntent.SetClearCacheDialog -> _uiState.update { it.copy(clearCacheDialogVisible = intent.visible) }
            SettingsIntent.ClearCache -> clearCache()
            SettingsIntent.CheckUpdates -> otaUpdateManager.check()
            SettingsIntent.DownloadUpdate -> otaUpdateManager.download()
            SettingsIntent.InstallUpdate -> otaUpdateManager.install()
            SettingsIntent.Logout -> logout()
        }
    }

    private fun UpdateState.extractRemoteVersionLabel(): String = when (this) {
        is UpdateState.Available -> info.manifest.version.orEmpty()
        is UpdateState.Verifying -> info.manifest.version.orEmpty()
        is UpdateState.ReadyToInstall -> info.manifest.version.orEmpty()
        is UpdateState.Installing -> info.manifest.version.orEmpty()
        else -> ""
    }

    private fun setTheme(mode: Boolean) {
        viewModelScope.launch {
            // ✅ збереження ThemeMode (краще ніж Boolean)
            localRepository.setThemeState(mode)
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            runCatching {
                PlatformFunctionProvider.clearCache()
                refreshCacheSize()
            }.onSuccess {
                _uiEvent.tryEmit(UIEvents.ShowMessage("Кеш очищено"))
            }.onFailure {
                _uiEvent.tryEmit(UIEvents.ShowMessage("Не вдалося очистити кеш: ${it.message.orEmpty()}"))
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
            _uiEvent.tryEmit(UIEvents.NotificationAndNavigate("Вихід виконано"))
        }
    }
}