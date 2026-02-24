// shared
package org.bigblackowl.vccadmin.di

import io.ktor.client.engine.HttpClientEngine
import org.koin.core.module.Module

// Модуль Koin для залежностей


expect val platformModule: Module
expect val ktorEngine: HttpClientEngine

val coreModules = listOf(
    logger,
    networkModule,
    repositoryModule,
    otaModule,
    navigationModule,
    screensViewModelModule,
    platformModule,
)