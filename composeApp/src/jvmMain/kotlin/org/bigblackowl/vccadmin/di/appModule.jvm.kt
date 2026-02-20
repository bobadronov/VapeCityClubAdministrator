package org.bigblackowl.vccadmin.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import org.bigblackowl.vccadmin.ota.OtaUpdateManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformModule = module {
    single<ImageLoader> {
        ImageLoader.Builder(PlatformContext.INSTANCE)
            .crossfade(true)
            .components {
                addPlatformFileSupport()
            }
            .build()
    }
    singleOf(::OtaUpdateManager)
}

actual val ktorEngine: HttpClientEngine = CIO.create()