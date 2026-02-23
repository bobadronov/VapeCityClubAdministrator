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
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import java.io.File

actual val platformModule = module {

    single<File>(named("coil3_disk_cache")) {
        val context: Context by inject(Context::class.java)
        File(context.cacheDir, "coil3_disk_cache").apply { mkdirs() }
    }

    single<DiskCache> {
        val dir = get<File>(named("coil3_disk_cache"))
        DiskCache.Builder()
            .directory(dir.toOkioPath())
            .maxSizeBytes(250L * 1024 * 1024)
            .build()
    }

    single<ImageLoader> {
        val context: Context by inject(Context::class.java)
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache { get<DiskCache>() }
            .components {
                addPlatformFileSupport()
            }
            .build()
    }
    singleOf(::OtaUpdateManager)
}
actual val ktorEngine: HttpClientEngine = CIO.create()