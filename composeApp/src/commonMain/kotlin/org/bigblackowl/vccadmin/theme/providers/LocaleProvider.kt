package org.bigblackowl.vccadmin.theme.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import org.bigblackowl.vccadmin.theme.locals.LocalAppLocale
import org.bigblackowl.vccadmin.theme.locals.customAppLocale

@Composable
fun LocaleProvider(
    effectiveLocale: String?,
    content: @Composable () -> Unit,
) {
    // 1) оновлюємо global compose-state (контрольовано)
    LaunchedEffect(effectiveLocale) {
        customAppLocale = effectiveLocale
    }

    // 2) застосовуємо мову на платформі при зміні цільової локалі
    val fallbackLocale = LocalAppLocale.current
    LaunchedEffect(effectiveLocale, fallbackLocale) {
        LocalAppLocale.applyLanguage(effectiveLocale ?: fallbackLocale)
    }

    CompositionLocalProvider(
        LocalAppLocale provides customAppLocale,
        content = content
    )
}