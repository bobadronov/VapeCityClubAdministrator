package org.bigblackowl.vccadmin.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.bigblackowl.vccadmin.data.entity.ShopStatusColors
import org.bigblackowl.vccadmin.data.entity.UserRoleColors
import org.bigblackowl.vccadmin.data.repository.LocalRepository
import org.koin.compose.koinInject

val LocalThemeMode = compositionLocalOf { mutableStateOf(ThemeMode.AUTO) }

val LocalShopStatusColors = staticCompositionLocalOf {
    ShopStatusColors(
        active = Color.Unspecified,
        inactive = Color.Unspecified,
        closed = Color.Unspecified,
        relocating = Color.Unspecified,
        underRepair = Color.Unspecified,
    )
}
val LocalUserRoleColors = staticCompositionLocalOf {
    UserRoleColors(
        admin = Color.Unspecified,
        user = Color.Unspecified,
    )
}

val MaterialTheme.shopStatusColors: ShopStatusColors
    @Composable get() = LocalShopStatusColors.current

val MaterialTheme.userRoleColors: UserRoleColors
    @Composable get() = LocalUserRoleColors.current

internal fun shopStatusColors(isDark: Boolean): ShopStatusColors = if (isDark) ShopStatusColorsDark else ShopStatusColorsLight
internal fun userRoleColors(isDark: Boolean): UserRoleColors = if (isDark) UserRoleColorsDark else UserRoleColorsLight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppTheme(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val localRepository: LocalRepository = koinInject()
    val themeState: Boolean? = localRepository.getThemeState()

    val initialThemeMode = when (themeState) {
        null -> ThemeMode.AUTO
        true -> ThemeMode.DARK
        false -> ThemeMode.LIGHT
    }

    val themeModeState = remember { mutableStateOf(initialThemeMode) }

    val systemIsDark = isSystemInDarkTheme()

    val themeMode by themeModeState
    val isDark = when (themeMode) {
        ThemeMode.AUTO -> systemIsDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val shopStatusColors = remember(isDark) { shopStatusColors(isDark) }
    val userRoleColors = remember(isDark) { userRoleColors(isDark) }

    CompositionLocalProvider(
        LocalThemeMode provides themeModeState,
        LocalShopStatusColors provides shopStatusColors,
        LocalUserRoleColors provides userRoleColors,
    ) {
        onThemeChanged(isDark)
        MaterialExpressiveTheme(
            colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
            content = { Surface(content = content) }
        )
    }
}
