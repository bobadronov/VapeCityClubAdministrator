package org.bigblackowl.vccadmin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.bigblackowl.vccadmin.di.coreModules
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport {
    KoinApplication(
        configuration = koinConfiguration { modules(modules = coreModules) },
        content = {
            App()
        }
    )
}
