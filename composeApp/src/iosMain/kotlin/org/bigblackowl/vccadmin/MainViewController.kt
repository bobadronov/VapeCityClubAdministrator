package org.bigblackowl.vccadmin

import androidx.compose.ui.window.ComposeUIViewController
import org.bigblackowl.vccadmin.di.coreModules
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    startKoin {
        modules(coreModules) // Ініціалізація Koin з модулями
    }
    App()
}