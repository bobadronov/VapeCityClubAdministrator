package org.bigblackowl.vccadmin.di

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.bigblackowl.vccadmin.ota.OtaUpdateManager
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent

@OptIn(ExperimentalForeignApi::class)
actual val platformModule = module {
    single<String>(named("imageCacheDir")) {
        val caches = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true
        ).first() as String

        // наприклад: .../Library/Caches/VCCAdmin/image_cache
        val appDir = (caches as NSString).stringByAppendingPathComponent("VCCAdmin")
        val cacheDir = (appDir as NSString).stringByAppendingPathComponent("image_cache")

        NSFileManager.defaultManager.createDirectoryAtPath(
            cacheDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        cacheDir
    }

    single<DiskCache> {
        val dir = get<String>(named("imageCacheDir"))
        DiskCache.Builder()
            .directory(dir.toPath())
            .maxSizeBytes(250L * 1024 * 1024)
            .build()
    }

    single<ImageLoader> {
        ImageLoader.Builder(get())
            .crossfade(true)
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder().maxSizePercent(get(),0.25).build()
            }
            .diskCache { get<DiskCache>() }
            .components {
                addPlatformFileSupport()
            }
            .build()
    }

    singleOf(::OtaUpdateManager)
}
actual val ktorEngine: HttpClientEngine = Darwin.create()