@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.theme.locals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class LanguageOption(
    val tag: String,   // "uk", "en", "ru"
    val label: String, // "Українська", "English", "Русский"
)

val AppLocalLanguages = listOf(
    LanguageOption("uk", "Українська"),
    LanguageOption("en", "English"),
    LanguageOption("ru", "Русский"),
)


/**
 * ISO: "uk", "en", "ru" або "uk-UA", "en-US" тощо.
 * customAppLocale — compose-state для провайду в CompositionLocal.
 */
var customAppLocale by mutableStateOf<String?>(null)


expect object LocalAppLocale {
    val current: String @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>

    fun applyLanguage(iso: String)
}