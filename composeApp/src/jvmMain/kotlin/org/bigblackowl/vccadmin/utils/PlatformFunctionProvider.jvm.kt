@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import androidx.compose.ui.platform.Clipboard
import coil3.ImageLoader
import coil3.disk.DiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.Locale


actual object PlatformFunctionProvider : KoinComponent {
    private val localRepository: LocalRepository by inject<LocalRepository>()
    private val _windowClosableState = MutableStateFlow(localRepository.getWindowClosableState())
    val windowClosableState: StateFlow<Boolean> = _windowClosableState.asStateFlow()

    fun changeWindowClosableState(state: Boolean) {
        _windowClosableState.value = state
        localRepository.setWindowClosable(state)
    }

    private val cacheDir: File get() = get(named("coil3_disk_cache"))
    private val imageLoader: ImageLoader get() = get()
    private val diskCache: DiskCache get() = get()   // <- додай
    actual fun openNetwork() {
        val os = System.getProperty("os.name").orEmpty().lowercase(Locale.getDefault())
        when {
            os.contains("win") -> openWindowsNetworkSettings()
            os.contains("mac") || os.contains("darwin") -> openMacNetworkSettings()
            else -> openLinuxNetworkSettings()
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

    private fun openWindowsNetworkSettings() {
        if (tryStart("cmd", "/c", "start", "", "ms-settings:network")) return
        if (tryStart("cmd", "/c", "start", "", "ms-settings:network-status")) return
        if (tryStart("cmd", "/c", "start", "", "ncpa.cpl")) return
        tryStart("cmd", "/c", "start", "", "control.exe", "/name", "Microsoft.NetworkAndSharingCenter")
    }

    private fun openMacNetworkSettings() {
        if (tryStart("open", "x-apple.systempreferences:com.apple.NetworkSettings")) return
        tryStart("open", "x-apple.systempreferences:com.apple.preference.network")
    }

    private fun openLinuxNetworkSettings() {
        // GNOME / Ubuntu
        if (tryStart("sh", "-c", "gnome-control-center network")) return
        // NetworkManager UI
        if (tryStart("sh", "-c", "nm-connection-editor")) return
        // KDE Plasma
        if (tryStart("sh", "-c", "systemsettings5 kcm_networkmanagement")) return
        if (tryStart("sh", "-c", "kcmshell5 kcm_networkmanagement")) return
        // Fallback: open "network" in settings apps if present
        tryStart("sh", "-c", "xdg-open 'network:'")
    }

    private fun tryStart(vararg command: String): Boolean {
        return try {
            ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun File.directorySizeBytes(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        var sum = 0L
        walkTopDown().forEach { f ->
            if (f.isFile) sum += f.length()
        }
        return sum
    }

    actual suspend fun Clipboard.setPlainText(text: String) {
        withContext(Dispatchers.IO) {
            try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(text), null)
            } catch (_: Throwable) {
                // fallback: ignore або можна логнути Napier
            }
        }
    }

    actual suspend fun Clipboard.getPlainText(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val data = clipboard.getData(DataFlavor.stringFlavor)
                data as? String
            } catch (_: Throwable) {
                null
            }
        }
    }
}