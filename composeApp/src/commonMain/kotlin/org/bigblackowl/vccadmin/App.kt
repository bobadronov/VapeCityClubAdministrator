package org.bigblackowl.vccadmin

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.bigblackowl.vccadmin.navigation.MainTopAppBar
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Navigator
import org.bigblackowl.vccadmin.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navigationViewModel: NavigationViewModel = koinViewModel()

    AppTheme(onThemeChanged) {
        Scaffold(
            topBar = { MainTopAppBar(snackbarHostState = snackbarHostState, navigationViewModel = navigationViewModel) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            Navigator(
                padding = padding,
                snackbarHostState = snackbarHostState,
                navigationViewModel = navigationViewModel
            )
        }
    }
}

