package org.bigblackowl.vccadmin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.bigblackowl.vccadmin.di.coreModules
import org.bigblackowl.vccadmin.utils.PlatformFunctionProvider
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    PlatformFunctionProvider.installReloadBlocker()

    ComposeViewport {
        KoinApplication(
            configuration = koinConfiguration { modules(modules = coreModules) },
        ) { App() }
    }
}