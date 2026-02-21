package org.bigblackowl.vccadmin.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import okio.Path.Companion.toOkioPath
import org.bigblackowl.vccadmin.ota.OtaUpdateManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

actual val platformModule = module {
    single<File>(named("imageCacheDir")) {
        val ctx: Context = androidContext()
        File(ctx.cacheDir, "image_cache").apply { mkdirs() }
    }

    single<DiskCache> {
        val dir = get<File>(named("imageCacheDir"))
        DiskCache.Builder()
            .directory(dir.toOkioPath())
            .maxSizeBytes(250L * 1024 * 1024) // приклад
            .build()
    }

    single<ImageLoader> {
        ImageLoader.Builder(androidContext())
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(get<Context>(), 0.25)
                    .build()
            }
            .components {
                addPlatformFileSupport()
            }
            .diskCache { get<DiskCache>() }
            .build()
    }
    singleOf(::OtaUpdateManager)
}
actual val ktorEngine: HttpClientEngine = CIO.create()