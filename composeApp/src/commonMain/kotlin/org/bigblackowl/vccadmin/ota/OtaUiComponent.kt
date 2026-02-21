package org.bigblackowl.vccadmin.ota

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallDesktop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.CancelButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.download
import vccadministrator.composeapp.generated.resources.ota_action_check
import vccadministrator.composeapp.generated.resources.ota_action_install
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
fun OtaUiComponent() {
    val otaUpdateManager: OtaUpdateManager = koinInject()

    LaunchedEffect(Unit) { otaUpdateManager.check() }

    val state by otaUpdateManager.state.collectAsState()

    OtaTopBarActionWithDialog(
        state = state,
        onCheck = { otaUpdateManager.check() },
        onDownload = { otaUpdateManager.download() },
        onInstall = { otaUpdateManager.install() },
    )
}

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun OtaTopBarActionWithDialog(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    hideNoUpdateDelayMs: Long = 1200L,
) {
    var dialogOpen by remember { mutableStateOf(true) }
    var suppressNoUpdate by remember { mutableStateOf(false) }

    // ref: не викликає рекомпозиції
    val prevRef = remember { arrayOfNulls<UpdateState>(1) }

    val onCheckState = rememberUpdatedState(onCheck)
    val onDownloadState = rememberUpdatedState(onDownload)
    val onInstallState = rememberUpdatedState(onInstall)

    LaunchedEffect(state) {
        val prev = prevRef[0]
        prevRef[0] = state

        when (state) {
            UpdateState.Checking -> suppressNoUpdate = false

            UpdateState.NoUpdate -> {
                if (prev === UpdateState.Checking) {
                    delay(hideNoUpdateDelayMs)
                    suppressNoUpdate = true
                    dialogOpen = false
                } else {
                    suppressNoUpdate = false
                }
            }

            UpdateState.Idle,
            UpdateState.NotAvailable -> {
                suppressNoUpdate = false
                dialogOpen = false
            }

            // всі “робочі” стани не ховаємо
            is UpdateState.Available,
            is UpdateState.Downloading,
            is UpdateState.Verifying,
            is UpdateState.ReadyToInstall,
            is UpdateState.Installing,
            is UpdateState.Error -> suppressNoUpdate = false
        }
    }

    val showButton by remember {
        derivedStateOf {
            state.shouldShowTopBarAction() &&
                    !(state === UpdateState.NoUpdate && suppressNoUpdate)
        }
    }

    AnimatedVisibility(visible = showButton) {
        OtaTopBarStatusButton(state = state, onClick = { dialogOpen = true })
    }

    if (dialogOpen) {
        BasicAlertDialog(onDismissRequest = { dialogOpen = false }) {
            OtaOverlayContent(
                state = state,
                onCheck = { onCheckState.value() },
                onDownload = { onDownloadState.value() },
                onInstall = { onInstallState.value() },
                onClose = { dialogOpen = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OtaTopBarStatusButton(
    state: UpdateState,
    onClick: () -> Unit,
) {
    OutlinedIconButton(
        onClick = onClick,
        border = BorderStroke(0.dp, Color.Transparent),
    ) {
        when (state) {
            UpdateState.Checking -> DefaultIcon(Icons.Default.Search)
            UpdateState.NoUpdate -> DefaultIcon(Icons.Default.DoneAll)

            is UpdateState.Error -> DefaultIcon(Icons.Default.Error)
            is UpdateState.Installing -> DefaultIcon(Icons.Default.InstallDesktop)
            is UpdateState.ReadyToInstall -> DefaultIcon(Icons.Default.FileDownloadDone)
            is UpdateState.Verifying -> DefaultIcon(Icons.Default.Security)
            is UpdateState.Available -> DefaultIcon(Icons.Default.SystemUpdate)

            is UpdateState.Downloading -> {
                val p = state.progress
                if (p != null) {
                    CircularWavyProgressIndicator(progress = { p })
                } else {
                    CircularWavyProgressIndicator()
                }
            }

            // ці стани кнопку не показують (але якщо показали — просто нічого)
            UpdateState.Idle,
            UpdateState.NotAvailable -> Unit
        }
    }
}

@Composable
private fun OtaOverlayContent(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onClose: () -> Unit,
) {

    val ui = state.toUiModel()

    Surface(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .heightIn(max = 620.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(DefaultValues.Padding.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TitleText(text = stringResource(Res.string.ota_title))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
            ) {
                Column(
                    modifier = Modifier.padding(DefaultValues.Padding.cardContentPadding),
                    verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = ui.message,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                    )

                    if (!ui.releaseNotes.isNullOrBlank()) {
                        val notesScroll = rememberScrollState()
                        // ✅ без Row/LazyColumn, щоб не викликати intrinsic на SubcomposeLayout
                        HelperText(
                            text = stringResource(Res.string.ota_release_notes_title),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(notesScroll)
                        ) {
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

                    OtaFooterSimple(ui)
                }
            }

            OtaActionsRowSimple(
                isBusy = !ui.isBusy,
                canDownload = ui.canDownload,
                isReadyToInstall = ui.isReadyToInstall,
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
fun OtaFooterSimple(ui: OtaUiModel) {
    when {
        ui.isChecking -> DefaultIcon(Icons.Default.Search)
        ui.isError -> DefaultIcon(Icons.Default.Error)
        ui.isInstalling -> DefaultIcon(Icons.Default.InstallDesktop)
        ui.isReadyToInstall -> DefaultIcon(Icons.Default.FileDownloadDone)
        ui.isVerifying -> DefaultIcon(Icons.Default.Security)
        ui.isNoUpdate -> DefaultIcon(Icons.Default.DoneAll)

        ui.downloadProgress != null -> {
            LinearWavyProgressIndicator(progress = { ui.downloadProgress }, modifier = Modifier.fillMaxWidth(.9f))
            if (ui.downloadText != null) BodyText(ui.downloadText)
        }

        ui.isDownloading -> LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(.9f))
        else -> DefaultIcon(Icons.Default.Info)
    }
}

@Composable
private fun OtaActionsRowSimple(
    isBusy: Boolean,
    canDownload: Boolean,
    isReadyToInstall: Boolean,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onClose: () -> Unit,
) {
    val showLabel: Boolean = isWideScreen()
    ButtonRowContainer {
        AnimatedVisibility(visible = isBusy, modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = onCheck) {
                DefaultIcon(Icons.Default.Refresh)
                if (showLabel) {
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    HelperText(stringResource(Res.string.ota_action_check))
                }
            }
        }

        AnimatedVisibility(visible = canDownload, modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = onDownload, modifier = Modifier.weight(1f)) {
                DefaultIcon(Icons.Default.Download)
                if (showLabel) {
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    HelperText(stringResource(Res.string.download))
                }
            }
        }

        AnimatedVisibility(visible = isReadyToInstall, modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = onInstall, modifier = Modifier.weight(1f)) {
                DefaultIcon(Icons.Default.InstallDesktop)
                if (showLabel) {
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    HelperText(stringResource(Res.string.ota_action_install))
                }
            }
        }
        CancelButton(
            modifier = Modifier.weight(1f),
        ) { onClose() }

    }
}

data class OtaUiModel(
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
fun UpdateState.toUiModel(): OtaUiModel {
    val message = when (this) {
        UpdateState.NotAvailable -> "" // або окремий string
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

/** Показувати іконку в TopBar коли є процес/апдейт/помилка */
private fun UpdateState.shouldShowTopBarAction(): Boolean = when (this) {
    UpdateState.NotAvailable -> false
    UpdateState.Idle -> false

    UpdateState.Checking -> true
    UpdateState.NoUpdate -> true

    is UpdateState.Available -> true
    is UpdateState.Downloading -> true
    is UpdateState.Verifying -> true
    is UpdateState.ReadyToInstall -> true
    is UpdateState.Installing -> true
    is UpdateState.Error -> true
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun OtaTopBarActionPreview_Menu() = PreviewDarkMaterialTheme(topBar = {
    val state = UpdateState.ReadyToInstall(FakeBackend.updateInfoWin)
    TopAppBar(title = {}, actions = {
        OtaTopBarActionWithDialog(
            state = state,
            onCheck = { },
            onDownload = { },
            onInstall = {},
        )
    })
}) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        @Composable
        fun Item(title: String, state: UpdateState) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HelperText(title, modifier = Modifier.weight(1f))
                OtaTopBarStatusButton(
                    state = state,
                    onClick = { }
                )
            }
        }

        Item("NotAvailable", UpdateState.NotAvailable)
        Item("Idle", UpdateState.Idle)
        Item("Checking", UpdateState.Checking)
        Item("NoUpdate", UpdateState.NoUpdate)

        Item("Available (win)", UpdateState.Available(FakeBackend.updateInfoWin))

        Item("Downloading (null)", UpdateState.Downloading(progress = null, total = "120 MB", downloaded = "—"))
        Item("Downloading (35%)", UpdateState.Downloading(progress = 0.35f, total = "120 MB", downloaded = "42 MB"))

        Item("Verifying (win)", UpdateState.Verifying(FakeBackend.updateInfoWin))
        Item("ReadyToInstall (win)", UpdateState.ReadyToInstall(FakeBackend.updateInfoWin))
        Item("Installing (win)", UpdateState.Installing(FakeBackend.updateInfoWin))

        Item("Error", UpdateState.Error(message = "Network error"))
    }
}