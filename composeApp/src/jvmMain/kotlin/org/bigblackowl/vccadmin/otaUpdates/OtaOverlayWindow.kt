package org.bigblackowl.vccadmin.otaUpdates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import org.bigblackowl.vccadmin.data.entity.UpdateInfo
import org.bigblackowl.vccadmin.data.repository.FakeBackend
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

    // Автоматично відкривати вікно коли є апдейт / йде процес
    LaunchedEffect(state, autoOpenOnAvailable) {
        if (!autoOpenOnAvailable) return@LaunchedEffect
        visible = when (state) {
            is UpdateState.Available,
            is UpdateState.Checking,
            is UpdateState.Downloading,
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
            SelectionContainer {
                OtaStateBlock(state)
            }

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
                append(state.info.manifest.desktopVersion ?: "—")
                append("\nФайл: ")
                append(state.info.asset.name)
            }

            is UpdateState.Downloading -> "Завантаження…"
            is UpdateState.Verifying -> "Перевірка цілісності (SHA256)…"
            is UpdateState.ReadyToInstall -> "Готово до встановлення"
            is UpdateState.Installing -> "Запуск інсталятора"
            is UpdateState.Error -> "Помилка: ${state.message}"
        }
    }

    // ✅ дістаємо releaseNotes тільки коли є апдейт/верифікація/установка (де є manifest)
    val releaseNotes = remember(state) {
        when (state) {
            is UpdateState.Available -> state.info.manifest.releaseNotes
            is UpdateState.Verifying -> state.info.manifest.releaseNotes
            is UpdateState.Installing -> state.info.manifest.releaseNotes
            else -> null
        }?.trim().orEmpty().takeIf { it.isNotBlank() }
    }

    val notesScroll = rememberScrollState()

    Card(Modifier.fillMaxWidth()) {
        Crossfade(state) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BodyText(
                    message,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                )

                // ✅ Блок release notes
                if (releaseNotes != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 170.dp)
                            .verticalScroll(notesScroll)
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HelperText(
                            text = "Що змінилось:",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BodyText(
                            text = releaseNotes,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            maxLines = Int.MAX_VALUE,
                        )
                    }
                }

                when (state) {
                    UpdateState.Checking -> Icon(Icons.Default.Search, null)

                    is UpdateState.Downloading -> {
                        val p = state.progress
                        val d = state.downloaded
                        val t = state.total
                        if (p != null) {
                            LinearWavyProgressIndicator(
                                progress = { p },
                                modifier = Modifier.fillMaxWidth(.9f)
                            )
                            BodyText("$d / $t")
                        } else {
                            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(.9f))
                            BodyText("$d / $t")
                        }
                    }

                    is UpdateState.Available -> Unit
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

    // ✅ автозавантаження лише 1 раз при вході в Available (а не на кожній рекомпозиції)
    LaunchedEffect(state) {
        if (state is UpdateState.Available) {
            onDownload(state.info)
        }
    }

    ButtonRowContainer {
        AnimatedVisibility(
            visible = !busy,
            modifier = Modifier.weight(1f)
        ) {
            OutlinedButton(onClick = onCheck) { HelperText("Перевірити") }
        }

        AnimatedVisibility(
            visible = state is UpdateState.ReadyToInstall,
            modifier = Modifier.weight(1f)
        ) {
            Button(
                onClick = onInstall,
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
private fun Idle() = PreviewDarkMaterialTheme {
    OtaOverlayContent(
        state = UpdateState.Idle,
        onCheck = {},
        onDownload = {},
        onInstall = {},
        onClose = {},
    )
}

@Preview
@Composable
private fun Checking() = PreviewDarkMaterialTheme {
    OtaOverlayContent(
        state = UpdateState.Checking,
        onCheck = {},
        onDownload = {},
        onInstall = {},
        onClose = {},
    )
}

@Preview
@Composable
private fun NoUpdate() = PreviewDarkMaterialTheme {
    OtaOverlayContent(
        state = UpdateState.NoUpdate,
        onCheck = {},
        onDownload = {},
        onInstall = {},
        onClose = {},
    )
}

@Preview
@Composable
private fun Available() = PreviewDarkMaterialTheme {
    OtaOverlayContent(
        state = UpdateState.Available(FakeBackend.updateInfoWin),
        onCheck = {},
        onDownload = {},
        onInstall = {},
        onClose = {},
    )
}

@Preview
@Composable
private fun Downloading() = PreviewDarkMaterialTheme {
    OtaOverlayContent(
        state = UpdateState.Downloading(
            progress = 0.42f,
            downloaded = "50.4 MB",
            total = "120.0 MB",
        ),
        onCheck = {},
        onDownload = {},
        onInstall = {},
        onClose = {},
    )
}

@Preview
@Composable
private fun Verifying() = PreviewDarkMaterialTheme {
    OtaOverlayContent(
        state = UpdateState.Verifying(FakeBackend.updateInfoWin),
        onCheck = {},
        onDownload = {},
        onInstall = {},
        onClose = {},
    )
}

@Preview
@Composable
private fun ReadyToInstall() = PreviewDarkMaterialTheme {
    OtaOverlayContent(
        state = UpdateState.ReadyToInstall,
        onCheck = {},
        onDownload = {},
        onInstall = {},
        onClose = {},
    )
}

@Preview
@Composable
private fun Error() = PreviewDarkMaterialTheme {
    OtaOverlayContent(
        state = UpdateState.Error("SHA256 mismatch. File may be corrupted."),
        onCheck = {},
        onDownload = {},
        onInstall = {},
        onClose = {},
    )
}