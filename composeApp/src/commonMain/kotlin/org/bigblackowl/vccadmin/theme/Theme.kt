package org.bigblackowl.vccadmin.theme

import androidx.compose.runtime.Composable

@Composable
internal fun AppTheme(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    AppSettingsContainer(
        onThemeChanged = onThemeChanged,
        content = content
    )
}