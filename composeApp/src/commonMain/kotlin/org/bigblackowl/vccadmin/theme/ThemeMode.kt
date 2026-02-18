package org.bigblackowl.vccadmin.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.theme_auto
import vccadministrator.composeapp.generated.resources.theme_dark
import vccadministrator.composeapp.generated.resources.theme_light

// Визначення режимів теми
enum class ThemeMode {
    LIGHT,
    DARK,
    AUTO;

    val label: StringResource
        get() = when (this) {
            AUTO -> Res.string.theme_auto
            DARK -> Res.string.theme_dark
            LIGHT -> Res.string.theme_light
        }

    val icon: ImageVector
        get() = when (this) {
            AUTO -> Icons.Default.BrightnessAuto
            DARK -> Icons.Default.DarkMode
            LIGHT -> Icons.Default.LightMode
        }
}

@Composable
fun rememberIsDarkTheme(): Boolean {
    val mode = LocalThemeMode.current.value
    return when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
}