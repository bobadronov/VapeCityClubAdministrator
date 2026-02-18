package org.bigblackowl.vccadmin.otaUpdates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DownloadDone
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.flow.StateFlow
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.AppTheme
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText


@Composable
fun OtaOverlayWindow(
    ota: OtaUpdateManager,
    stateFlow: StateFlow<UpdateState>,
    autoOpenOnAvailable: Boolean = true,
) {
    val state by stateFlow.collectAsState()

    var visible by remember { mutableStateOf(true) }

    // Автовідкривати вікно коли є апдейт / йде процес
    LaunchedEffect(state, autoOpenOnAvailable) {
        if (!autoOpenOnAvailable) return@LaunchedEffect
        visible = when (state) {
            is UpdateState.Available,
            is UpdateState.Checking,
            is UpdateState.Downloading,
            is UpdateState.Downloaded,
            is UpdateState.Verifying,
            is UpdateState.ReadyToInstall,
            is UpdateState.Installing,
            is UpdateState.Error -> true

            is UpdateState.NoUpdate -> false
            else -> visible
        }
    }

    if (!visible) return

    AppTheme({}) {
        DialogWindow(
            onCloseRequest = { visible = false },
            title = "Update",
            state = rememberDialogState(width = 460.dp, height = 320.dp),
            resizable = false,
            alwaysOnTop = true,
            undecorated = true,
            transparent = true,
        ) {
            OtaOverlayContent(
                state = state,
                onCheck = { ota.checkOnAppStart() },
                onDownload = { ota.downloadUpdate(it) },
                onInstall = { ota.startInstall() },
                onClose = { visible = false },
            )
        }
    }
}

@Composable
fun OtaOverlayContent(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (UpdateInfo) -> Unit,
    onInstall: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TitleText(text = "Оновлення програми")

            OtaStateBlock(state)

            OtaActionsRow(
                state = state,
                onCheck = onCheck,
                onDownload = onDownload,
                onInstall = onInstall,
                onClose = onClose
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OtaStateBlock(state: UpdateState) {
    val message = remember(state) {
        when (state) {
            UpdateState.Idle -> "Очікування…"
            UpdateState.Checking -> "Перевіряємо оновлення…"
            UpdateState.NoUpdate -> "Оновлень немає."
            is UpdateState.Available -> buildString {
                append("Доступне оновлення: ")
                append(state.info.manifest.desktopVersion)
                append("\nФайл: ")
                append(state.info.asset.name)
            }

            is UpdateState.Downloading -> "Завантаження…"
            is UpdateState.Downloaded -> "Завантажено"
            is UpdateState.Verifying -> "Перевірка цілісності (SHA256)…"
            is UpdateState.ReadyToInstall -> "Готово до встановлення"
            is UpdateState.Installing -> "Запуск інсталятора"
            is UpdateState.Error -> "Помилка: ${state.message}"
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Crossfade(state) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BodyText(
                    message,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                )

                when (state) {
                    UpdateState.Checking -> Icon(Icons.Default.Search, null)
                    is UpdateState.Downloaded -> Icon(Icons.Default.DownloadDone, null)
                    is UpdateState.Downloading -> {
                        val p = state.progress
                        val d = state.downloaded
                        val t = state.total
                        if (p != null) {
//                            BodyText("${(p * 100).toInt()}%")
                            LinearWavyProgressIndicator(
                                progress = { p },
                                modifier = Modifier.fillMaxWidth(.9f)
                            )
                            BodyText("$d / $t")

                        } else {
                            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(.9f))
                            BodyText("$d / $t") // total може бути "—"
                        }
                    }

                    is UpdateState.Error -> Icon(Icons.Default.Error, null)
                    is UpdateState.Installing -> Icon(Icons.Default.InstallDesktop, null)
                    UpdateState.NoUpdate -> Icon(Icons.Default.DoneAll, null)
                    UpdateState.ReadyToInstall -> Icon(Icons.Default.FileDownloadDone, null)
                    is UpdateState.Verifying -> Icon(Icons.Default.Security, null)
                    else -> LoadingIndicator(modifier = Modifier)
                }
            }
        }
    }
}

@Composable
private fun OtaActionsRow(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (UpdateInfo) -> Unit,
    onInstall: () -> Unit,
    onClose: () -> Unit,
) {
    val busy = state is UpdateState.Checking ||
            state is UpdateState.Downloading ||
            state is UpdateState.Verifying ||
            state is UpdateState.Installing

    when (state) {
        is UpdateState.Available -> onDownload(state.info)

        else -> {}
    }

    ButtonRowContainer {
        AnimatedVisibility(
            visible = !busy,
            modifier = Modifier.weight(1f)
        ) {
            OutlinedButton(
                onClick = onCheck,
            ) { HelperText("Перевірити") }
        }

        AnimatedVisibility(
            visible = state is UpdateState.ReadyToInstall,
            modifier = Modifier.weight(1f)
        ) {
            Button(
                onClick = { onInstall() },
                modifier = Modifier.weight(1f)
            ) { HelperText("Встановити") }
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(.15f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) { HelperText("Закрити") }
    }
}

@Preview
@Composable
private fun OtaOverlayPreviewContent() = PreviewDarkMaterialTheme {
    val manifest = UpdateManifest(
        tag = "v1.2.3",
        publishedAt = "2026-02-15T12:34:56Z",
        versionName = "1.2.3",
        desktopVersion = "1.2.303",
        assets = Assets(
            windows = AssetInfo(
                name = "VCC-Admin-Setup-1.2.303.exe",
                url = "https://example.com/VCC-Admin-Setup-1.2.303.exe",
                size = 120L * 1024 * 1024,
                sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
            ),
            macos = AssetInfo(
                name = "VCC-Admin-1.2.303.dmg",
                url = "https://example.com/VCC-Admin-1.2.303.dmg",
                size = 140L * 1024 * 1024,
                sha256 = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
            ),
            linux = AssetInfo(
                name = "vcc-admin_1.2.303_amd64.deb",
                url = "https://example.com/vcc-admin_1.2.303_amd64.deb",
                size = 110L * 1024 * 1024,
                sha256 = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"
            )
        )
    )

    val updateInfoWin = UpdateInfo(
        manifest = manifest,
        asset = manifest.assets.windows!!
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OtaOverlayContent(
            state = UpdateState.Idle,
            onCheck = {},
            onDownload = {},
            onInstall = {},
            onClose = {},
        )
//
//        OtaOverlayContent(
//            state = UpdateState.Checking,
//            onCheck = {},
//            onDownload = {},
//            onInstall = {},
//            onClose = {},
//        )
//
//        OtaOverlayContent(
//            state = UpdateState.NoUpdate,
//            onCheck = {},
//            onDownload = {},
//            onInstall = {},
//            onClose = {},
//        )
//
//        OtaOverlayContent(
//            state = UpdateState.Available(updateInfoWin),
//            onCheck = {},
//            onDownload = {},
//            onInstall = {},
//            onClose = {},
//        )
//
//        OtaOverlayContent(
//            state = UpdateState.Downloading(
//                progress = .5f,
//                total = "120 MB",
//                downloaded = "60 MB"
//            ),
//            onCheck = {},
//            onDownload = {},
//            onInstall = {},
//            onClose = {},
//        )
//
//        // якщо хочеш показати “без Content-Length”
//        OtaOverlayContent(
//            state = UpdateState.Downloading(
//                progress = null,
//                total = "—",
//                downloaded = "37.2 MB"
//            ),
//            onCheck = {},
//            onDownload = {},
//            onInstall = {},
//            onClose = {},
//        )
//
//        OtaOverlayContent(
//            state = UpdateState.Downloaded(
//                fileName = updateInfoWin.asset.name,
//                info = updateInfoWin
//            ),
//            onCheck = {},
//            onDownload = {},
//            onInstall = {},
//            onClose = {},
//        )

        OtaOverlayContent(
            state = UpdateState.Verifying(updateInfoWin),
            onCheck = {},
            onDownload = {},
            onInstall = {},
            onClose = {},
        )

        OtaOverlayContent(
            state = UpdateState.ReadyToInstall,
            onCheck = {},
            onDownload = {},
            onInstall = {},
            onClose = {},
        )

        OtaOverlayContent(
            state = UpdateState.Installing(updateInfoWin),
            onCheck = {},
            onDownload = {},
            onInstall = {},
            onClose = {},
        )

        OtaOverlayContent(
            state = UpdateState.Error(
                message = "SHA256 mismatch. File may be corrupted.",
                cause = IllegalStateException("Digest mismatch")
            ),
            onCheck = {},
            onDownload = {},
            onInstall = {},
            onClose = {},
        )
    }
}
