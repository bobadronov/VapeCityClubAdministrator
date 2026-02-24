package org.bigblackowl.vccadmin.theme.locals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
// desktop
actual object LocalAppLocale {
    private var default: Locale? = null
    private val LocalAppLocale = staticCompositionLocalOf { Locale.getDefault().toString() }
    actual val current: String
        @Composable get() = LocalAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (default == null) {
            default = Locale.getDefault()
        }
        val new = when(value) {
            null -> default!!
            else -> Locale(value)
        }
        Locale.setDefault(new)
        return LocalAppLocale.provides(new.toString())
    }

    actual fun applyLanguage(iso: String) {
        val locale = Locale(iso.replace('_', '-'))
        Locale.setDefault(locale)
    }
}