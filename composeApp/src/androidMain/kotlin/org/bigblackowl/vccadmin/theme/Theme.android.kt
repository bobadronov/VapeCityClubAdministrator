@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.theme

import android.content.Context
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import org.koin.java.KoinJavaComponent.inject
import java.util.Locale

// android
@Suppress("DEPRECATION")
actual object LocalAppLocale {
    private val context: Context by inject(Context::class.java)

    private var default: Locale? = null
    actual val current: String
        @Composable get() = LocalLocale.current.platformLocale.toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current

        if (default == null) {
            default = LocalLocale.current.platformLocale
        }

        val new = when(value) {
            null -> default!!
            else -> Locale(value)
        }
        Locale.setDefault(new)
        configuration.setLocale(new)
        val resources = LocalResources.current

        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }

    actual fun applyLanguage(iso: String) {
        val locale = Locale.forLanguageTag(iso.replace('_','-'))
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocales(LocaleList(locale))
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}