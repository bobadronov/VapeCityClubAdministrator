@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import coil3.ImageLoader
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.stringByAppendingPathComponent
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual object PlatformFunctionProvider : KoinComponent {

    private val cacheDirPath: String get() = get(named("imageCacheDir"))
    private val imageLoader: ImageLoader get() = get()

    actual fun openNetwork() {
        val app = UIApplication.sharedApplication
        // ✅ Safe, App Store friendly: відкриває налаштування саме твого застосунку.
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (settingsUrl != null && app.canOpenURL(settingsUrl)) {
            app.openURL(settingsUrl)
            return
        }
    }

    actual fun getCacheSize(): Long = nsDirectorySizeBytes(cacheDirPath)

    @OptIn(ExperimentalForeignApi::class)
    actual fun clearCache() {
        imageLoader.memoryCache?.clear()

        val fm = NSFileManager.defaultManager
        fm.removeItemAtPath(cacheDirPath, error = null)
        fm.createDirectoryAtPath(
            cacheDirPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun nsDirectorySizeBytes(path: String): Long {
        val fm = NSFileManager.defaultManager
        val enumerator = fm.enumeratorAtPath(path) ?: return 0L

        var total = 0L
        while (true) {
            val next = enumerator.nextObject() as? String ?: break
            val full = (path as NSString).stringByAppendingPathComponent(next)
            val attrs = fm.attributesOfItemAtPath(full, error = null)
            val fileType = attrs?.get(NSFileType) as? String
            if (fileType == NSFileTypeRegular) {
                val size = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
                total += size
            }
        }
        return total
    }
}
