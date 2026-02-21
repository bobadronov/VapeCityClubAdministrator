package org.bigblackowl.vccadmin.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import okio.Path.Companion.toOkioPath
import org.bigblackowl.vccadmin.ota.OtaUpdateManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File

actual val platformModule = module {
    // 1) Де зберігати disk-cache (підбери базову папку під свій застосунок)
    single<File>(qualifier = org.koin.core.qualifier.named("imageCacheDir")) {
        val base = File(System.getProperty("user.home"), ".vccadmin") // приклад
        File(base, "image_cache").apply { mkdirs() }
    }

    // 2) DiskCache (якщо ти на Coil3)
    single<DiskCache> {
        val dir = get<File>(org.koin.core.qualifier.named("imageCacheDir"))
        DiskCache.Builder()
            .directory(dir.toOkioPath())
            .maxSizeBytes(250L * 1024 * 1024) // 250MB приклад
            .build()
    }

    // 3) ImageLoader з memory + disk cache
    single<ImageLoader> {
        ImageLoader.Builder(PlatformContext.INSTANCE)
            .crossfade(true)
            .components { addPlatformFileSupport() }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(get<PlatformContext>(), 0.25) // 25% від доступної пам’яті
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