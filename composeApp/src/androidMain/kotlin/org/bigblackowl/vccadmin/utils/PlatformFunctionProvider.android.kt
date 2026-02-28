@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry
import coil3.ImageLoader
import coil3.disk.DiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject
import java.io.File

actual object PlatformFunctionProvider : KoinComponent {
    private val context: Context by inject(Context::class.java)
    private val cacheDir: File get() = get(named("coil3_disk_cache"))
    private val imageLoader: ImageLoader get() = get()
    private val diskCache: DiskCache get() = get()   // <- додай

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

    actual suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        val mem = imageLoader.memoryCache?.size ?: 0L
        val netMem = imageLoader.diskCache?.size ?: 0L
        val disk = cacheDir.directorySizeBytes()
        mem + disk + netMem
    }

    actual fun clearCache() {
        imageLoader.memoryCache?.clear()
        runCatching { diskCache.clear() }
            .onFailure {
                // fallback лише якщо clear() не зміг
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }
    }

    private fun File.directorySizeBytes(): Long {
        if (!exists()) return 0L
        var sum = 0L
        walkTopDown().forEach { f -> if (f.isFile) sum += f.length() }
        return sum
    }

    // android
    actual suspend fun Clipboard.setPlainText(text: String) {
        val clipData = ClipData.newPlainText("vcc", text)
        // IMPORTANT: call the member function, not the extension again
        this.setClipEntry(clipData.toClipEntry())
    }

    actual suspend fun Clipboard.getPlainText(): String? {
        val entry = this.getClipEntry() ?: return null
        val clipData = entry.clipData
        if (clipData.itemCount <= 0) return null

        // Prefer coerced text (handles text/plain, text/html, etc.)
        val item = clipData.getItemAt(0)
        return item.coerceToText(context)?.toString()
            ?: item.text?.toString()
    }
}
