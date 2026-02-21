@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import coil3.ImageLoader
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject
import java.io.File

actual object PlatformFunctionProvider : KoinComponent {
    private val context: Context by inject(Context::class.java)
    private val cacheDir: File get() = get(named("imageCacheDir"))
    private val imageLoader: ImageLoader get() = get()

    actual fun openNetwork() {
        val intents = listOf(
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }

    actual fun getCacheSize(): Long = cacheDir.directorySizeBytes()

    actual fun clearCache() {
        imageLoader.memoryCache?.clear()
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    private fun File.directorySizeBytes(): Long {
        if (!exists()) return 0L
        var sum = 0L
        walkTopDown().forEach { f -> if (f.isFile) sum += f.length() }
        return sum
    }
}