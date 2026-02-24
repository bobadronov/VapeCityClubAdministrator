package org.bigblackowl.vccadmin.theme.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import org.bigblackowl.vccadmin.data.entity.ThemeMode
import org.bigblackowl.vccadmin.theme.locals.LocalThemeMode

@Composable
fun ThemeModeProvider(
    themeModeState: MutableState<ThemeMode>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalThemeMode provides themeModeState,
        content = content
    )
}