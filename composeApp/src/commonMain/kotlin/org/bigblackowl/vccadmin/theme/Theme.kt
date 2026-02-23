@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.bigblackowl.vccadmin.data.entity.ShopStatusColors
import org.bigblackowl.vccadmin.data.entity.UserRoleColors
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.koin.compose.koinInject

/**
 * ISO може бути: "uk", "en", "ru" або з регіоном: "uk-UA", "en-US".
 *
 * customAppLocale — compose-state, який ми провайдим у CompositionLocal.
 * Не оновлюй його напряму під час композиції — тільки через remember/LaunchedEffect.
 */
var customAppLocale by mutableStateOf<String?>(null)

expect object LocalAppLocale {
    val current: String @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>

    fun applyLanguage(iso: String)
}

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

@Suppress("UnusedReceiverParameter")
val MaterialTheme.shopStatusColors: ShopStatusColors
    @Composable get() = LocalShopStatusColors.current
@Suppress("UnusedReceiverParameter")
val MaterialTheme.userRoleColors: UserRoleColors
    @Composable get() = LocalUserRoleColors.current

internal fun shopStatusColors(isDark: Boolean): ShopStatusColors =
    if (isDark) ShopStatusColorsDark else ShopStatusColorsLight

internal fun userRoleColors(isDark: Boolean): UserRoleColors =
    if (isDark) UserRoleColorsDark else UserRoleColorsLight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppTheme(
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

    // -------- Locale (init + apply once per change) --------
    val savedIso: String? = remember {
        localRepository.getLanguage()
            ?.replace('_', '-') // уніфікуємо "uk_UA" -> "uk-UA"
    }

    // Локаль, яку будемо провайдити у CompositionLocal
    val effectiveLocale: String? = remember(savedIso) { savedIso }

    // Оновлюємо глобальний compose-state, але контрольовано (поза прямим присвоєнням у композиції)
    LaunchedEffect(effectiveLocale) {
        customAppLocale = effectiveLocale
    }

    // Застосовуємо мову на платформі щоразу, коли змінюється цільова локаль
    val fallbackLocale = LocalAppLocale.current
    LaunchedEffect(effectiveLocale, fallbackLocale) {
        LocalAppLocale.applyLanguage(effectiveLocale ?: fallbackLocale)
    }

    // -------- Custom colors --------
    val shopStatus = remember(isDark) { shopStatusColors(isDark) }
    val userRoles = remember(isDark) { userRoleColors(isDark) }

    CompositionLocalProvider(
        LocalThemeMode provides themeModeState,
        LocalShopStatusColors provides shopStatus,
        LocalUserRoleColors provides userRoles,
        LocalAppLocale provides customAppLocale,
    ) {
        onThemeChanged(isDark)

        MaterialExpressiveTheme(
            colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        ) {
            Surface(content = content)
        }
    }
}