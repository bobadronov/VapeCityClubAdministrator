package org.bigblackowl.vccadmin.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.data.repository.LocalRepository
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.ota.OtaFooterSimple
import org.bigblackowl.vccadmin.ota.OtaUpdateManager
import org.bigblackowl.vccadmin.ota.UpdateState
import org.bigblackowl.vccadmin.ota.toUiModel
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.LocalThemeMode
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.ThemeMode
import org.bigblackowl.vccadmin.theme.rememberIsDarkTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.loading.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.utils.PlatformFunctionProvider
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.aboutApp
import vccadministrator.composeapp.generated.resources.accountLabel
import vccadministrator.composeapp.generated.resources.appVersion
import vccadministrator.composeapp.generated.resources.cacheSize
import vccadministrator.composeapp.generated.resources.cancel
import vccadministrator.composeapp.generated.resources.clear
import vccadministrator.composeapp.generated.resources.clearCache
import vccadministrator.composeapp.generated.resources.confirm
import vccadministrator.composeapp.generated.resources.confirmClearCacheMessage
import vccadministrator.composeapp.generated.resources.confirmClearCacheTitle
import vccadministrator.composeapp.generated.resources.confirmLogoutMessage
import vccadministrator.composeapp.generated.resources.confirmLogoutTitle
import vccadministrator.composeapp.generated.resources.exit
import vccadministrator.composeapp.generated.resources.theme_setting
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: SettingsScreenViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(null)

    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    SettingsContent(
        uiState = uiState, onIntent = viewModel::onIntent, goBack = { navigationViewModel.popBackStack() })
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    goBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    var themeMode by LocalThemeMode.current
    val systemDark = rememberIsDarkTheme()

    if (uiState.isInitialLoading) {
        LoadingComponent()
        return
    }

    Column(
        modifier = Modifier
            .padding(DefaultValues.Padding.mainBoxPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ThemeSettingCard(
                        mode = themeMode,
                        onModeChange = {
                            themeMode = it
                            onIntent(
                                SettingsIntent.SetTheme(
                                    when (it) {
                                        ThemeMode.AUTO -> systemDark
                                        ThemeMode.DARK -> true
                                        ThemeMode.LIGHT -> false
                                    }
                                )
                            )
                        }
                    )
                }

                item {
                    SettingCard(
                        label = stringResource(Res.string.clearCache),
                        icon = { DefaultIcon(Icons.Default.DiscFull) },
                        description = "${stringResource(Res.string.cacheSize)} ${formatBytesToString(uiState.cacheSizeBytes)}",
                        onTap = { onIntent(SettingsIntent.SetClearCacheDialog(true)) }
                    )
                }

                item {
                    UpdatesSettingCard(
                        versionLabel = uiState.newAppVersionLabel,
                        buildLabel = uiState.appBuildLabel,
                        updateState = uiState.updateState,
                        onCheck = { onIntent(SettingsIntent.CheckUpdates) },
                        onDownload = { onIntent(SettingsIntent.DownloadUpdate) },
                        onInstall = { onIntent(SettingsIntent.InstallUpdate) },

                    )
                }

                item {
                    SettingCard(
                        label = stringResource(Res.string.accountLabel),
                        icon = { DefaultIcon(Icons.AutoMirrored.Filled.Logout) },
                        description = stringResource(Res.string.exit),
                        onTap = { onIntent(SettingsIntent.SetLogoutDialog(true)) }
                    )
                }
            }

            DefaultScrollbar(scrollState = listState)
        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton { goBack() }
            }
        }
    }

    if (uiState.logoutDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.SetLogoutDialog(false)) },
            title = { Text(stringResource(Res.string.confirmLogoutTitle)) },
            text = { Text(stringResource(Res.string.confirmLogoutMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(SettingsIntent.Logout)
                    onIntent(SettingsIntent.SetLogoutDialog(false))
                }) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.SetLogoutDialog(false)) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (uiState.clearCacheDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.SetClearCacheDialog(false)) },
            title = { Text(stringResource(Res.string.confirmClearCacheTitle)) },
            text = { Text(stringResource(Res.string.confirmClearCacheMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(SettingsIntent.ClearCache)
                    onIntent(SettingsIntent.SetClearCacheDialog(false))
                }) { Text(stringResource(Res.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.SetClearCacheDialog(false)) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ThemeSettingCard(
    mode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
) {
    OutlinedCardWithLabel(label = stringResource(Res.string.theme_setting), modifier = Modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            ThemeMode.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = mode == item,
                    onClick = { onModeChange(item) },
                    icon = { SegmentedButtonDefaults.Icon(mode == item, { DefaultIcon(item.icon) }) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                ) {
                    HelperText(stringResource(item.label))
                }
            }
        }
    }
}

@Composable
private fun UpdatesSettingCard(
    versionLabel: String,
    buildLabel: String,
    updateState: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val (statusText, trailing) = remember(updateState) {
        when (updateState) {
            UpdateState.NotAvailable -> "Оновлення недоступні" to TrailingAction.Check
            UpdateState.Idle -> "Готово" to TrailingAction.Check
            UpdateState.Checking -> "Перевірка…" to TrailingAction.None
            UpdateState.NoUpdate -> "Оновлень немає" to TrailingAction.Check

            is UpdateState.Available -> "Є оновлення: ${updateState.info.manifest.desktopVersion ?: "—"}" to TrailingAction.Download
            is UpdateState.Downloading -> "Завантаження…" to TrailingAction.None
            is UpdateState.Verifying -> "Перевірка файлу…" to TrailingAction.None
            is UpdateState.ReadyToInstall -> "Готово до встановлення" to TrailingAction.Install
            is UpdateState.Installing -> "Встановлення…" to TrailingAction.None

            is UpdateState.Error -> "Помилка: ${updateState.message}" to TrailingAction.Check
        }
    }

    OutlinedCardWithLabel(label = stringResource(Res.string.aboutApp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ✅ твоя функція іконок (як у фрагменті)
            OtaFooterSimple(updateState.toUiModel())

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "${stringResource(Res.string.appVersion)} $versionLabel ($buildLabel)\n$statusText",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // ✅ вторинна дія (check/download/install)
            when (trailing) {
                TrailingAction.None -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                TrailingAction.Check -> {
                    TextButton(onClick = onCheck) {
                        Text("Check")
                    }
                }

                TrailingAction.Download -> {
                    TextButton(onClick = onDownload) {
                        Text("Download")
                    }
                }

                TrailingAction.Install -> {
                    TextButton(onClick = onInstall) {
                        Text("Install")
                    }
                }
            }
        }
    }
}

private enum class TrailingAction { None, Check, Download, Install }

@Composable
private fun SettingCard(
    label: String,
    icon: @Composable () -> Unit,
    description: String? = null,
    onTap: (() -> Unit)? = null,
) {
    OutlinedCardWithLabel(
        label = label,
        modifier = Modifier.fillMaxWidth(),
        onTap = { onTap?.invoke() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                if (!description.isNullOrBlank()) {
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun formatBytesToString(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
    val value = bytes / 1024.0.pow(digitGroups)
    val factor = 10.0.pow(2)
    return "${round(value * factor) / factor} ${units[digitGroups]}"
}


class SettingsScreenViewModel(
    private val localRepository: LocalRepository,
    private val otaUpdateManager: OtaUpdateManager,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UIEvents>(extraBufferCapacity = 1)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            otaUpdateManager.state.collect { state ->
                _uiState.update {
                    it.copy(
                        updateState = state,
                        newAppVersionLabel = state.availableVersionLabel()
                    )
                }
            }
        }

        viewModelScope.launch {
            refreshCacheSize()
        }

        _uiState.update {
            it.copy(
                appBuildLabel = BuildConfig.APP_VERSION, // або BuildConfig.BUILD_TYPE / git hash
                isInitialLoading = false
            )
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> setTheme(intent.state)

            is SettingsIntent.SetLogoutDialog ->
                _uiState.update { it.copy(logoutDialogVisible = intent.visible) }

            is SettingsIntent.SetClearCacheDialog ->
                _uiState.update { it.copy(clearCacheDialogVisible = intent.visible) }

            SettingsIntent.ClearCache -> clearCache()

            SettingsIntent.CheckUpdates -> otaUpdateManager.check()
            SettingsIntent.DownloadUpdate -> otaUpdateManager.download()
            SettingsIntent.InstallUpdate -> otaUpdateManager.install()

            SettingsIntent.Logout -> logout()
        }
    }

    private fun UpdateState.availableVersionLabel(): String = when (this) {
        is UpdateState.Available -> with(info.manifest) {
            androidVersion
                ?: versionName
                ?: desktopVersion
                ?: ""
        }

        is UpdateState.Verifying -> with(info.manifest) {
            androidVersion ?: versionName ?: desktopVersion ?: ""
        }

        is UpdateState.ReadyToInstall -> with(info.manifest) {
            androidVersion ?: versionName ?: desktopVersion ?: ""
        }

        is UpdateState.Installing -> with(info.manifest) {
            androidVersion ?: versionName ?: desktopVersion ?: ""
        }

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

    private fun refreshCacheSize() {
        val size = PlatformFunctionProvider.getCacheSize()
        _uiState.update { it.copy(cacheSizeBytes = size) }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiEvent.tryEmit(UIEvents.ShowMessage("Вихід виконано"))
        }
    }
}

sealed interface SettingsIntent {
    data class SetTheme(val state: Boolean) : SettingsIntent

    data class SetLogoutDialog(val visible: Boolean) : SettingsIntent
    data class SetClearCacheDialog(val visible: Boolean) : SettingsIntent

    object ClearCache : SettingsIntent

    object CheckUpdates : SettingsIntent
    object DownloadUpdate : SettingsIntent
    object InstallUpdate : SettingsIntent

    object Logout : SettingsIntent
}

data class SettingsUiState(
    val isInitialLoading: Boolean = true,
    val isDarkEffective: Boolean = false,
    val cacheSizeBytes: Long = 0L,
    val currentAppVersionLabel: String = BuildConfig.APP_VERSION,
    val newAppVersionLabel: String = "", // on init set from OtaUpdateManager.state
    val appBuildLabel: String = "",
    val updateState: UpdateState = UpdateState.NotAvailable,
    val logoutDialogVisible: Boolean = false,
    val clearCacheDialogVisible: Boolean = false,
)

@Preview
@Composable
private fun Preview_SettingsContent_Normal() = PreviewDarkMaterialTheme {
    SettingsContent(
        uiState = SettingsUiState(
            isInitialLoading = false,
            isDarkEffective = false,
            cacheSizeBytes = 128L * 1024 * 1024, // 128MB
            newAppVersionLabel = "1.2.3",
            updateState = UpdateState.Available(FakeBackend.updateInfoWin),
            logoutDialogVisible = false,
            clearCacheDialogVisible = false
        ),
        onIntent = {},
        goBack = {}
    )
}

@Preview
@Composable
private fun Preview_SettingsContent_ClearCacheDialog() = PreviewDarkMaterialTheme {
    SettingsContent(
        uiState = SettingsUiState(
            isInitialLoading = false,
            isDarkEffective = true,
            cacheSizeBytes = 42L * 1024 * 1024,
            newAppVersionLabel = "1.2.3",
            appBuildLabel = "123",
            updateState = UpdateState.NoUpdate,
            clearCacheDialogVisible = true
        ),
        onIntent = {},
        goBack = {}
    )
}

@Preview
@Composable
private fun Preview_SettingsContent_LogoutDialog() = PreviewDarkMaterialTheme {
    SettingsContent(
        uiState = SettingsUiState(
            isInitialLoading = false,
            isDarkEffective = false,
            cacheSizeBytes = 6L * 1024 * 1024,
            newAppVersionLabel = "1.2.3",
            appBuildLabel = "123",
            updateState = UpdateState.NotAvailable,
            logoutDialogVisible = true
        ),
        onIntent = {},
        goBack = {}
    )
}

@Preview
@Composable
private fun Preview_SettingsContent_WasmCacheUnknown() = PreviewDarkMaterialTheme {
    SettingsContent(
        uiState = SettingsUiState(
            isInitialLoading = false,
            isDarkEffective = false,
            cacheSizeBytes = -1L, // як у wasm
            newAppVersionLabel = "1.2.3",
            appBuildLabel = "123",
            updateState = UpdateState.Idle
        ),
        onIntent = {},
        goBack = {}
    )
}

@Preview
@Composable
private fun Preview_SettingsContent_Loading() = PreviewDarkMaterialTheme {
    SettingsContent(
        uiState = SettingsUiState(isInitialLoading = true),
        onIntent = {},
        goBack = {}
    )
}