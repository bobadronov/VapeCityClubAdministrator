// /src/main/kotlin/org/bigblackowl/vccadmin/Main.kt
package org.bigblackowl.vccadmin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Maximize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.SingleInstanceManager
import io.github.vinceglb.filekit.FileKit
import org.bigblackowl.vccadmin.di.coreModules
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.exit
import vccadministrator.composeapp.generated.resources.main_logo
import vccadministrator.composeapp.generated.resources.show_pc_window
import vccadministrator.composeapp.generated.resources.stay_online_on_close
import java.awt.Dimension
import kotlin.system.exitProcess

fun main() =
//    try {
    application {
        FileKit.init(appId = BuildConfig.APP_NAME)

        KoinApplication(
            configuration = koinConfiguration { modules(coreModules) }
        ) {
            val localRepository: LocalRepository = koinInject()

            var awtWindowRef by remember { mutableStateOf<java.awt.Window?>(null) }
            var isWindowVisible by remember { mutableStateOf(true) }

            var minimizeToTrayOnClose by remember { mutableStateOf(localRepository.getWindowClosableState()) }

            // Allows triggering tray disposal from outside Tray scope
            var trayDisposer by remember { mutableStateOf<(() -> Unit)?>(null) }

            val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

            fun bringToFront() {
                isWindowVisible = true
                windowState.isMinimized = false
                awtWindowRef?.apply {
                    isVisible = true
                    toFront()
                    requestFocus()
                }
            }

            val isSingleInstance = SingleInstanceManager.isSingleInstance(
                onRestoreRequest = { bringToFront() }
            )

            if (!isSingleInstance) {
                exitApplication()
                return@KoinApplication
            }

            val appIconPainter = painterResource(Res.drawable.main_logo)

            val exitLabel = stringResource(Res.string.exit)
            val showWindowLabel = stringResource(Res.string.show_pc_window)
            val stayOnlineLabel = stringResource(Res.string.stay_online_on_close)

            fun exitFully() {
                trayDisposer?.invoke()
                // If you want "hard" exit (kills all coroutines/threads), keep exitProcess:
                exitProcess(0)
                // If you prefer graceful shutdown, replace the two lines above with:
                // exitApplication()
            }

            Tray(
                icon = appIconPainter,
                tooltip = BuildConfig.APP_NAME,
                primaryAction = { bringToFront() },
            ) {
                // Not composable scope => no DisposableEffect here.
                trayDisposer = { dispose() }

                Item(
                    label = showWindowLabel,
                    icon = Icons.Default.Maximize,
                    isEnabled = !isWindowVisible
                ) { bringToFront() }

                Divider()

                CheckableItem(
                    label = stayOnlineLabel,
                    checked = minimizeToTrayOnClose,
                    onCheckedChange = {
                        minimizeToTrayOnClose = it
                        localRepository.setWindowClosable(it)
                    }
                )

                Divider()

                Item(label = exitLabel, icon = Icons.Default.Close) {
                    exitFully()
                }
            }



            Window(
                title = BuildConfig.APP_NAME,
                icon = appIconPainter,
                state = windowState,
                visible = isWindowVisible,
                onCloseRequest = {
                    if (minimizeToTrayOnClose) {
                        isWindowVisible = false
                        windowState.isMinimized = true
                    } else {
                        exitFully()
                    }
                }
            ) {

                DisposableEffect(Unit) {
                    awtWindowRef = window
                    onDispose {
                        if (awtWindowRef === window) awtWindowRef = null
                    }
                }

                window.minimumSize = Dimension(600, HEIGHT_DP_EXPANDED_LOWER_BOUND)

                App()

            }
        }
    }
//} catch (e: Exception) {
//    println(e)
//}
