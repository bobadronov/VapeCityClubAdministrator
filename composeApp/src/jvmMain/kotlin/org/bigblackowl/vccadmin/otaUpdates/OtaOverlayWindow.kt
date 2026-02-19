package org.bigblackowl.vccadmin.otaUpdates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.InstallDesktop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.AppTheme
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.ota_action_check
import vccadministrator.composeapp.generated.resources.ota_action_close
import vccadministrator.composeapp.generated.resources.ota_action_install
import vccadministrator.composeapp.generated.resources.ota_dialog_title
import vccadministrator.composeapp.generated.resources.ota_download_counter_template
import vccadministrator.composeapp.generated.resources.ota_release_notes_title
import vccadministrator.composeapp.generated.resources.ota_state_available_template
import vccadministrator.composeapp.generated.resources.ota_state_checking
import vccadministrator.composeapp.generated.resources.ota_state_downloading
import vccadministrator.composeapp.generated.resources.ota_state_error_template
import vccadministrator.composeapp.generated.resources.ota_state_idle
import vccadministrator.composeapp.generated.resources.ota_state_installing
import vccadministrator.composeapp.generated.resources.ota_state_no_update
import vccadministrator.composeapp.generated.resources.ota_state_ready_to_install
import vccadministrator.composeapp.generated.resources.ota_state_verifying
import vccadministrator.composeapp.generated.resources.ota_title

@Composable
fun OtaOverlayWindow(
    ota: OtaUpdateManager,
    autoOpen: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val state by ota.state.collectAsState()

    // Ключ “оновлення” (щоб скинути dismiss, коли прийшов інший реліз)
    val updateKey = remember(state) { state.updateKeyOrNull() }

    // ✅ ЄДИНИЙ механізм dismiss, який працює для всіх станів
    var dismissed by remember { mutableStateOf(false) }

    // ✅ Як тільки з’явився інший updateKey — знов показуємо вікно
    LaunchedEffect(updateKey) {
        if (updateKey != null) dismissed = false
    }

    // Якщо апдейт закінчився/нема — закрите
    LaunchedEffect(state) {
        if (state is UpdateState.Idle || state is UpdateState.NoUpdate) dismissed = false
    }

    val visible = remember(state, dismissed, autoOpen) {
        autoOpen && !dismissed && state.shouldShowOverlay()
    }

    if (!visible) return

    val onCheck = remember(ota) { { ota.check() } }
    val onDownload = remember(ota) { { ota.downloadIfAvailable() } }
    val onInstall = remember(ota) { { ota.installIfReadyAndExit() } }

    AppTheme({}) {
        DialogWindow(
            onCloseRequest = { dismissed = true },
            title = stringResource(Res.string.ota_dialog_title),
            state = rememberDialogState(width = 460.dp, height = 320.dp),
            resizable = false,
            alwaysOnTop = false,
            undecorated = true,
            transparent = true,
        ) {
            OtaOverlayContentSimple(
                state = state,
                onCheck = onCheck,
                onDownload = onDownload,
                onInstall = onInstall,
                onClose = { dismissed = true },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun OtaOverlayContentSimple(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui = state.toUiModel()
    val notesScroll = rememberLazyListState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(DefaultValues.Padding.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TitleText(text = stringResource(Res.string.ota_title))

            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = ui.message,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                    )

                    if (!ui.releaseNotes.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 170.dp),
                                state = notesScroll,
                            ) {
                                item {
                                    HelperText(
                                        text = stringResource(Res.string.ota_release_notes_title),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                item {
                                    SelectionContainer {
                                        BodyText(
                                            text = ui.releaseNotes,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Start,
                                            maxLines = Int.MAX_VALUE,
                                        )
                                    }
                                }
                            }

                            DefaultScrollbar(scrollState = notesScroll)
                        }
                    }

                    OtaFooterSimple(ui)
                }
            }

            OtaActionsRowSimple(
                ui = ui,
                onCheck = onCheck,
                onDownload = onDownload,
                onInstall = onInstall,
                onClose = onClose,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OtaFooterSimple(ui: OtaUiModel) {
    when {
        ui.isChecking -> Icon(Icons.Default.Search, null)
        ui.isError -> Icon(Icons.Default.Error, null)
        ui.isInstalling -> Icon(Icons.Default.InstallDesktop, null)
        ui.isReadyToInstall -> Icon(Icons.Default.FileDownloadDone, null)
        ui.isVerifying -> Icon(Icons.Default.Security, null)
        ui.isNoUpdate -> Icon(Icons.Default.DoneAll, null)
        ui.downloadProgress != null -> {
            LinearWavyProgressIndicator(
                progress = { ui.downloadProgress },
                modifier = Modifier.fillMaxWidth(.9f)
            )
            if (ui.downloadText != null) BodyText(ui.downloadText)
        }

        ui.isDownloading -> LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(.9f))
        else -> LoadingIndicator()
    }
}

@Composable
private fun OtaActionsRowSimple(
    ui: OtaUiModel,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onClose: () -> Unit,
) {
    ButtonRowContainer {
        AnimatedVisibility(visible = !ui.isBusy, modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = onCheck) { HelperText(stringResource(Res.string.ota_action_check)) }
        }

        AnimatedVisibility(visible = ui.canDownload, modifier = Modifier.weight(1f)) {
            Button(onClick = onDownload, modifier = Modifier.weight(1f)) {
                HelperText("Download")
            }
        }

        AnimatedVisibility(visible = ui.isReadyToInstall, modifier = Modifier.weight(1f)) {
            Button(onClick = onInstall, modifier = Modifier.weight(1f)) {
                HelperText(stringResource(Res.string.ota_action_install))
            }
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(.15f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            HelperText(stringResource(Res.string.ota_action_close))
        }
    }
}

/** Показуємо overlay тільки коли треба */
private fun UpdateState.shouldShowOverlay(): Boolean = when (this) {
    is UpdateState.Available,
    is UpdateState.Checking,
    is UpdateState.Downloading,
    is UpdateState.Verifying,
    is UpdateState.ReadyToInstall,
    is UpdateState.Installing,
    is UpdateState.Error -> true

    UpdateState.Idle,
    UpdateState.NoUpdate -> false
}

private fun UpdateState.updateKeyOrNull(): String? = when (this) {
    is UpdateState.Available -> info.manifest.tag ?: info.manifest.desktopVersion ?: info.asset.sha256
    is UpdateState.Verifying -> info.manifest.tag ?: info.manifest.desktopVersion ?: info.asset.sha256
    is UpdateState.Installing -> info.manifest.tag ?: info.manifest.desktopVersion ?: info.asset.sha256
    is UpdateState.ReadyToInstall -> info.manifest.tag ?: info.manifest.desktopVersion ?: info.asset.sha256
    else -> null
}

/** UI-модель: один remember(state) => мінімум рекомпозицій */
private data class OtaUiModel(
    val message: String,
    val releaseNotes: String?,
    val isBusy: Boolean,
    val isChecking: Boolean,
    val isDownloading: Boolean,
    val isVerifying: Boolean,
    val isReadyToInstall: Boolean,
    val isInstalling: Boolean,
    val isError: Boolean,
    val isNoUpdate: Boolean,
    val canDownload: Boolean,
    val downloadProgress: Float?,
    val downloadText: String?,
)

@Composable
private fun UpdateState.toUiModel(): OtaUiModel {
    val message = when (this) {
        UpdateState.Idle -> stringResource(Res.string.ota_state_idle)
        UpdateState.Checking -> stringResource(Res.string.ota_state_checking)
        UpdateState.NoUpdate -> stringResource(Res.string.ota_state_no_update)
        is UpdateState.Available -> {
            val version = info.manifest.desktopVersion ?: "—"
            stringResource(Res.string.ota_state_available_template, version)
        }
        is UpdateState.Downloading -> stringResource(Res.string.ota_state_downloading)
        is UpdateState.Verifying -> stringResource(Res.string.ota_state_verifying)
        is UpdateState.ReadyToInstall -> stringResource(Res.string.ota_state_ready_to_install)
        is UpdateState.Installing -> stringResource(Res.string.ota_state_installing)
        is UpdateState.Error -> stringResource(Res.string.ota_state_error_template, message)
    }

    val notes = when (this) {
        is UpdateState.Available -> info.manifest.releaseNotes
        is UpdateState.Verifying -> info.manifest.releaseNotes
        is UpdateState.Installing -> info.manifest.releaseNotes
        is UpdateState.ReadyToInstall -> info.manifest.releaseNotes
        else -> null
    }?.trim().orEmpty().takeIf { it.isNotBlank() }

    val isBusy = this is UpdateState.Checking ||
            this is UpdateState.Downloading ||
            this is UpdateState.Verifying ||
            this is UpdateState.Installing

    val dlProgress = (this as? UpdateState.Downloading)?.progress
    val dlText = (this as? UpdateState.Downloading)?.let { st ->
        val d = st.downloaded
        val t = st.total
        if (!d.isNullOrBlank() || !t.isNullOrBlank()) {
            stringResource(
                Res.string.ota_download_counter_template,
                d.orEmpty(),
                t.orEmpty(),
            )
        } else null
    }

    return OtaUiModel(
        message = message,
        releaseNotes = notes,
        isBusy = isBusy,
        isChecking = this is UpdateState.Checking,
        isDownloading = this is UpdateState.Downloading,
        isVerifying = this is UpdateState.Verifying,
        isReadyToInstall = this is UpdateState.ReadyToInstall,
        isInstalling = this is UpdateState.Installing,
        isError = this is UpdateState.Error,
        isNoUpdate = this is UpdateState.NoUpdate,
        canDownload = this is UpdateState.Available,
        downloadProgress = dlProgress,
        downloadText = dlText,
    )
}
