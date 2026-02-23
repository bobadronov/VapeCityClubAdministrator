@file:Suppress("AssignedValueIsNeverRead")

package org.bigblackowl.vccadmin.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallDesktop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.data.entity.UpdateInfo
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
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
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.about_app
import vccadministrator.composeapp.generated.resources.account_label
import vccadministrator.composeapp.generated.resources.cache_size
import vccadministrator.composeapp.generated.resources.cancel
import vccadministrator.composeapp.generated.resources.clear
import vccadministrator.composeapp.generated.resources.clear_cache
import vccadministrator.composeapp.generated.resources.confirm
import vccadministrator.composeapp.generated.resources.confirm_clear_cache_message
import vccadministrator.composeapp.generated.resources.confirm_clear_cache_title
import vccadministrator.composeapp.generated.resources.confirm_logout_message
import vccadministrator.composeapp.generated.resources.confirm_logout_title
import vccadministrator.composeapp.generated.resources.current_app_version
import vccadministrator.composeapp.generated.resources.exit
import vccadministrator.composeapp.generated.resources.ota_state_available_template
import vccadministrator.composeapp.generated.resources.ota_state_checking
import vccadministrator.composeapp.generated.resources.ota_state_downloading
import vccadministrator.composeapp.generated.resources.ota_state_error_template
import vccadministrator.composeapp.generated.resources.ota_state_idle
import vccadministrator.composeapp.generated.resources.ota_state_installing
import vccadministrator.composeapp.generated.resources.ota_state_no_update
import vccadministrator.composeapp.generated.resources.ota_state_ready_to_install
import vccadministrator.composeapp.generated.resources.ota_state_verifying
import vccadministrator.composeapp.generated.resources.theme_setting

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

                is UIEvents.NotificationAndNavigate -> {
                    navigationViewModel.logout()
                    snackbarHostState.showSnackbar(event.message)
                }

                else -> {}
            }
        }
    }

    SettingsContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        goBack = { navigationViewModel.popBackStack() }
    )
}

@Suppress("VariableNeverRead")
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    goBack: () -> Unit,
) {
    var themeMode by LocalThemeMode.current
    val systemDark = rememberIsDarkTheme()
    val listState = rememberLazyListState()

    if (uiState.isInitialLoading) {
        LoadingComponent()
        return
    }

    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.weight(1f).sizeIn(maxWidth = WIDTH_DP_MEDIUM_LOWER_BOUND.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "theme") {
                    ThemeCard(
                        onSetTheme = { theme ->
                            val isDark = when (theme) {
                                ThemeMode.AUTO -> systemDark
                                ThemeMode.DARK -> true
                                ThemeMode.LIGHT -> false
                            }
                            themeMode = theme
                            onIntent(SettingsIntent.SetTheme(isDark))
                        }
                    )
                }

                item(key = "cache") {
                    CacheCard(
                        cacheSizeBytes = uiState.cacheSizeBytes,
                        onClearClick = { onIntent(SettingsIntent.SetClearCacheDialog(true)) }
                    )
                }

                item(key = "updates") {
                    UpdatesCard(
                        updateState = uiState.updateState,
                        currentBuildLabel = uiState.currentAppVersionLabel,
                        onCheck = { onIntent(SettingsIntent.CheckUpdates) },
                        onDownload = { onIntent(SettingsIntent.DownloadUpdate) },
                        onInstall = { onIntent(SettingsIntent.InstallUpdate) },
                    )
                }

                item(key = "account") {
                    AccountCard(
                        onLogoutClick = { onIntent(SettingsIntent.SetLogoutDialog(true)) }
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
            title = { TitleText(stringResource(Res.string.confirm_logout_title)) },
            text = { Text(stringResource(Res.string.confirm_logout_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(SettingsIntent.Logout)
                    onIntent(SettingsIntent.SetLogoutDialog(false))
                }) { BodyText(stringResource(Res.string.confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.SetLogoutDialog(false)) }) {
                    BodyText(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (uiState.clearCacheDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.SetClearCacheDialog(false)) },
            title = { TitleText(stringResource(Res.string.confirm_clear_cache_title)) },
            text = { Text(stringResource(Res.string.confirm_clear_cache_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(SettingsIntent.ClearCache)
                    onIntent(SettingsIntent.SetClearCacheDialog(false))
                }) { BodyText(stringResource(Res.string.clear), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.SetClearCacheDialog(false)) }) {
                    BodyText(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ThemeCard(
    onSetTheme: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var themeMode by LocalThemeMode.current

    OutlinedCardWithLabel(
        label = stringResource(Res.string.theme_setting),
        modifier = modifier.fillMaxWidth(),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            ThemeMode.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = themeMode == item,
                    onClick = {
                        themeMode = item
                        onSetTheme(themeMode)
                    },
                    icon = {
                        SegmentedButtonDefaults.Icon(
                            active = themeMode == item,
                            activeContent = { DefaultIcon(item.icon) }
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                ) {
                    BodyText(stringResource(item.label))
                }
            }
        }
    }
}

@Composable
private fun CacheCard(
    cacheSizeBytes: Long,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cacheSizeText = remember(cacheSizeBytes) {
        AppStringProvider.formatBytesToString(cacheSizeBytes)
    }

    OutlinedCardWithLabel(
        label = stringResource(Res.string.clear_cache),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DefaultIcon(Icons.Default.DiscFull)
            Spacer(Modifier.width(12.dp))

            Text(
                text = "${stringResource(Res.string.cache_size)} $cacheSizeText",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            AnimatedVisibility(
                visible = cacheSizeBytes > 0,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut()
            ) {
                OutlinedButton(onClick = onClearClick) {
                    HelperText(stringResource(Res.string.clear_cache))
                }
            }
        }
    }
}

@Composable
private fun UpdatesCard(
    updateState: UpdateState,
    currentBuildLabel: String,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableVersion = (updateState as? UpdateState.Available)?.info?.manifest?.version ?: "—"
    val errorMsg = (updateState as? UpdateState.Error)?.message.orEmpty()

    val progressText = when (updateState) {
        UpdateState.NotAvailable -> ""
        UpdateState.Idle -> stringResource(Res.string.ota_state_idle)
        UpdateState.Checking -> stringResource(Res.string.ota_state_checking)
        UpdateState.NoUpdate -> stringResource(Res.string.ota_state_no_update)
        is UpdateState.Available -> stringResource(Res.string.ota_state_available_template, availableVersion)
        is UpdateState.Downloading -> stringResource(Res.string.ota_state_downloading)
        is UpdateState.Verifying -> stringResource(Res.string.ota_state_verifying)
        is UpdateState.ReadyToInstall -> stringResource(Res.string.ota_state_ready_to_install)
        is UpdateState.Installing -> stringResource(Res.string.ota_state_installing)
        is UpdateState.Error -> stringResource(Res.string.ota_state_error_template, errorMsg)
    }

    OutlinedCardWithLabel(
        label = stringResource(Res.string.about_app),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.rowItemPadding)
        ) {
            OtaStatusIcon(updateState)

            SelectionContainer(Modifier.weight(1f)) {
                Column {
                    BodyText("${stringResource(Res.string.current_app_version)} $currentBuildLabel")
                    HelperText(progressText)
                }
            }

            // ✅ одна точка анімації для кнопки
            UpdateActionButton(
                updateState = updateState,
                onCheck = onCheck,
                onDownload = onDownload,
                onInstall = onInstall,
            )
        }
    }
}

@Composable
private fun UpdateActionButton(
    updateState: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Мапимо багато станів в маленький enum — менше "шуму" для анімації
    val action = when (updateState) {
        UpdateState.Idle, UpdateState.NoUpdate -> OtaAction.Check
        is UpdateState.Available -> OtaAction.Download
        is UpdateState.ReadyToInstall -> OtaAction.Install
        else -> OtaAction.None
    }

    AnimatedContent(
        targetState = action,
        modifier = modifier,
        transitionSpec = {
            (slideInHorizontally { it } + fadeIn())
                .togetherWith(slideOutHorizontally { it } + fadeOut())
        },
        label = "ota_action_button"
    ) { a ->
        when (a) {
            OtaAction.None -> Spacer(Modifier.width(1.dp)) // або нічого
            OtaAction.Check -> OutlinedButton(onClick = onCheck) { HelperText("Check") }
            OtaAction.Download -> OutlinedButton(onClick = onDownload) { HelperText("Download") }
            OtaAction.Install -> OutlinedButton(onClick = onInstall) { HelperText("Install") }
        }
    }
}

private enum class OtaAction { None, Check, Download, Install }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OtaStatusIcon(state: UpdateState) {
    when (state) {
        UpdateState.Checking -> DefaultIcon(Icons.Default.Search)
        is UpdateState.Error -> DefaultIcon(Icons.Default.Error)
        is UpdateState.Installing -> DefaultIcon(Icons.Default.InstallDesktop)
        is UpdateState.ReadyToInstall -> DefaultIcon(Icons.Default.FileDownloadDone)
        is UpdateState.Verifying -> DefaultIcon(Icons.Default.Security)
        UpdateState.NoUpdate -> DefaultIcon(Icons.Default.DoneAll)

        is UpdateState.Downloading -> {
            // якщо маєш прогрес — підстав сюди
            val p = state.toUiModel().downloadProgress // якщо в state немає progress поля
            if (p != null) {
                CircularWavyProgressIndicator(
                    progress = { p },
                    modifier = Modifier.size(24.dp)
                )
            } else {
                CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        else -> DefaultIcon(Icons.Default.Info)
    }
}

@Composable
private fun AccountCard(
    onLogoutClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCardWithLabel(
        label = stringResource(Res.string.account_label),
        modifier = modifier.fillMaxWidth(),
        onTap = onLogoutClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DefaultIcon(Icons.AutoMirrored.Filled.Logout)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.exit),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview
@Composable
private fun Preview_SettingsContent_Normal() = PreviewDarkMaterialTheme {

    fun demoStates(demoInfo: UpdateInfo) = listOf(
        UpdateState.NotAvailable,
        UpdateState.Idle,
        UpdateState.Checking,
        UpdateState.NoUpdate,
        UpdateState.Available(demoInfo),
        UpdateState.Downloading(progress = 0.42f, total = "120 MB", downloaded = "50 MB"),
        UpdateState.Verifying(demoInfo),
        UpdateState.ReadyToInstall(demoInfo),
        UpdateState.Installing(demoInfo),
        UpdateState.Error("Network error")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(demoStates(FakeBackend.updateInfoWin)) { state ->
            val buildLabel = when (state) {
                is UpdateState.Available -> state.info.manifest.version.orEmpty()
                is UpdateState.Verifying -> state.info.manifest.version.orEmpty()
                is UpdateState.ReadyToInstall -> state.info.manifest.version.orEmpty()
                is UpdateState.Installing -> state.info.manifest.version.orEmpty()
                else -> BuildConfig.APP_VERSION // або ""
            }

            UpdatesCard(updateState = state, currentBuildLabel = buildLabel, onCheck = {}, onDownload = {}, onInstall = {})
        }
    }
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
            updateState = UpdateState.Downloading(progress = .2f),
        ), onIntent = {}, goBack = {})
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun Preview_SettingsContent_ClearCacheDialogPC() = PreviewDarkMaterialTheme {
    SettingsContent(
        uiState = SettingsUiState(
            isInitialLoading = false,
            isDarkEffective = true,
            cacheSizeBytes = 42L * 1024 * 1024,
            newAppVersionLabel = "1.2.3",
            appBuildLabel = "123",
            updateState = UpdateState.Downloading(),
        ), onIntent = {}, goBack = {})
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
        ), onIntent = {}, goBack = {})
}

@Preview
@Composable
private fun Preview_SettingsContent_WasmCacheUnknown() = PreviewDarkMaterialTheme {
    SettingsContent(
        uiState = SettingsUiState(
            isInitialLoading = false, isDarkEffective = false, cacheSizeBytes = -1L, // як у wasm
            clearCacheDialogVisible = true,
        ), onIntent = {}, goBack = {})
}

@Preview
@Composable
private fun Preview_SettingsContent_Loading() = PreviewDarkMaterialTheme {
    SettingsContent(uiState = SettingsUiState(isInitialLoading = true), onIntent = {}, goBack = {})
}

@Preview
@Composable
private fun CacheCardPreview() = PreviewDarkMaterialTheme {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CacheCard(0L, {})
        }
        item {
            CacheCard(2650L, {})
        }
    }
}