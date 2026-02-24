package org.bigblackowl.vccadmin.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.bigblackowl.vccadmin.data.entity.ThemeMode
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.theme.locals.shopStatusColors
import org.bigblackowl.vccadmin.theme.locals.userRoleColors
import org.bigblackowl.vccadmin.theme.providers.ColorsProvider
import org.bigblackowl.vccadmin.theme.providers.LocaleProvider
import org.bigblackowl.vccadmin.theme.providers.ThemeModeProvider
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppSettingsContainer(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val localRepository: LocalRepository = koinInject()

    // -------- Theme mode (init once) --------
    val initialThemeMode: ThemeMode = remember {
        when (localRepository.getThemeState()) {
            null -> ThemeMode.AUTO
            true -> ThemeMode.DARK
            false -> ThemeMode.LIGHT
        }
    }
    val themeModeState = remember { mutableStateOf(initialThemeMode) }

    val systemIsDark = isSystemInDarkTheme()
    val themeMode by themeModeState
    val isDark = when (themeMode) {
        ThemeMode.AUTO -> systemIsDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    // -------- Locale (init) --------
    val savedIso: String? = remember {
        localRepository.getLanguage()?.replace('_', '-')
    }
    val effectiveLocale: String? = remember(savedIso) { savedIso }

    // -------- Custom colors --------
    val shopStatus = remember(isDark) { shopStatusColors(isDark) }
    val userRoles = remember(isDark) { userRoleColors(isDark) }

    ThemeModeProvider(themeModeState) {
        ColorsProvider(
            shopStatusColors = shopStatus,
            userRoleColors = userRoles
        ) {
            LocaleProvider(effectiveLocale) {
                onThemeChanged(isDark)

                MaterialExpressiveTheme(
                    colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
                ) {
                    Surface(content = content)
                }
            }
        }
    }
}